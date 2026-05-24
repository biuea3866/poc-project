package com.biuea.springai.security

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate

/**
 * 보안 PoC End-to-End 통합 테스트 (REST 도구 노출이 없는 환경).
 *
 * 검증 대상:
 *   - 인증: /auth/login → JWT 발급, 잘못된 자격증명 401
 *   - 인가: 토큰 없이 /sse 호출 → 401
 *   - 인가: 잘못된 토큰 → 401
 *   - 정적 리소스: / 와 /actuator/health 는 permitAll
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

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

    private fun login(clientId: String, clientSecret: String): JsonNode {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = mapOf("clientId" to clientId, "clientSecret" to clientSecret)
        val response = rest.postForEntity(
            baseUrl("/auth/login"),
            HttpEntity(objectMapper.writeValueAsString(body), headers),
            String::class.java,
        )
        return objectMapper.readTree(response.body)
    }

    private fun get(path: String, token: String?): Int {
        val headers = HttpHeaders().apply { if (token != null) setBearerAuth(token) }
        val response = rest.exchange(baseUrl(path), HttpMethod.GET, HttpEntity<Any>(headers), String::class.java)
        return response.statusCode.value()
    }

    @Test
    fun `S-01 정상 자격증명 으로 JWT 가 발급된다`() {
        val token = login("shopper-llm", "dev-secret-1")
        assertNotNull(token.get("accessToken").asText())
        assertEquals("Bearer", token.get("tokenType").asText())
        val scopes = token.get("scopes").map { it.asText() }
        assertTrue(scopes.contains("catalog:read"))
        assertTrue(scopes.contains("order:read"))
    }

    @Test
    fun `S-02 잘못된 자격증명 은 401 을 반환한다`() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = """{"clientId":"shopper-llm","clientSecret":"wrong"}"""
        val response = rest.postForEntity(
            baseUrl("/auth/login"),
            HttpEntity(body, headers),
            String::class.java,
        )
        assertEquals(401, response.statusCode.value())
    }

    @Test
    fun `S-03 토큰 없이 MCP SSE 호출 시 401`() {
        assertEquals(401, get("/sse", null))
    }

    @Test
    fun `S-04 잘못된 토큰 으로 MCP SSE 호출 시 401`() {
        assertEquals(401, get("/sse", "invalid.token.value"))
    }

    @Test
    fun `S-05 catalog-only 클라이언트도 정상 자격증명이면 JWT 가 발급된다`() {
        val token = login("catalog-only-llm", "dev-secret-2")
        val scopes = token.get("scopes").map { it.asText() }
        assertEquals(listOf("catalog:read"), scopes)
    }

    @Test
    fun `S-06 UI 페이지 (정적 리소스) 는 permitAll`() {
        assertEquals(200, get("/", null))
    }

    @Test
    fun `S-07 actuator 헬스체크 는 permitAll`() {
        assertEquals(200, get("/actuator/health", null))
    }
}

class NoopErrorHandler : org.springframework.web.client.ResponseErrorHandler {
    override fun hasError(response: org.springframework.http.client.ClientHttpResponse) = false
}
