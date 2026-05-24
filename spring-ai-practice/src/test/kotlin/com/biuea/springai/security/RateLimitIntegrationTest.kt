package com.biuea.springai.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.biuea.springai.service.KnowledgeBaseLoader
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate

/**
 * Rate limit 동작 검증. limit-for-period 를 5/60s 로 좁힌 뒤
 * 인증된 사용자(=subject 키)로 /sse 를 6회 호출하면 6번째에서 429 가 떨어진다.
 *
 * 5번까지는 401(미인증) 대신 보호 경로의 SSE 핸드셰이크가 진행되어 200/이외가 떨어질 수 있는데,
 * 본 테스트는 정확한 응답코드가 아닌 "429 발생 + Retry-After 헤더" 만 확인한다.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "security.rate-limit.limit-for-period=5",
        "security.rate-limit.refresh-period-seconds=60",
        "security.rate-limit.timeout-millis=0",
    ],
)
class RateLimitIntegrationTest {

    @MockitoBean
    lateinit var chatModel: ChatModel

    @MockitoBean
    lateinit var embeddingModel: EmbeddingModel

    @MockitoBean
    lateinit var knowledgeBaseLoader: KnowledgeBaseLoader

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @LocalServerPort
    var port: Int = 0

    private val rest = RestTemplate().apply { errorHandler = NoopErrorHandler() }

    private fun baseUrl(path: String) = "http://localhost:$port$path"

    @Test
    fun `S-08 한도 초과 시 429 와 Retry-After 헤더가 반환된다`() {
        // /auth/login 은 permitAll 이지만 RateLimitFilter 는 actuator 만 제외하므로
        // 같은 client IP 키로 6회 호출 시 6번째에서 429 가 떨어진다.
        val headers = HttpHeaders().apply { contentType = org.springframework.http.MediaType.APPLICATION_JSON }
        val body = """{"clientId":"shopper-llm","clientSecret":"dev-secret-1"}"""

        repeat(5) {
            val response = rest.postForEntity(
                baseUrl("/auth/login"),
                HttpEntity(body, headers),
                String::class.java,
            )
            // 200 또는 401 어느 쪽이든 통과 (인증 결과는 무관) — 429 가 아니면 OK
            assertTrue(response.statusCode.value() != 429, "${it + 1}번째 호출은 429 가 아니어야 한다")
        }

        val sixth = rest.postForEntity(
            baseUrl("/auth/login"),
            HttpEntity(body, headers),
            String::class.java,
        )
        assertEquals(429, sixth.statusCode.value())
        val retryAfter = sixth.headers.getFirst("Retry-After")
        assertTrue(retryAfter != null && retryAfter.toLong() > 0)
        val responseBody = objectMapper.readTree(sixth.body)
        assertEquals("rate_limited", responseBody.get("error").asText())
    }
}
