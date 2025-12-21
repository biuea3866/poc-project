package com.example.cachepractice.service.strategy

import com.example.cachepractice.domain.Product
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

/**
 * Read Through + Write Around 전략 테스트
 *
 * 검증 항목:
 * 1. Read Through: 캐시가 DB 접근을 담당
 * 2. Write Around: DB 저장 후 캐시 무효화
 * 3. 캐시 스템피드 자동 방지 (get with mappingFunction)
 */
@SpringBootTest
@ActiveProfiles("caffeine")
class ReadThroughWriteAroundServiceTest {

    @Autowired
    private lateinit var service: ReadThroughWriteAroundService

    @Autowired
    private lateinit var repository: com.example.cachepractice.repository.ProductRepository

    @BeforeEach
    fun setup() {
        repository.resetToInitialState()
        service.clearCache()
    }

    @AfterEach
    fun cleanup() {
        service.clearCache()
    }

    @Test
    fun `Read Through - 캐시가 DB 접근 담당`() {
        // Given
        val productId = 1L

        println("\n=== Read Through 읽기 테스트 시작 ===")
        println("특징: 애플리케이션은 캐시만 접근, DB 접근은 캐시가 담당")

        // When - 첫 번째 조회 (캐시 미스, 캐시가 DB에서 로드)
        val startTime1 = System.currentTimeMillis()
        val product1 = service.getProduct(productId)
        val duration1 = System.currentTimeMillis() - startTime1

        // When - 두 번째 조회 (캐시 적중)
        val startTime2 = System.currentTimeMillis()
        val product2 = service.getProduct(productId)
        val duration2 = System.currentTimeMillis() - startTime2

        // Then
        assertNotNull(product1)
        assertFalse(product1.isNotFound())
        assertEquals(product1.id, product2.id)

        // 성능 검증
        assertTrue(duration1 >= 100, "첫 조회는 캐시가 DB에서 로드 (100ms+)")
        assertTrue(duration2 < 10, "두 번째 조회는 캐시 적중 (매우 빠름)")

        println("첫 조회 (Read Through - 캐시가 DB 로드): ${duration1}ms")
        println("두 번째 조회 (캐시 적중): ${duration2}ms")
        println("=== Read Through 테스트 완료 ===\n")
    }

    @Test
    fun `Write Around - DB 저장 후 캐시 무효화`() {
        // Given
        val productId = 1L
        service.getProduct(productId) // 캐시에 로드

        println("\n=== Write Around 쓰기 테스트 시작 ===")

        // When - 제품 수정 (DB 저장, 캐시 무효화)
        val updatedProduct = Product(
            id = productId,
            name = "Updated via Write Around",
            price = BigDecimal("888.88")
        )
        service.createOrUpdateProduct(updatedProduct)

        // Then - 다음 읽기 시 캐시 미스 (캐시가 다시 DB에서 로드)
        val startTime = System.currentTimeMillis()
        val readProduct = service.getProduct(productId)
        val duration = System.currentTimeMillis() - startTime

        assertTrue(duration >= 100, "캐시 무효화로 인해 Read Through 발생")
        assertEquals("Updated via Write Around", readProduct.name)

        println("쓰기 후 첫 읽기 (Read Through): ${duration}ms")
        println("=== Write Around 테스트 완료 ===\n")
    }

    @Test
    fun `캐시 스템피드 자동 방지 - get with mappingFunction`() {
        // Given
        val productId = 2L
        val threadCount = 10

        println("\n=== Read Through 스템피드 방지 테스트 ===")
        println("Caffeine의 get(key, mappingFunction)이 자동으로 스템피드 방지")

        // When - 여러 스레드가 동시에 같은 키 요청
        val threads = (1..threadCount).map {
            Thread {
                val product = service.getProduct(productId)
                assertNotNull(product)
            }
        }

        val startTime = System.currentTimeMillis()
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        val duration = System.currentTimeMillis() - startTime

        // Then - 하나의 DB 조회만 발생 (약 100ms)
        assertTrue(duration < 300, "스템피드 방지로 대부분의 요청이 대기 후 공유")

        println("${threadCount}개 동시 요청 처리 시간: ${duration}ms")
        println("예상: 스템피드 없이 ~100ms (1번의 DB 조회)")
        println("=== 스템피드 방지 테스트 완료 ===\n")
    }

    @Test
    fun `캐시 통계 확인`() {
        // Given
        println("\n=== 캐시 통계 테스트 ===")

        // When - 여러 번 조회
        service.getProduct(1L) // 미스
        service.getProduct(1L) // 적중
        service.getProduct(1L) // 적중
        service.getProduct(2L) // 미스
        service.getProduct(2L) // 적중

        // Then
        val stats = service.getCacheStats()
        println(stats)

        // 통계 정보가 포함되어 있는지 확인 (정확한 숫자는 이전 테스트의 영향을 받을 수 있음)
        assertTrue(stats.contains("요청 수:"))
        assertTrue(stats.contains("적중 수:"))
        assertTrue(stats.contains("미스 수:"))
        assertTrue(stats.contains("적중률:"))

        println("=== 통계 테스트 완료 ===\n")
    }

    @Test
    fun `제품 삭제 - 캐시에서도 제거`() {
        // Given
        val productId = 3L
        service.getProduct(productId) // 캐시에 로드

        println("\n=== 제품 삭제 테스트 ===")

        // When - 삭제
        service.deleteProduct(productId)

        // Then - 다음 조회 시 NOT_FOUND 반환
        val product = service.getProduct(productId)
        assertTrue(product.isNotFound())

        println("삭제 후 조회: NOT_FOUND")
        println("=== 삭제 테스트 완료 ===\n")
    }

    @Test
    fun `Look Aside vs Read Through 차이점 검증`() {
        println("\n=== Look Aside vs Read Through 차이 ===")
        println()
        println("Look Aside:")
        println("- 애플리케이션이 캐시와 DB 모두 접근")
        println("- Spring @Cacheable 사용")
        println("- 애플리케이션 코드에 DB 접근 로직 포함")
        println()
        println("Read Through (현재 테스트):")
        println("- 애플리케이션은 캐시만 접근")
        println("- 캐시가 DB 접근 담당 (get with mappingFunction)")
        println("- DB 접근 로직이 캐시 레벨에 캡슐화")
        println()

        // 실제 동작 확인
        val product = service.getProduct(1L)
        assertNotNull(product)
        println("✓ Read Through 동작 확인 완료")
        println("=== 차이점 검증 완료 ===\n")
    }
}
