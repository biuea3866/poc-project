package com.biuea.springai.tool

import org.springframework.ai.image.ImageModel
import org.springframework.ai.image.ImagePrompt
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * Spring AI 의 **`ImageModel`** (text → image) 시연 도구.
 *
 * 컴포넌트 매핑:
 *
 * - **`ImageModel`** — `ChatModel` 과 동일한 추상화 계층의 이미지 생성 모델 인터페이스.
 *   `spring-ai-starter-model-openai` 가 OpenAI 의 DALL-E 어댑터를 자동 등록한다.
 *   API 키(`spring.ai.openai.api-key`) 가 없으면 빈이 등록되지 않으므로 `ObjectProvider` 로 안전 주입.
 *
 * - **`ImagePrompt`** — 텍스트 프롬프트 + 생성 옵션(모델/사이즈/품질) 을 묶는 컨테이너.
 *   `application.yml` 의 `spring.ai.openai.image.options.*` 가 기본값.
 *
 * - **`ImageResponse.result.output.url`** — 생성된 이미지의 임시 URL (DALL-E 는 ~1시간 유효).
 *
 * 본 도구는 `@Tool` 로 노출되어 LLM 이 사용자의 자연어 의도("로고 디자인 만들어줘") 를 보고
 * 호출할 수 있다. 운영에선 API 비용/저작권 이슈가 있으므로 권한 통제(`catalog:write`) 적용.
 */
@Component
class ImageGenerationTool(
    private val imageModelProvider: ObjectProvider<ImageModel>,
    private val toolGuard: ToolGuard,
) {

    @Tool(description = "주어진 텍스트 묘사로 상품 이미지를 생성한다 (OpenAI DALL-E). " +
        "예: '미니멀한 화이트 코튼 셔츠, 옆모습, 회색 배경, 스튜디오 조명'. " +
        "OPENAI_API_KEY 가 설정되지 않으면 사용할 수 없다.")
    fun generateProductImage(
        @ToolParam(description = "이미지 생성 프롬프트 (영문 권장, 한국어도 가능)") prompt: String,
    ): GeneratedImage = toolGuard.invoke(
        tool = "generateProductImage",
        scope = "catalog:write",
        args = mapOf("prompt" to prompt),
    ) {
        val imageModel = imageModelProvider.getIfAvailable()
            ?: return@invoke GeneratedImage(
                url = null,
                message = "OPENAI_API_KEY 가 설정되지 않아 이미지 생성을 사용할 수 없습니다.",
            )
        val response = imageModel.call(ImagePrompt(prompt))
        val output = response.result?.output
        GeneratedImage(
            url = output?.url,
            base64 = output?.b64Json,
            message = if (output?.url != null) "이미지 생성 완료" else "이미지를 받아오지 못했습니다.",
        )
    }
}

data class GeneratedImage(
    val url: String? = null,
    val base64: String? = null,
    val message: String,
)
