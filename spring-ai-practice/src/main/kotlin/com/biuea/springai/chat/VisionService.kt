package com.biuea.springai.chat

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.content.Media
import org.springframework.ai.ollama.api.OllamaChatOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import org.springframework.web.multipart.MultipartFile

/**
 * 멀티모달(Vision) 채팅 서비스. 사용자가 업로드한 이미지를 비전 모델 LLM 에 첨부해 분석한다.
 *
 * Spring AI 컴포넌트 매핑:
 *
 * - **`UserMessage`** — 사용자 메시지의 멀티모달 표현. `.text(...)` + `.media(...)` 로
 *   이미지·오디오·문서 등 비텍스트 컨텐츠를 함께 담는다.
 *
 * - **`Media`** — 첨부 콘텐츠 단위. `mimeType` + `data(byte[])` 로 구성.
 *   본 PoC 는 multipart 업로드된 이미지 바이트를 그대로 묶는다.
 *
 * - **`Prompt`** — UserMessage 와 모델 옵션을 묶어 LLM 에 전달하는 컨테이너.
 *
 * - **`ChatModel.call(Prompt)`** — ChatClient 보다 한 단계 낮은 레벨 API. 도구·메모리·advisor 없이
 *   직접 LLM 만 호출. 멀티모달 입력에 가장 단순한 경로다.
 *
 * - **`OllamaChatOptions`** — 요청별 모델 오버라이드. 채팅 기본 모델(`llama3.2:3b`) 은 비전이 안 되므로
 *   `app.ollama.vision-model`(=`llava:7b`) 로 바꿔 호출한다.
 */
@Service
class VisionService(
    private val chatModel: ChatModel,
    @Value("\${app.ollama.vision-model:llava:7b}") private val visionModel: String,
) {

    fun describe(image: MultipartFile, prompt: String): String {
        val mimeType: MimeType = parseMimeType(image.contentType)
        val media = Media.builder()
            .mimeType(mimeType)
            .data(image.bytes)
            .build()
        val userMessage = UserMessage.builder()
            .text(prompt)
            .media(media)
            .build()
        val options = OllamaChatOptions.builder().model(visionModel).build()

        val response = chatModel.call(Prompt(listOf(userMessage), options))
        return response.result.output.text ?: "이미지 분석 결과가 없습니다."
    }

    private fun parseMimeType(contentType: String?): MimeType {
        if (contentType.isNullOrBlank()) return MimeTypeUtils.IMAGE_PNG
        return runCatching { MimeTypeUtils.parseMimeType(contentType) }.getOrDefault(MimeTypeUtils.IMAGE_PNG)
    }
}
