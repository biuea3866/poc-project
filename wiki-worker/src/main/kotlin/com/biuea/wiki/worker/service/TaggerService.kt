package com.biuea.wiki.worker.service

import com.biuea.wiki.domain.event.AiTaggingRequestEvent
import com.biuea.wiki.domain.tag.entity.TagConstant
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class TaggerService(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper,
) {
    private val allowedTagConstants = TagConstant.entries.joinToString(", ")

    fun extractTags(title: String, summary: String): List<AiTaggingRequestEvent.TagItem> {
        val prompt = """
            다음 문서 요약을 분석하여 적절한 태그를 추출해줘.

            제목: $title
            요약: $summary

            허용 태그 타입: $allowedTagConstants

            JSON 배열 형식으로만 응답해줘 (다른 텍스트 없이):
            [{"name": "태그명", "tagConstant": "TECH"}]

            규칙:
            - 최대 5개의 태그
            - tagConstant는 허용 목록에서만 선택
            - name은 구체적인 키워드 (예: "Spring Boot", "Kafka", "Redis")
        """.trimIndent()

        val response = chatClient.prompt()
            .user(prompt)
            .call()
            .content()
            ?: return emptyList()

        return runCatching {
            val jsonContent = response.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            objectMapper.readValue(jsonContent, objectMapper.typeFactory.constructCollectionType(
                List::class.java, AiTaggingRequestEvent.TagItem::class.java
            ))
        }.getOrDefault(emptyList())
    }
}
