package com.biuea.wiki.worker.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OpenAiEmbeddingClient(
    @Value("\${spring.ai.openai.api-key}") private val apiKey: String,
    @Value("\${spring.ai.openai.embedding.options.model:text-embedding-3-small}") private val model: String,
    private val objectMapper: ObjectMapper,
) {
    private val restClient = RestClient.builder()
        .baseUrl("https://api.openai.com")
        .defaultHeader("Authorization", "Bearer $apiKey")
        .defaultHeader("content-type", "application/json")
        .build()

    fun embed(text: String): FloatArray {
        val requestBody = mapOf(
            "model" to model,
            "input" to text,
        )

        val response = restClient.post()
            .uri("/v1/embeddings")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(requestBody))
            .retrieve()
            .body(OpenAiEmbeddingResponse::class.java)

        return response?.data?.firstOrNull()?.embedding
            ?: throw IllegalStateException("OpenAI 임베딩 응답이 비어있습니다.")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenAiEmbeddingResponse(
        val data: List<EmbeddingData> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EmbeddingData(
        val embedding: FloatArray = floatArrayOf(),
    )
}
