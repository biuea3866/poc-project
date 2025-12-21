package com.example.cachepractice.service.strategy

import com.example.cachepractice.domain.Product
import com.example.cachepractice.repository.ProductRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

/**
 * Look Aside + Write Around 전략 테스트
 *
 * 검증 항목:
 * 1. Look Aside 읽기: 캐시 미스 후 DB 조회 및 캐시 업데이트
 * 2. Write Around 쓰기: DB 저장 후 캐시 무효화
 * 3. 다음 읽기 시 캐시 미스 발생 확인
 */
@SpringBootTest
@ActiveProfiles("caffeine")
class LookAsideWriteAroundServiceTest {

    @Autowired
    private lateinit var service: LookAsideWriteAroundService

    @Autowired
    private lateinit var repository: ProductRepository

    @Autowired
    private lateinit var cacheManager: CacheManager

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
    fun `Look Aside 읽기 - 캐시 미스 후 캐시 적중`() {
        // Given
        val productId = 1L

        println("\n=== Look Aside 읽기 테스트 시작 ===")

        // When - 첫 번째 조회 (캐시 미스, DB 조회)
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
        assertEquals(product1.name, product2.name)

        // 성능 검증
        assertTrue(duration1 >= 100, "첫 조회는 DB 접근 시간(100ms) 이상")
        assertTrue(duration2 < duration1, "두 번째 조회는 캐시에서 가져와 빠름")

        println("첫 조회 (캐시 미스): ${duration1}ms")
        println("두 번째 조회 (캐시 적중): ${duration2}ms")
        println("성능 향상: ${duration1 / duration2.coerceAtLeast(1)}배")

        // 캐시에 데이터가 있는지 확인
        val cachedProduct = cacheManager.getCache("lookAsideProducts")?.get(productId, Product::class.java)
        assertNotNull(cachedProduct)
        assertEquals(product1.id, cachedProduct?.id)

        println("=== Look Aside 읽기 테스트 완료 ===\n")
    }

    @Test
    fun `Write Around 쓰기 - DB 저장 후 캐시 무효화`() {
        // Given
        val productId = 1L
        service.getProduct(productId) // 캐시에 로드

        println("\n=== Write Around 쓰기 테스트 시작 ===")

        // 캐시에 있는지 확인
        val cachedBefore = cacheManager.getCache("lookAsideProducts")?.get(productId)
        assertNotNull(cachedBefore, "쓰기 전 캐시에 데이터 존재")

        // When - 제품 수정 (DB에만 저장, 캐시 무효화)
        val updatedProduct = Product(
            id = productId,
            name = "Updated Product",
            price = BigDecimal("999.99")
        )
        service.createOrUpdateProduct(updatedProduct)

        // Then - 캐시가 무효화되었는지 확인
        val cachedAfter = cacheManager.getCache("lookAsideProducts")?.get(productId)
        assertNull(cachedAfter, "쓰기 후 캐시 무효화됨")

        println("쓰기 전 캐시: 존재")
        println("쓰기 후 캐시: 무효화")

        // 다음 읽기 시 캐시 미스 발생
        val startTime = System.currentTimeMillis()
        val readProduct = service.getProduct(productId)
        val duration = System.currentTimeMillis() - startTime

        // 캐시 미스로 인해 DB 조회 시간 소요
        assertTrue(duration >= 100, "캐시 미스로 DB 조회 시간 소요")
        assertEquals("Updated Product", readProduct.name)

        println("다음 읽기 (캐시 미스): ${duration}ms")
        println("=== Write Around 쓰기 테스트 완료 ===\n")
    }

    @Test
    fun `존재하지 않는 제품 조회 - NOT_FOUND 캐싱`() {
        // Given
        val nonExistentId = 9999L

        println("\n=== 존재하지 않는 제품 조회 테스트 ===")

        // When - 첫 번째 조회
        val product1 = service.getProduct(nonExistentId)

        // Then
        assertTrue(product1.isNotFound(), "NOT_FOUND 반환")

        // NOT_FOUND도 캐싱되는지 확인
        val startTime = System.currentTimeMillis()
        val product2 = service.getProduct(nonExistentId)
        val duration = System.currentTimeMillis() - startTime

        assertTrue(product2.isNotFound())
        assertTrue(duration < 50, "NOT_FOUND도 캐싱되어 빠르게 반환")

        println("두 번째 조회 (NOT_FOUND 캐시 적중): ${duration}ms")
        println("=== 테스트 완료 ===\n")
    }

    @Test
    fun `제품 삭제 - 캐시 무효화 확인`() {
        // Given
        val productId = 1L
        service.getProduct(productId) // 캐시에 로드

        println("\n=== 제품 삭제 테스트 ===")

        // 캐시 확인
        val cachedBefore = cacheManager.getCache("lookAsideProducts")?.get(productId)
        assertNotNull(cachedBefore)

        // When - 삭제
        service.deleteProduct(productId)

        // Then - 캐시에서 제거됨
        val cachedAfter = cacheManager.getCache("lookAsideProducts")?.get(productId)
        assertNull(cachedAfter, "삭제 후 캐시에서 제거됨")

        // DB에서도 삭제됨
        val fromDb = repository.findById(productId)
        assertNull(fromDb, "DB에서도 삭제됨")

        println("삭제 전 캐시: 존재")
        println("삭제 후 캐시: 제거됨")
        println("=== 테스트 완료 ===\n")
    }

    @Test
    fun `여러 제품 연속 조회 - 캐시 효율성 확인`() {
        // Given
        val productIds = listOf(1L, 2L, 3L, 4L, 5L)

        println("\n=== 여러 제품 연속 조회 테스트 ===")

        // When - 첫 번째 라운드 (모두 캐시 미스)
        val startTime1 = System.currentTimeMillis()
        productIds.forEach { id -> service.getProduct(id) }
        val duration1 = System.currentTimeMillis() - startTime1

        // When - 두 번째 라운드 (모두 캐시 적중)
        val startTime2 = System.currentTimeMillis()
        productIds.forEach { id -> service.getProduct(id) }
        val duration2 = System.currentTimeMillis() - startTime2

        // Then
        println("첫 번째 라운드 (${productIds.size}개 제품, 모두 캐시 미스): ${duration1}ms")
        println("두 번째 라운드 (${productIds.size}개 제품, 모두 캐시 적중): ${duration2}ms")
        println("성능 향상: ${duration1 / duration2.coerceAtLeast(1)}배")

        assertTrue(duration2 < duration1 / 10, "캐시 적중 시 10배 이상 빠름")

        println("=== 테스트 완료 ===\n")
    }
}
