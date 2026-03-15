package com.biuea.wiki.worker.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class SummaryService(
    private val chatClient: ChatClient,
) {
    fun summarize(title: String, content: String?): String {
        val prompt = """
            다음 문서를 핵심 내용 위주로 3줄 이내로 요약해줘. 한국어로 작성해줘.

            제목: $title
            내용: ${content ?: "(내용 없음)"}

            요약:
        """.trimIndent()

        return chatClient.prompt()
            .user(prompt)
            .call()
            .content()
            ?: throw IllegalStateException("LLM 요약 응답이 비어있습니다.")
    }
}
