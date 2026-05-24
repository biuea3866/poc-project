package com.biuea.springai.tool

import com.biuea.springai.domain.ExtractedProduct
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * `BeanOutputConverter` 시연 도구 — 자유 텍스트에서 구조화 객체를 추출.
 *
 * Spring AI 컴포넌트 매핑:
 *
 * - **`ChatClient.entity(Class<T>)`** — 내부적으로 `BeanOutputConverter<T>` 를 사용한다.
 *   1. 타겟 클래스(`ExtractedProduct`) 의 JSON Schema 를 자동 생성
 *   2. "이 스키마에 맞는 JSON 으로만 응답하라" 는 지침을 시스템/유저 프롬프트에 추가
 *   3. 모델 응답을 ObjectMapper 로 역직렬화해 즉시 객체로 반환
 *
 * - **`@Tool`** — 이 도구도 일반 도구처럼 LLM 이 동적으로 호출한다.
 *   사용자가 "이 옷 설명에서 카테고리·색상 뽑아줘" 라고 자연어로 말하면 LLM 이 이 도구를 자동 선택.
 *
 * - 도구 내부에서 다시 `ChatClient.call()` 을 호출하는 **재귀적 LLM 사용** 패턴이다.
 *   학습 목적이 명확하지만 운영 코드에서는 LLM 비용·지연이 누적되므로 신중히 사용한다.
 */
@Component
class ExtractionTool(
    // 순환 의존(ChatClient → ToolCallbackProvider → ExtractionTool → ChatClient) 회피용 lazy 주입.
    // ChatClient 가 fully initialized 된 후 도구 호출 시점에 한 번만 lookup.
    private val chatClientProvider: ObjectProvider<ChatClient>,
    private val toolGuard: ToolGuard,
) {

    @Tool(description = "자유 텍스트 형태의 옷 설명에서 카테고리·색상·소재·핏·시즌·키워드를 구조화해 추출한다. " +
        "예) '빈티지한 오버핏 카키색 면 자켓, M사이즈, 가을용' → category=자켓, color=카키, ...")
    fun extractProductAttributes(
        @ToolParam(description = "옷 설명 자유 텍스트") text: String,
    ): ExtractedProduct = toolGuard.invoke(
        tool = "extractProductAttributes",
        scope = "catalog:read",
        args = mapOf("text" to text),
    ) {
        // ChatClient.entity() 가 내부에서 BeanOutputConverter 를 사용해
        // strict JSON 응답을 받아 ExtractedProduct 로 역직렬화한다.
        chatClientProvider.getObject().prompt()
            .system(
                "당신은 옷 설명 텍스트에서 속성을 추출하는 도우미입니다. " +
                    "주어진 텍스트에서 카테고리, 색상, 소재, 핏, 사이즈 힌트, 시즌, 핵심 키워드를 뽑아 JSON 으로 반환하세요. " +
                    "확인할 수 없는 항목은 null 또는 빈 리스트로 두세요.",
            )
            .user(text)
            .call()
            .entity(ExtractedProduct::class.java)
            ?: ExtractedProduct()
    }
}
