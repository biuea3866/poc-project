package com.biuea.wiki.worker.config

import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig(
    @Value("\${spring.ai.anthropic.api-key}") private val anthropicApiKey: String,
    @Value("\${spring.ai.anthropic.chat.options.model:claude-haiku-4-5-20251001}") private val anthropicModel: String,
    @Value("\${spring.ai.anthropic.chat.options.max-tokens:1024}") private val anthropicMaxTokens: Int,
    @Value("\${spring.ai.openai.api-key}") private val openAiApiKey: String,
    @Value("\${spring.ai.openai.embedding.options.model:text-embedding-3-small}") private val openAiEmbeddingModel: String,
) {

    @Bean
    fun anthropicChatModel(): AnthropicChatModel {
        val api = AnthropicApi.builder()
            .apiKey(anthropicApiKey)
            .build()
        val options = AnthropicChatOptions.builder()
            .model(anthropicModel)
            .maxTokens(anthropicMaxTokens)
            .build()
        return AnthropicChatModel.builder()
            .anthropicApi(api)
            .defaultOptions(options)
            .build()
    }

    @Bean
    fun chatClient(anthropicChatModel: AnthropicChatModel): ChatClient =
        ChatClient.builder(anthropicChatModel).build()

    @Bean
    fun embeddingModel(): EmbeddingModel {
        val api = OpenAiApi.builder()
            .apiKey(openAiApiKey)
            .build()
        val options = OpenAiEmbeddingOptions.builder()
            .model(openAiEmbeddingModel)
            .build()
        return OpenAiEmbeddingModel(api, MetadataMode.EMBED, options)
    }
}
