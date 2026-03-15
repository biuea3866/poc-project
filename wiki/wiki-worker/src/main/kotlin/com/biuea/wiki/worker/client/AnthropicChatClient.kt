package com.biuea.wiki.worker.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AnthropicChatClient(
    @Value("\${spring.ai.anthropic.api-key}") private val apiKey: String,
    @Value("\${spring.ai.anthropic.chat.options.model:claude-haiku-4-5-20251001}") private val model: String,
    @Value("\${spring.ai.anthropic.chat.options.max-tokens:1024}") private val maxTokens: Int,
    private val objectMapper: ObjectMapper,
) {
    private val restClient = RestClient.builder()
        .baseUrl("https://api.anthropic.com")
        .defaultHeader("x-api-key", apiKey)
        .defaultHeader("anthropic-version", "2023-06-01")
        .defaultHeader("content-type", "application/json")
        .build()

    fun chat(prompt: String): String? {
        val requestBody = mapOf(
            "model" to model,
            "max_tokens" to maxTokens,
            "messages" to listOf(
                mapOf("role" to "user", "content" to prompt)
            )
        )

        val response = restClient.post()
            .uri("/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(requestBody))
            .retrieve()
            .body(AnthropicResponse::class.java)

        return response?.content?.firstOrNull()?.text
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AnthropicResponse(
        val content: List<ContentBlock> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ContentBlock(
        val type: String = "",
        val text: String = "",
    )
}
