package com.biuea.springai.chat

import com.biuea.springai.tool.CatalogTools
import com.biuea.springai.tool.ExtractionTool
import com.biuea.springai.tool.ImageGenerationTool
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.tool.ToolCallback
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * 채팅 게이트웨이 — LLM 호출 + 도구 + 메모리 + 선택적 RAG advisor.
 *
 * 작은 모델(llama3.2:3b) 친화적 단순화:
 * - 응답 객체 `ChatAnswer` 필드 2개로 축소 (answer + intent)
 * - 시스템 프롬프트도 짧게 (도구 description 은 `tools(...)` 가 자동 주입하므로 중복 제거)
 * - RAG advisor 가 필요 없는 단순 도구 호출은 `useRag = false` 로 `noRagChatClient` 를 사용해
 *   LLM 컨텍스트를 줄임 — 응답 시간 단축
 */
@Service
class ChatGatewayService(
    private val chatClient: ChatClient,
    @Qualifier("noRagChatClient") private val noRagChatClient: ChatClient,
    private val catalogTools: CatalogTools,
    private val extractionTool: ExtractionTool,
    private val imageGenerationTool: ImageGenerationTool,
    private val externalToolCallbacks: List<ToolCallback>,
    private val chatMemory: ChatMemory,
) {

    fun ask(conversationId: String, message: String, useRag: Boolean = true): ChatAnswer {
        val client = if (useRag) chatClient else noRagChatClient
        return client.prompt()
            .user(message)
            .tools(catalogTools, extractionTool, imageGenerationTool)
            .toolCallbacks(*externalToolCallbacks.toTypedArray())
            .advisors { it.param(ChatMemory.CONVERSATION_ID, conversationId) }
            .call()
            .entity(ChatAnswer::class.java)
            ?: ChatAnswer(answer = "응답을 생성하지 못했습니다.", intent = "OTHER")
    }

    fun stream(conversationId: String, message: String): Flux<String> =
        chatClient.prompt()
            .user(message)
            .tools(catalogTools, extractionTool, imageGenerationTool)
            .toolCallbacks(*externalToolCallbacks.toTypedArray())
            .advisors { it.param(ChatMemory.CONVERSATION_ID, conversationId) }
            .stream()
            .content()
}
