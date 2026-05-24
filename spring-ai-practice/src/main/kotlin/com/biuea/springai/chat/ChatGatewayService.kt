package com.biuea.springai.chat

import com.biuea.springai.tool.CatalogTools
import com.biuea.springai.tool.ExtractionTool
import com.biuea.springai.tool.ImageGenerationTool
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.tool.ToolCallback
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * 사용자의 자연어 메시지를 LLM 에 위임하고, 도구·메모리·RAG advisor 를 함께 작동시킨다.
 *
 * Spring AI 컴포넌트 매핑:
 *
 * - **`ChatClient.prompt()` fluent chain** — 시스템·유저 메시지, 도구, advisor 파라미터를 묶어 한 번에 LLM 호출.
 * - **`.system(PromptTemplate)` + `PromptTemplate.render`** — 매 요청마다 현재 토큰의 스코프를
 *   시스템 프롬프트에 동적 주입한다. (`{scopes}` 플레이스홀더 → 실제 권한 목록)
 * - **`.tools(catalogTools, extractionTool)`** — LLM 이 자동으로 발견·선택해 호출할 도구 객체.
 *   `@Tool` 어노테이션의 메타데이터가 JSON Schema 로 변환돼 함께 전송됨.
 * - **`.advisors { it.param(CONVERSATION_ID, ...) }`** — ChatClientConfig 에 사전 부착된
 *   `MessageChatMemoryAdvisor` + `QuestionAnswerAdvisor` 에 요청별 컨텍스트(conversationId) 를 전달.
 * - **`.call().content()`** — 동기 호출. 도구 콜링 라운드까지 마친 최종 자연어 응답을 반환.
 * - **`.stream().content()`** — 토큰 청크를 `Flux<String>` 으로 반환 (SSE 스트리밍 엔드포인트용).
 */
@Service
class ChatGatewayService(
    private val chatClient: ChatClient,
    private val catalogTools: CatalogTools,
    private val extractionTool: ExtractionTool,
    private val imageGenerationTool: ImageGenerationTool,
    private val chatMemory: ChatMemory,
    private val externalToolCallbacks: List<ToolCallback>,
) {

    private val systemTemplate = PromptTemplate(
        """
        당신은 의류 이커머스 쇼핑 어시스턴트입니다.
        현재 사용자의 보유 권한(scopes): {scopes}
        - 보유한 권한 범위 안에서 도구를 호출하세요.
        - 권한 부족 도구를 LLM 추론으로 우회하지 마세요. 거부되면 그 사실을 정중히 안내하세요.
        - 사이즈/세탁/교환/배송 정책은 컨텍스트로 자동 주입되는 내부 문서를 인용해 답하세요.
        - 한국어 해요체로 답합니다.
        """.trimIndent(),
    )

    fun ask(conversationId: String, message: String): String {
        val systemPrompt = renderSystemPrompt()
        val answer = chatClient.prompt()
            .system(systemPrompt)
            .user(message)
            .tools(catalogTools, extractionTool, imageGenerationTool)
            .toolCallbacks(*externalToolCallbacks.toTypedArray())
            .advisors { it.param(ChatMemory.CONVERSATION_ID, conversationId) }
            .call()
            .content()
        return answer ?: "응답을 생성하지 못했습니다."
    }

    /** SSE 스트리밍용. ChatClient.stream() 이 토큰 단위 청크를 Flux 로 반환한다. */
    fun stream(conversationId: String, message: String): Flux<String> {
        val systemPrompt = renderSystemPrompt()
        return chatClient.prompt()
            .system(systemPrompt)
            .user(message)
            .tools(catalogTools, extractionTool, imageGenerationTool)
            .toolCallbacks(*externalToolCallbacks.toTypedArray())
            .advisors { it.param(ChatMemory.CONVERSATION_ID, conversationId) }
            .stream()
            .content()
    }

    private fun renderSystemPrompt(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val scopes = authentication?.authorities
            ?.map { it.authority.removePrefix("SCOPE_") }
            ?.sorted()
            ?.joinToString(", ")
            ?: "(없음)"
        return systemTemplate.render(mapOf("scopes" to scopes))
    }
}
