package com.biuea.springai.chat

import com.biuea.springai.tool.CatalogTools
import com.biuea.springai.tool.ExtractionTool
import com.biuea.springai.tool.ImageGenerationTool
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.tool.ToolCallback
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * 채팅 게이트웨이 — LLM 호출 + 도구 + 메모리 + 선택적 RAG advisor.
 *
 * Structured Output 정책:
 * - `ChatAnswer { answer, intent }` 두 필드만 — 작은 모델 친화적.
 * - `.entity(...)` 대신 **수동 `BeanOutputConverter`** 사용. JSON 변환에 실패해도
 *   raw text 를 `answer` 필드에 그대로 채워 200 응답 보장 (LLM 이 스키마를 안 지키는 경우 fallback).
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

    private val converter = BeanOutputConverter(ChatAnswer::class.java)

    fun ask(conversationId: String, message: String, useRag: Boolean = true): ChatAnswer {
        val client = if (useRag) chatClient else noRagChatClient
        val systemWithSchema = "응답은 아래 JSON 스키마를 따르세요:\n${converter.format}"
        val raw = client.prompt()
            .system(systemWithSchema)
            .user(message)
            .tools(catalogTools, extractionTool, imageGenerationTool)
            .toolCallbacks(*externalToolCallbacks.toTypedArray())
            .advisors { it.param(ChatMemory.CONVERSATION_ID, conversationId) }
            .call()
            .content()
            ?: return ChatAnswer(answer = "응답을 생성하지 못했습니다.", intent = "OTHER")

        // LLM 이 JSON 스키마를 안 지키는 경우(answer 필드에 array 반환 등) raw 텍스트 그대로 채움.
        return runCatching { converter.convert(raw) ?: fallback(raw) }
            .getOrElse { fallback(raw) }
    }

    private fun fallback(raw: String): ChatAnswer = ChatAnswer(answer = raw, intent = null)

    fun stream(conversationId: String, message: String): Flux<String> =
        chatClient.prompt()
            .user(message)
            .tools(catalogTools, extractionTool, imageGenerationTool)
            .toolCallbacks(*externalToolCallbacks.toTypedArray())
            .advisors { it.param(ChatMemory.CONVERSATION_ID, conversationId) }
            .stream()
            .content()
}
