package com.biuea.wiki

import com.biuea.wiki.domain.search.SearchMode
import com.biuea.wiki.domain.search.SearchResponse
import com.biuea.wiki.domain.search.SemanticSearchResult
import com.biuea.wiki.domain.search.SemanticSearchService
import com.biuea.wiki.infrastructure.kafka.OutboxKafkaPublisher
import com.biuea.wiki.infrastructure.kafka.OutboxScheduler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.junit.jupiter.api.BeforeEach
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import com.biuea.wiki.WikiApiApplication

/**
 * 시맨틱 검색 API 통합 테스트 (NAW-130)
 *
 * 실제 임베딩/pgvector 없이 SemanticSearchService를 MockitoBean으로 대체하여 검증.
 *
 * TC-I-1: GET /api/v1/search/semantic — 200 OK + 올바른 응답 구조
 * TC-I-2: GET /api/v1/search/integrated?mode=HYBRID — hybrid 결과 반환
 * TC-I-3: GET /api/v1/search/integrated?mode=KEYWORD — keyword 결과 반환
 * TC-I-4: GET /api/v1/search/integrated?mode=SEMANTIC — semantic 결과 반환
 * TC-I-5: q 파라미터 누락 시 400 Bad Request
 * TC-I-6: mode 파라미터 기본값 HYBRID 적용 확인
 */
@SpringBootTest(classes = [WikiApiApplication::class])
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb_semantic;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.kafka.bootstrap-servers=localhost:9999",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "datasource-vector.url=jdbc:h2:mem:testvectordb;DB_CLOSE_DELAY=-1",
    "datasource-vector.driver-class-name=org.h2.Driver",
    "datasource-vector.username=sa",
    "datasource-vector.password=",
    "spring.ai.openai.api-key=test-key",
    "security.jwt.secret=test-secret-key-for-integration-test-minimum-32-chars",
])
class SemanticSearchIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean
    private lateinit var semanticSearchService: SemanticSearchService

    @MockitoBean
    private lateinit var outboxKafkaPublisher: OutboxKafkaPublisher

    @MockitoBean
    private lateinit var outboxScheduler: OutboxScheduler

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    // TC-I-1
    @Test
    @WithMockUser
    fun `TC-I-1 시맨틱 검색 API가 올바른 응답 구조를 반환한다`() {
        val fakeResult = SearchResponse(
            items = listOf(
                SemanticSearchResult(1L, "AI 문서", "머신러닝 관련 내용", 0.92, listOf("AI")),
                SemanticSearchResult(2L, "ML 가이드", "딥러닝 개요", 0.85, listOf("ML")),
            ),
            page = 0,
            size = 10,
            totalElements = 2L,
        )
        `when`(semanticSearchService.semanticSearch("머신러닝", 0.7, 0, 10)).thenReturn(fakeResult)

        mockMvc.perform(
            get("/api/v1/search/semantic")
                .param("q", "머신러닝")
                .param("threshold", "0.7")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].documentId").value(1))
            .andExpect(jsonPath("$.items[0].title").value("AI 문서"))
            .andExpect(jsonPath("$.items[0].similarity").value(0.92))
            .andExpect(jsonPath("$.items[0].tags[0]").value("AI"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    // TC-I-2
    @Test
    @WithMockUser
    fun `TC-I-2 통합 검색 HYBRID 모드가 올바른 결과를 반환한다`() {
        val fakeResult = SearchResponse(
            items = listOf(
                SemanticSearchResult(3L, "Spring Boot", "스프링 부트 가이드", 0.88, listOf("Spring")),
            ),
            page = 0,
            size = 20,
            totalElements = 1L,
        )
        `when`(semanticSearchService.integratedSearch("spring", SearchMode.HYBRID, 0, 20)).thenReturn(fakeResult)

        mockMvc.perform(
            get("/api/v1/search/integrated")
                .param("query", "spring")
                .param("mode", "HYBRID")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].documentId").value(3))
            .andExpect(jsonPath("$.items[0].similarity").value(0.88))
    }

    // TC-I-3
    @Test
    @WithMockUser
    fun `TC-I-3 통합 검색 KEYWORD 모드가 키워드 결과를 반환한다`() {
        val fakeResult = SearchResponse(
            items = listOf(
                SemanticSearchResult(4L, "Kotlin 기초", "Kotlin 언어 소개", 0.0, emptyList()),
            ),
            page = 0,
            size = 20,
            totalElements = 1L,
        )
        `when`(semanticSearchService.integratedSearch("kotlin", SearchMode.KEYWORD, 0, 20)).thenReturn(fakeResult)

        mockMvc.perform(
            get("/api/v1/search/integrated")
                .param("query", "kotlin")
                .param("mode", "KEYWORD")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].documentId").value(4))
            .andExpect(jsonPath("$.items[0].similarity").value(0.0))
    }

    // TC-I-4
    @Test
    @WithMockUser
    fun `TC-I-4 통합 검색 SEMANTIC 모드가 시맨틱 결과를 반환한다`() {
        val fakeResult = SearchResponse(
            items = listOf(
                SemanticSearchResult(5L, "Vector Search", "pgvector 활용", 0.91, listOf("DB")),
            ),
            page = 0,
            size = 20,
            totalElements = 1L,
        )
        `when`(semanticSearchService.integratedSearch("벡터 검색", SearchMode.SEMANTIC, 0, 20)).thenReturn(fakeResult)

        mockMvc.perform(
            get("/api/v1/search/integrated")
                .param("query", "벡터 검색")
                .param("mode", "SEMANTIC")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].documentId").value(5))
            .andExpect(jsonPath("$.items[0].similarity").value(0.91))
    }

    // TC-I-5
    @Test
    @WithMockUser
    fun `TC-I-5 시맨틱 검색 q 파라미터 누락 시 400 반환`() {
        mockMvc.perform(
            get("/api/v1/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    // TC-I-6
    @Test
    @WithMockUser
    fun `TC-I-6 통합 검색 mode 파라미터 기본값은 HYBRID이다`() {
        val fakeResult = SearchResponse(items = emptyList(), page = 0, size = 20, totalElements = 0L)
        `when`(semanticSearchService.integratedSearch("test", SearchMode.HYBRID, 0, 20)).thenReturn(fakeResult)

        mockMvc.perform(
            get("/api/v1/search/integrated")
                .param("query", "test")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isArray)
    }
}
