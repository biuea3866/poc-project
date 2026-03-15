package com.biuea.wiki.domain.search

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service

@Service
class VectorSearchService(
    @Qualifier("vectorJdbcClient")
    private val vectorJdbcClient: JdbcClient,
) {
    fun searchByVector(embedding: List<Float>, limit: Int = 10): List<VectorSearchResult> {
        val vectorStr = embedding.joinToString(",", "[", "]")

        return vectorJdbcClient.sql("""
            SELECT
                id,
                document_id,
                document_revision_id,
                chunk_content,
                token_count,
                1 - (embedding <=> :embedding::vector) AS similarity
            FROM document_embeddings
            ORDER BY embedding <=> :embedding::vector
            LIMIT :limit
        """)
            .param("embedding", vectorStr)
            .param("limit", limit)
            .query { rs, _ ->
                VectorSearchResult(
                    id = rs.getLong("id"),
                    documentId = rs.getLong("document_id"),
                    documentRevisionId = rs.getLong("document_revision_id"),
                    chunkContent = rs.getString("chunk_content"),
                    tokenCount = rs.getInt("token_count"),
                    similarity = rs.getDouble("similarity"),
                )
            }
            .list()
    }

    fun searchByText(query: String, limit: Int = 10): List<VectorSearchResult> {
        return vectorJdbcClient.sql("""
            SELECT
                id,
                document_id,
                document_revision_id,
                chunk_content,
                token_count,
                0.0 AS similarity
            FROM document_embeddings
            WHERE chunk_content ILIKE :query
            ORDER BY id DESC
            LIMIT :limit
        """)
            .param("query", "%$query%")
            .param("limit", limit)
            .query { rs, _ ->
                VectorSearchResult(
                    id = rs.getLong("id"),
                    documentId = rs.getLong("document_id"),
                    documentRevisionId = rs.getLong("document_revision_id"),
                    chunkContent = rs.getString("chunk_content"),
                    tokenCount = rs.getInt("token_count"),
                    similarity = rs.getDouble("similarity"),
                )
            }
            .list()
    }
}

data class VectorSearchResult(
    val id: Long,
    val documentId: Long,
    val documentRevisionId: Long,
    val chunkContent: String,
    val tokenCount: Int,
    val similarity: Double,
)
