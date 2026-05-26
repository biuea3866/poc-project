package com.biuea.springai.tool

import com.biuea.springai.security.RequireScope
import org.springframework.ai.image.ImageModel
import org.springframework.ai.image.ImagePrompt
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * Spring AI 의 `ImageModel` (text → image) 시연 도구.
 *
 * 스코프(`catalog:write`) 검사 + 감사 로그는 `GuardedToolCallback` 데코레이터가 처리.
 *
 * `ImageModel` 빈은 `spring.ai.openai.api-key` 가 설정되어 있어야 등록되므로
 * `ObjectProvider` 로 안전 주입하고, 빈이 없으면 graceful 메시지를 반환한다.
 */
@Component
class ImageGenerationTool(
    private val imageModelProvider: ObjectProvider<ImageModel>,
) {

    @Tool(description = "주어진 텍스트 묘사로 상품 이미지를 생성한다 (OpenAI DALL-E). " +
        "예: '미니멀한 화이트 코튼 셔츠, 옆모습, 회색 배경, 스튜디오 조명'. " +
        "OPENAI_API_KEY 가 설정되지 않으면 사용할 수 없다.")
    @RequireScope("catalog:write")
    fun generateProductImage(
        @ToolParam(description = "이미지 생성 프롬프트 (영문 권장, 한국어도 가능)") prompt: String,
    ): GeneratedImage {
        val imageModel = imageModelProvider.getIfAvailable()
            ?: return GeneratedImage(
                url = null,
                message = "OPENAI_API_KEY 가 설정되지 않아 이미지 생성을 사용할 수 없습니다.",
            )
        val response = imageModel.call(ImagePrompt(prompt))
        val output = response.result?.output
        return GeneratedImage(
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
