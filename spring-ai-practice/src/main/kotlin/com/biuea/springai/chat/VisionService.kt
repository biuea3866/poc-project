package com.biuea.springai.chat

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.content.Media
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.ollama.api.OllamaChatOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import org.springframework.web.multipart.MultipartFile

/**
 * 멀티모달(Vision) 채팅 서비스 — 업로드 이미지를 비전 모델 LLM 으로 분석해
 * [VisionAnswer] 구조화 객체로 반환한다.
 *
 * Spring AI 컴포넌트 매핑:
 * - **`UserMessage` + `Media`** — 텍스트 + 이미지 첨부 멀티모달 메시지 구성
 * - **`Prompt(messages, options)`** — `OllamaChatOptions.model(visionModel)` 로 모델을 vision 으로 오버라이드
 * - **`ChatModel.call(Prompt)`** — 낮은 레벨 호출. `ChatClient` 는 기본 ChatModel 의 옵션을 따르므로
 *   vision 모델 임시 사용에는 ChatModel 직접 호출이 단순함
 * - **`BeanOutputConverter<VisionAnswer>`** — `.entity()` 가 내부에서 하는 일을 수동으로 수행:
 *   1) `converter.format` 으로 JSON Schema 문자열 생성
 *   2) 사용자 프롬프트에 "이 스키마에 맞춰 답하라" 지침 추가
 *   3) 응답 텍스트를 `converter.convert(text)` 로 역직렬화
 */
@Service
class VisionService(
    private val chatModel: ChatModel,
    @Value("\${app.ollama.vision-model:llava:7b}") private val visionModel: String,
) {

    private val converter = BeanOutputConverter(VisionAnswer::class.java)

    fun describe(image: MultipartFile, prompt: String): VisionAnswer {
        val mimeType: MimeType = parseMimeType(image.contentType)
        val media = Media.builder().mimeType(mimeType).data(image.bytes).build()

        val instruction = """
            $prompt

            응답은 아래 JSON 스키마를 그대로 따르세요 (자연어 설명은 description 필드에 담습니다):
            ${converter.format}
        """.trimIndent()

        val userMessage = UserMessage.builder().text(instruction).media(media).build()
        val options = OllamaChatOptions.builder().model(visionModel).build()
        val response = chatModel.call(Prompt(listOf(userMessage), options))

        val rawText = response.result.output.text ?: ""
        return runCatching { converter.convert(rawText) ?: emptyAnswer(rawText) }
            .getOrElse { emptyAnswer(rawText) }
    }

    private fun emptyAnswer(rawText: String): VisionAnswer =
        VisionAnswer(description = if (rawText.isBlank()) "이미지 분석 결과가 없습니다." else rawText)

    private fun parseMimeType(contentType: String?): MimeType {
        if (contentType.isNullOrBlank()) return MimeTypeUtils.IMAGE_PNG
        return runCatching { MimeTypeUtils.parseMimeType(contentType) }.getOrDefault(MimeTypeUtils.IMAGE_PNG)
    }
}
