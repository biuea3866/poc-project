package com.biuea.wiki.domain.search

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.domain.tag.entity.Tag
import com.biuea.wiki.domain.tag.entity.TagConstant
import com.biuea.wiki.domain.tag.entity.TagDocumentMapping
import com.biuea.wiki.domain.tag.entity.TagType
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.tag.TagDocumentMappingRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.simple.JdbcClient

/**
 * SemanticSearchService 단위 테스트 (NAW-130 인수 기준 기반)
 *
 * TC-1: KEYWORD 모드 — documentRepository LIKE 검색 결과 반환
 * TC-2: SEMANTIC 모드 — pgvector 유사도 검색 결과 반환
 * TC-3: HYBRID 모드 — 키워드 + 시맨틱 결과 합산 반환
 * TC-4: RRF 점수 — 두 목록에 모두 있는 문서가 가장 높은 점수 획득
 * TC-5: HYBRID 모드 — 시맨틱 전용 문서(키워드 미매칭)도 결과에 포함
 * TC-6: 태그가 검색 결과에 올바르게 매핑
 * TC-7: 결과 없을 때 빈 리스트 반환
 * TC-8: KEYWORD 모드 페이지네이션 파라미터 정상 전달
 */
@ExtendWith(MockitoExtension::class)
class SemanticSearchServiceTest {

    private val documentRepository: DocumentRepository = mock(DocumentRepository::class.java)
    private val tagDocumentMappingRepository: TagDocumentMappingRepository =
        mock(TagDocumentMappingRepository::class.java)
    private val objectMapper = ObjectMapper()

    private fun buildService(
        semanticResults: List<SemanticSearchResult> = emptyList(),
        hybridVectorRows: List<HybridVectorRow> = emptyList(),
    ): SemanticSearchService {
        return TestableSemanticSearchService(
            vectorJdbcClient = mock(JdbcClient::class.java),
            documentRepository = documentRepository,
            tagDocumentMappingRepository = tagDocumentMappingRepository,
            objectMapper = objectMapper,
            semanticResults = semanticResults,
            hybridVectorRows = hybridVectorRows,
        )
    }

    // TC-1
    @Test
    fun `TC-1 KEYWORD 모드는 documentRepository searchByKeyword 결과를 반환한다`() {
        val doc = makeDoc(1L, "Kotlin Coroutines", "Suspending functions")
        `when`(documentRepository.searchByKeyword(anyString(), any<Pageable>()))
            .thenReturn(PageImpl(listOf(doc)))
        `when`(tagDocumentMappingRepository.findByDocumentIdIn(any())).thenReturn(emptyList())

        val result = buildService().integratedSearch("kotlin", SearchMode.KEYWORD, page = 0, size = 10)

        assertEquals(1, result.items.size)
        assertEquals(1L, result.items[0].documentId)
        assertEquals("Kotlin Coroutines", result.items[0].title)
        assertEquals(0.0, result.items[0].similarity)
    }

    // TC-2
    @Test
    fun `TC-2 SEMANTIC 모드는 pgvector 유사도 검색 결과를 반환한다`() {
        val semanticResults = listOf(
            SemanticSearchResult(10L, "AI Paper", "ml text", 0.92),
            SemanticSearchResult(11L, "DL Guide", "deep text", 0.85),
        )
        `when`(tagDocumentMappingRepository.findByDocumentIdIn(any())).thenReturn(emptyList())

        val result = buildService(semanticResults = semanticResults)
            .integratedSearch("machine learning", SearchMode.SEMANTIC, page = 0, size = 10)

        assertEquals(2, result.items.size)
        assertEquals(10L, result.items[0].documentId)
        assertEquals(0.92, result.items[0].similarity)
    }

    // TC-3
    @Test
    fun `TC-3 HYBRID 모드는 키워드와 시맨틱 결과를 합산 반환한다`() {
        val doc1 = makeDoc(1L, "Machine Learning", "ML")
        val doc2 = makeDoc(2L, "Deep Learning", "DL")
        `when`(documentRepository.searchByKeyword(anyString(), any<Pageable>()))
            .thenReturn(PageImpl(listOf(doc1, doc2)))
        `when`(tagDocumentMappingRepository.findByDocumentIdIn(any())).thenReturn(emptyList())

        val hybridRows = listOf(
            HybridVectorRow(2L, "Deep Learning", "DL", 0.95, 1),
            HybridVectorRow(3L, "Neural Nets", "NN", 0.80, 2),
        )

        val result = buildService(hybridVectorRows = hybridRows)
            .integratedSearch("neural", SearchMode.HYBRID, page = 0, size = 10)

        assertEquals(3, result.items.size)
        val ids = result.items.map { it.documentId }.toSet()
        assertTrue(ids.containsAll(setOf(1L, 2L, 3L)))
    }

