package com.biuea.springai.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * ChatGateway 의 ChatClient + ChatMemory + RAG Advisor 구성.
 *
 * Spring AI 컴포넌트 매핑:
 *
 * - **`ChatClient`** — LLM 호출의 fluent API. `prompt().user(...).tools(...).call().content()` 패턴.
 *   `ChatClient.Builder` 가 자동 등록되어 있고, 이 빈에서 시스템 프롬프트·advisor 를 미리 엮는다.
 *
 * - **`ChatMemory`** — 같은 `conversationId` 의 이전 메시지를 보관하는 메모리.
 *   `MessageWindowChatMemory` 는 최근 N 개만 유지(슬라이딩 윈도우)해 토큰 한도를 보호한다.
 *   `InMemoryChatMemoryRepository` 는 PoC 용. 운영에서는 Redis / JDBC 구현체로 교체.
 *
 * - **`MessageChatMemoryAdvisor`** — ChatClient 의 advisor 체인에 끼워 넣어
 *   매 요청마다 자동으로 메모리에서 이전 메시지를 읽어 컨텍스트로 주입하고, 새 메시지는 메모리에 저장.
 *
 * - **`QuestionAnswerAdvisor`** — 매 채팅 요청마다 `VectorStore.similaritySearch(question)` 을 수행해
 *   유사한 문서 청크를 LLM 프롬프트의 컨텍스트로 자동 주입(=RAG). 사용자는 도구를 명시적으로 부르지 않아도
 *   사이즈 가이드·FAQ 같은 자체 문서 기반으로 답변을 받게 된다.
 */
@Configuration
class ChatClientConfig {

    @Bean
    fun chatMemory(): ChatMemory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(InMemoryChatMemoryRepository())
            .maxMessages(20)
            .build()

    @Bean
    fun defaultChatClient(
        builder: ChatClient.Builder,
        chatMemory: ChatMemory,
        vectorStore: VectorStore,
    ): ChatClient = builder
        .defaultSystem(
            "당신은 의류 이커머스 쇼핑 어시스턴트입니다. " +
                "사용 가능한 도구를 적극 활용해 사용자 질문에 답하세요. " +
                "사이즈·세탁·교환·배송 같은 정책 질문은 도구 호출 없이도 컨텍스트에 자동 주입된 " +
                "내부 문서(사이즈 가이드/FAQ/관리 가이드)를 참고해 답변할 수 있습니다. " +
                "추측하지 말고 도구 결과나 컨텍스트에 근거해 한국어 해요체로 답합니다. " +
                "권한 부족으로 도구가 거부되면 그 사실을 정중히 안내하세요.",
        )
        .defaultAdvisors(
            MessageChatMemoryAdvisor.builder(chatMemory).build(),
            QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                    SearchRequest.builder()
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build(),
                )
                .build(),
        )
        .build()
}
