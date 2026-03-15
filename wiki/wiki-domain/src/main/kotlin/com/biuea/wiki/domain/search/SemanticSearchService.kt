package com.biuea.wiki.domain.search

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.tag.TagDocumentMappingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

enum class SearchMode {
    KEYWORD, SEMANTIC, HYBRID
}

data class SemanticSearchResult(
    val documentId: Long,
    val title: String,
    val snippet: String,
    val similarity: Double,
    val tags: List<String> = emptyList(),
)

data class SearchResponse(
    val items: List<SemanticSearchResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
)

/**
 * Intermediate row type used for the hybrid RRF pipeline.
 * Exposed so TestableSemanticSearchService can override [hybridSemanticQuery].
 */
data class HybridVectorRow(
    val documentId: Long,
    val title: String,
    val snippet: String,
    val similarity: Double,
    val rank: Int,
)

@Service
open class SemanticSearchService(
    @Qualifier("vectorJdbcClient")
    private val vectorJdbcClient: JdbcClient,
    private val documentRepository: DocumentRepository,
    private val tagDocumentMappingRepository: TagDocumentMappingRepository,
    @Value("\${spring.ai.openai.api-key:}") private val openAiApiKey: String,
    @Value("\${spring.ai.openai.embedding.options.model:text-embedding-3-small}") private val embeddingModel: String,
    private val objectMapper: ObjectMapper,
) {
    private val restClient = RestClient.builder()
        .baseUrl("https://api.openai.com")
        .build()

    open fun semanticSearch(
        query: String,
        threshold: Double = 0.7,
        page: Int = 0,
        size: Int = 10,
    ): SearchResponse {
        val embedding = embed(query)
        val vectorStr = embedding.joinToString(",", "[", "]")

        val results = vectorJdbcClient.sql(
            """
            SELECT
                de.document_id,
                d.title,
                de.chunk_content AS snippet,
                1 - (de.embedding <=> :queryVector::vector) AS similarity
            FROM document_embeddings de
            JOIN document d ON d.id = de.document_id
            WHERE d.deleted_at IS NULL
              AND d.status = 'COMPLETED'
              AND 1 - (de.embedding <=> :queryVector::vector) >= :threshold
            ORDER BY similarity DESC
            LIMIT :limit OFFSET :offset
            """
        )
            .param("queryVector", vectorStr)
            .param("threshold", threshold)
            .param("limit", size)
            .param("offset", page * size)
            .query { rs, _ ->
                SemanticSearchResult(
                    documentId = rs.getLong("document_id"),
                    title = rs.getString("title"),
                    snippet = rs.getString("snippet"),
                    similarity = rs.getDouble("similarity"),
                )
            }
            .list()

        val total = vectorJdbcClient.sql(
            """
            SELECT COUNT(DISTINCT de.document_id)
            FROM document_embeddings de
            JOIN document d ON d.id = de.document_id
            WHERE d.deleted_at IS NULL
              AND d.status = 'COMPLETED'
              AND 1 - (de.embedding <=> :queryVector::vector) >= :threshold
            """
        )
            .param("queryVector", vectorStr)
            .param("threshold", threshold)
            .query { rs, _ -> rs.getLong(1) }
            .single()

        val tagMap = buildTagMap(results.map { it.documentId })
        val itemsWithTags = results.map { it.copy(tags = tagMap[it.documentId] ?: emptyList()) }

        return SearchResponse(
            items = itemsWithTags,
            page = page,
            size = size,
            totalElements = total,
        )
    }

    fun integratedSearch(
        query: String,
        mode: SearchMode = SearchMode.HYBRID,
        page: Int = 0,
        size: Int = 10,
    ): SearchResponse {
        return when (mode) {
            SearchMode.KEYWORD -> keywordSearchAsResponse(query, page, size)
            SearchMode.SEMANTIC -> semanticSearch(query, threshold = 0.5, page = page, size = size)
            SearchMode.HYBRID -> hybridSearch(query, page, size)
        }
    }

    private fun keywordSearchAsResponse(query: String, page: Int, size: Int): SearchResponse {
        val pageResult = documentRepository.searchByKeyword(query, PageRequest.of(page, size))
        val tagMap = buildTagMap(pageResult.content.map { it.id })
        return SearchResponse(
            items = pageResult.content.map {
                SemanticSearchResult(
                    documentId = it.id,
                    title = it.title,
                    snippet = it.content?.take(200) ?: "",
                    similarity = 0.0,
                    tags = tagMap[it.id] ?: emptyList(),
                )
            },
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
        )
    }

    private fun hybridSearch(query: String, page: Int, size: Int): SearchResponse {
        val fetchSize = (size * 10).coerceAtLeast(50)

        // Keyword results
        val keywordDocs = documentRepository.searchByKeyword(query, PageRequest.of(0, fetchSize)).content
        val keywordItems = keywordDocs.mapIndexed { rank, doc ->
            RrfItem(
                documentId = doc.id,
                title = doc.title,
                snippet = doc.content?.take(200) ?: "",
                keywordRank = rank + 1,
                semanticRank = null,
                similarity = 0.0,
            )
        }

        // Semantic results from pgvector
        val embedding = embed(query)
        val vectorStr = embedding.joinToString(",", "[", "]")
        val semanticRows = hybridSemanticQuery(vectorStr, fetchSize)
        val semanticItems = semanticRows.map { row ->
            RrfItem(
                documentId = row.documentId,
                title = row.title,
                snippet = row.snippet,
                keywordRank = null,
                semanticRank = row.rank,
                similarity = row.similarity,
            )
        }

        // RRF merge
        val merged = mutableMapOf<Long, RrfItem>()
        for (item in keywordItems) {
            merged[item.documentId] = item
        }
        for (item in semanticItems) {
            val existing = merged[item.documentId]
            if (existing != null) {
                merged[item.documentId] = existing.copy(
                    semanticRank = item.semanticRank,
                    similarity = item.similarity,
                )
            } else {
                merged[item.documentId] = item
            }
        }

        val k = 60.0
        val ranked = merged.values
            .map { item ->
                val kScore = if (item.keywordRank != null) 1.0 / (k + item.keywordRank) else 0.0
                val sScore = if (item.semanticRank != null) 1.0 / (k + item.semanticRank) else 0.0
                item to (kScore + sScore)
            }
            .sortedByDescending { it.second }

        val total = ranked.size.toLong()
        val paged = ranked.drop(page * size).take(size)
        val tagMap = buildTagMap(paged.map { it.first.documentId })

        return SearchResponse(
            items = paged.map { (item, score) ->
                SemanticSearchResult(
                    documentId = item.documentId,
                    title = item.title,
                    snippet = item.snippet,
                    similarity = score,
                    tags = tagMap[item.documentId] ?: emptyList(),
                )
            },
            page = page,
            size = size,
            totalElements = total,
        )
    }

    /**
     * Executes the pgvector cosine similarity query for the hybrid pipeline.
     * Extracted as an open method so tests can override it without a real PostgreSQL connection.
     */
    open fun hybridSemanticQuery(vectorStr: String, fetchSize: Int): List<HybridVectorRow> {
        return vectorJdbcClient.sql(
            """
            SELECT
                de.document_id,
                d.title,
                de.chunk_content AS snippet,
                1 - (de.embedding <=> :queryVector::vector) AS similarity
            FROM document_embeddings de
            JOIN document d ON d.id = de.document_id
            WHERE d.deleted_at IS NULL AND d.status = 'COMPLETED'
            ORDER BY similarity DESC
            LIMIT :limit
            """
        )
            .param("queryVector", vectorStr)
            .param("limit", fetchSize)
            .query { rs, idx ->
                HybridVectorRow(
                    documentId = rs.getLong("document_id"),
                    title = rs.getString("title"),
                    snippet = rs.getString("snippet"),
                    similarity = rs.getDouble("similarity"),
                    rank = idx + 1,
                )
            }
            .list()
    }

    private fun buildTagMap(documentIds: List<Long>): Map<Long, List<String>> {
        if (documentIds.isEmpty()) return emptyMap()
        val mappings = tagDocumentMappingRepository.findByDocumentIdIn(documentIds)
        return mappings.groupBy(
            keySelector = { it.document.id },
            valueTransform = { it.tag.name },
        )
    }

    open fun embed(text: String): List<Float> {
        val requestBody = mapOf("model" to embeddingModel, "input" to text)
        val response = restClient.post()
            .uri("/v1/embeddings")
            .header("Authorization", "Bearer $openAiApiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(requestBody))
            .retrieve()
            .body(OpenAiEmbeddingResponse::class.java)
        return response?.data?.firstOrNull()?.embedding?.toList()
            ?: throw IllegalStateException("OpenAI 임베딩 응답이 비어있습니다.")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenAiEmbeddingResponse(val data: List<EmbeddingData> = emptyList())

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EmbeddingData(val embedding: FloatArray = floatArrayOf())

    private data class RrfItem(
        val documentId: Long,
        val title: String,
        val snippet: String,
        val keywordRank: Int?,
        val semanticRank: Int?,
        val similarity: Double,
    )
}
