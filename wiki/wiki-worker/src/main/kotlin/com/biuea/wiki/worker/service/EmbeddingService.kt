package com.biuea.wiki.worker.service

import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.document.DocumentRevisionRepository
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EmbeddingService(
    private val embeddingModel: EmbeddingModel,
    private val documentRepository: DocumentRepository,
    private val documentRevisionRepository: DocumentRevisionRepository,
    private val jdbcClient: JdbcClient,
) {
    @Transactional
    fun embed(documentId: Long, documentRevisionId: Long) {
        val document = documentRepository.findById(documentId).orElseThrow {
            IllegalArgumentException("문서를 찾을 수 없습니다. id=$documentId")
        }
        val revision = documentRevisionRepository.findById(documentRevisionId).orElseThrow {
            IllegalArgumentException("리비전을 찾을 수 없습니다. id=$documentRevisionId")
        }

        val textToEmbed = buildString {
            append(document.title)
            document.content?.let { append("\n\n").append(it) }
        }

        val vector = embeddingModel.embed(textToEmbed)
        val tokenCount = textToEmbed.split(" ").size  // 간단 추정

        // PostgreSQL document_embeddings 테이블에 upsert
        jdbcClient.sql("""
            INSERT INTO document_embeddings
                (document_id, document_revision_id, embedding, chunk_content, token_count)
            VALUES
                (:documentId, :documentRevisionId, :embedding::vector, :chunkContent, :tokenCount)
            ON CONFLICT (document_id, document_revision_id)
            DO UPDATE SET
                embedding = EXCLUDED.embedding,
                chunk_content = EXCLUDED.chunk_content,
                token_count = EXCLUDED.token_count
        """)
            .param("documentId", documentId)
            .param("documentRevisionId", documentRevisionId)
            .param("embedding", vector.toList().toString().replace("[", "[").replace("]", "]"))
            .param("chunkContent", textToEmbed)
            .param("tokenCount", tokenCount)
            .update()
    }
}
