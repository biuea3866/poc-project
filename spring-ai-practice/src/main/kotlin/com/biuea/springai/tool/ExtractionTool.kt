package com.biuea.springai.tool

import com.biuea.springai.domain.ExtractedProduct
import com.biuea.springai.security.RequireScope
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * `BeanOutputConverter` 시연 도구 — 자유 텍스트에서 구조화 객체를 추출.
 *
 * 스코프(`catalog:read`) 검사 + 감사 로그는 `GuardedToolCallback` 데코레이터가 처리.
 * 본 메서드는 비즈니스 로직 (`chatClient.entity(...)`) 만 담는다.
 *
 * `ChatClient.entity(Class<T>)` 내부 동작:
 * 1. `BeanOutputConverter<T>` 가 타겟 클래스(ExtractedProduct) 의 JSON Schema 생성
 * 2. "이 스키마에 맞는 JSON 으로만 응답하라" 는 지침을 LLM 프롬프트에 추가
 * 3. 모델 응답을 ObjectMapper 로 역직렬화해 즉시 객체 반환
 *
 * ChatClient 와의 순환 의존(ChatClient → ToolCallbackProvider → ExtractionTool → ChatClient)
 * 회피를 위해 `ObjectProvider<ChatClient>` 로 lazy 주입.
 */
@Component
class ExtractionTool(
    private val chatClientProvider: ObjectProvider<ChatClient>,
) {

    @Tool(description = "자유 텍스트 형태의 옷 설명에서 카테고리·색상·소재·핏·시즌·키워드를 구조화해 추출한다. " +
        "예) '빈티지한 오버핏 카키색 면 자켓, M사이즈, 가을용' → category=자켓, color=카키, ...")
    @RequireScope("catalog:read")
    fun extractProductAttributes(
        @ToolParam(description = "옷 설명 자유 텍스트") text: String,
    ): ExtractedProduct =
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
