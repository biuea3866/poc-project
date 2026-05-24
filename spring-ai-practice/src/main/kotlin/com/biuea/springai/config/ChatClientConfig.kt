package com.biuea.springai.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * ChatGateway 가 사용하는 ChatClient 빈 2종을 구성한다.
 *
 * - `defaultChatClient` (Primary) — `MessageChatMemoryAdvisor` + `QuestionAnswerAdvisor(RAG)`
 * - `noRagChatClient`             — `MessageChatMemoryAdvisor` 만 (작은 모델로 빠른 응답이 필요할 때)
 *
 * 두 빈을 분리해 둠으로써 `ChatGatewayService` 가 `useRag` 플래그로 즉시 분기할 수 있다.
 * advisor 체인은 ChatClient 의 immutable 한 default 상태라 호출별로 토글하기 어렵기 때문이다.
 */
@Configuration
class ChatClientConfig {

    private val systemPrompt =
        "당신은 의류 이커머스 어시스턴트입니다. 도구를 적극 호출해 답합니다. " +
            "한국어 해요체로 답합니다. 응답은 ChatAnswer JSON 스키마({answer, intent}) 를 따르세요."

    @Bean
    fun chatMemory(): ChatMemory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(InMemoryChatMemoryRepository())
            .maxMessages(20)
            .build()

    @Bean
    @Primary
    fun defaultChatClient(
        chatModel: ChatModel,
        chatMemory: ChatMemory,
        vectorStore: VectorStore,
    ): ChatClient = ChatClient.builder(chatModel)
        .defaultSystem(systemPrompt)
        .defaultAdvisors(
            MessageChatMemoryAdvisor.builder(chatMemory).build(),
            QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(3).similarityThreshold(0.5).build())
                .build(),
        )
        .build()

    @Bean("noRagChatClient")
    fun noRagChatClient(
        chatModel: ChatModel,
        chatMemory: ChatMemory,
    ): ChatClient = ChatClient.builder(chatModel)
        .defaultSystem(systemPrompt)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build()
}
