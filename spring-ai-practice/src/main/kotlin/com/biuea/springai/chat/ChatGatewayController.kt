package com.biuea.springai.chat

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.ResourceAccessException
import reactor.core.publisher.Flux

/**
 * 채팅 게이트웨이 — UI 가 자연어 메시지를 보내면 LLM 이 도구를 동적으로 호출해 응답한다.
 *
 * 엔드포인트:
 *
 * - **`POST /chat`** — 동기 응답. 도구 라운드까지 마친 최종 응답을 `ChatAnswer` JSON 으로 반환.
 *
 * - **`POST /chat/stream`** — SSE 스트리밍. Spring AI 의 `chatClient.stream()` 가 반환하는
 *   `Flux<String>` 을 `text/event-stream` 으로 그대로 흘려보낸다. UI 에 타자기 효과로 표시 가능.
 */
@RestController
class ChatGatewayController(
    private val chatGatewayService: ChatGatewayService,
) {

    @PostMapping("/chat")
    fun chat(@Valid @RequestBody request: ChatRequest): ChatAnswerResponse {
        val answer = chatGatewayService.ask(request.conversationId, request.message, request.useRag)
        return ChatAnswerResponse(conversationId = request.conversationId, result = answer)
    }

    /**
     * 스트리밍 응답. 토큰 청크가 도착할 때마다 `data: <chunk>` 한 줄씩 전송.
     *
     * SecurityContextHolder 는 ThreadLocal 이라 Flux 의 비동기 스케줄러로 넘어가면 사라진다.
     * `Hooks.enableAutomaticContextPropagation()` + ContextRegistry 등록 덕분에 자동 복원되지만,
     * 안전을 위해 캡쳐된 컨텍스트를 도구 호출 직전에 명시 복원한다.
     */
    @PostMapping(value = ["/chat/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun chatStream(@Valid @RequestBody request: ChatRequest): Flux<ServerSentEvent<String>> {
        val captured: SecurityContext = SecurityContextHolder.getContext()
        return Flux.defer {
            SecurityContextHolder.setContext(captured)
            chatGatewayService.stream(request.conversationId, request.message)
        }
            .map { chunk -> ServerSentEvent.builder(chunk).build() }
            .concatWith(Flux.just(ServerSentEvent.builder("[DONE]").event("done").build()))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(403)
            .body(mapOf("error" to "forbidden", "message" to (e.message ?: "insufficient scope")))

    @ExceptionHandler(ResourceAccessException::class)
    fun handleLlmUnreachable(e: ResourceAccessException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(502)
            .body(mapOf(
                "error" to "llm_unreachable",
                "message" to "LLM 서버에 연결할 수 없습니다. Ollama 가 실행 중인지 확인하세요. (${e.message})",
            ))

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(404)
            .body(mapOf("error" to "not_found", "message" to (e.message ?: "리소스를 찾을 수 없습니다.")))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(400)
            .body(mapOf("error" to "bad_request", "message" to (e.message ?: "잘못된 요청입니다.")))
}

data class ChatRequest(
    @field:NotBlank val conversationId: String,
    @field:NotBlank val message: String,
    /** RAG (QuestionAnswerAdvisor) 자동 컨텍스트 주입 사용 여부. 작은 모델 + 단순 도구 호출은 false 로 더 빠름. */
    val useRag: Boolean = true,
)

data class ChatAnswerResponse(
    val conversationId: String,
    val result: ChatAnswer,
)