    // TC-4
    @Test
    fun `TC-4 RRF 점수 — 두 목록에 모두 있는 문서가 가장 높은 점수를 가진다`() {
        val doc1 = makeDoc(1L, "Spring Boot", "spring")
        val doc2 = makeDoc(2L, "Spring Cloud", "cloud")
        `when`(documentRepository.searchByKeyword(anyString(), any<Pageable>()))
            .thenReturn(PageImpl(listOf(doc1, doc2)))
        `when`(tagDocumentMappingRepository.findByDocumentIdIn(any())).thenReturn(emptyList())

        // doc1은 semantic rank 1 (keyword rank도 1) → 가장 높은 RRF 점수
        val hybridRows = listOf(
            HybridVectorRow(1L, "Spring Boot", "spring", 0.95, 1),
            HybridVectorRow(3L, "Other", "other", 0.70, 2),
        )

        val result = buildService(hybridVectorRows = hybridRows)
            .integratedSearch("spring", SearchMode.HYBRID, page = 0, size = 10)

        assertEquals(1L, result.items[0].documentId)
    }

    // TC-5
    @Test
    fun `TC-5 HYBRID 모드는 시맨틱 전용 문서도 결과에 포함한다`() {
        `when`(documentRepository.searchByKeyword(anyString(), any<Pageable>()))
            .thenReturn(PageImpl(emptyList()))
        `when`(tagDocumentMappingRepository.findByDocumentIdIn(any())).thenReturn(emptyList())

        val hybridRows = listOf(
            HybridVectorRow(99L, "Vector DB", "pgvector", 0.88, 1),
        )

        val result = buildService(hybridVectorRows = hybridRows)
            .integratedSearch("semantic similarity", SearchMode.HYBRID, page = 0, size = 10)

        assertEquals(1, result.items.size)
        assertEquals(99L, result.items[0].documentId)
    }

    // TC-6
    @Test
    fun `TC-6 태그가 검색 결과에 올바르게 매핑된다`() {
        val doc = makeDoc(1L, "Spring Security", "security")
        `when`(documentRepository.searchByKeyword(anyString(), any<Pageable>()))
            .thenReturn(PageImpl(listOf(doc)))

        val tagType = TagType(tagConstant = TagConstant.TECH, id = 1L)
        val tag = Tag(name = "Security", tagType = tagType, id = 1L)
        val revision = mock(DocumentRevision::class.java)
        val mapping = TagDocumentMapping(tag = tag, document = doc, documentRevision = revision)
        `when`(tagDocumentMappingRepository.findByDocumentIdIn(any())).thenReturn(listOf(mapping))

        val result = buildService().integratedSearch("spring", SearchMode.KEYWORD, page = 0, size = 10)

        assertEquals(listOf("Security"), result.items[0].tags)
    }

    // TC-7
    @Test
    fun `TC-7 결과가 없을 때 빈 리스트와 totalElements 0을 반환한다`() {
        `when`(documentRepository.searchByKeyword(anyString(), any<Pageable>()))
            .thenReturn(PageImpl(emptyList()))

        val result = buildService().integratedSearch("xyzzy_no_match", SearchMode.KEYWORD, page = 0, size = 10)

        assertTrue(result.items.isEmpty())
        assertEquals(0L, result.totalElements)
    }

    // TC-8
    @Test
    fun `TC-8 KEYWORD 모드 페이지네이션 파라미터가 정상 전달된다`() {
        val docs = (1L..5L).map { makeDoc(it, "Doc $it", "content") }
        val sliced = docs.drop(2).take(2)
        `when`(documentRepository.searchByKeyword(anyString(), any<Pageable>()))
            .thenReturn(PageImpl(sliced, PageRequest.of(1, 2), 5L))
        `when`(tagDocumentMappingRepository.findByDocumentIdIn(any())).thenReturn(emptyList())

        val result = buildService().integratedSearch("doc", SearchMode.KEYWORD, page = 1, size = 2)

        assertEquals(2, result.items.size)
        assertEquals(1, result.page)
        assertEquals(2, result.size)
        assertEquals(5L, result.totalElements)
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------
    private fun makeDoc(id: Long, title: String, content: String): Document {
        val doc = Document(title = title, content = content, status = DocumentStatus.COMPLETED, createdBy = 1L, updatedBy = 1L)
        val field = Document::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(doc, id)
        return doc
    }
}

/**
 * 테스트용 SemanticSearchService 서브클래스.
 * OpenAI embed / pgvector 쿼리를 오버라이드하여 실제 DB/API 없이 로직 검증.
 */
class TestableSemanticSearchService(
    vectorJdbcClient: JdbcClient,
    documentRepository: DocumentRepository,
    tagDocumentMappingRepository: TagDocumentMappingRepository,
    objectMapper: ObjectMapper,
    private val semanticResults: List<SemanticSearchResult>,
    private val hybridVectorRows: List<HybridVectorRow>,
) : SemanticSearchService(
    vectorJdbcClient,
    documentRepository,
    tagDocumentMappingRepository,
    "fake-key",
    "text-embedding-3-small",
    objectMapper,
) {
    override fun embed(text: String): List<Float> = List(1536) { 0.1f }

    override fun semanticSearch(query: String, threshold: Double, page: Int, size: Int): SearchResponse {
        val paged = semanticResults.drop(page * size).take(size)
        return SearchResponse(items = paged, page = page, size = size, totalElements = semanticResults.size.toLong())
    }

    override fun hybridSemanticQuery(vectorStr: String, fetchSize: Int): List<HybridVectorRow> = hybridVectorRows
}
