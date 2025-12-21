package com.example.cachepractice.service

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
 * ProductService 통합 테스트
 *
 * 검증 항목:
 * 1. 캐시 적중/미스 동작
 * 2. Create, Update, Delete 작업의 캐시 상호작용
 * 3. 캐시 관통(Cache Penetration) 해결
 */
@SpringBootTest
@ActiveProfiles("caffeine")
class ProductServiceTest {

    @Autowired
    private lateinit var productService: ProductService

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setup() {
        // 각 테스트 전 캐시 초기화
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
    }

    @AfterEach
    fun cleanup() {
        // 각 테스트 후 캐시 초기화
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
    }

    @Test
    fun `제품 조회 - 캐시 미스 후 캐시 적중`() {
        // Given
        val productId = 1L

        // When - 첫 번째 조회 (캐시 미스)
        val startTime1 = System.currentTimeMillis()
        val product1 = productService.getProductById(productId)
        val duration1 = System.currentTimeMillis() - startTime1

        // When - 두 번째 조회 (캐시 적중)
        val startTime2 = System.currentTimeMillis()
        val product2 = productService.getProductById(productId)
        val duration2 = System.currentTimeMillis() - startTime2

        // Then
        assertNotNull(product1)
        assertNotNull(product2)
        assertEquals(product1.id, product2.id)
        assertEquals(product1.name, product2.name)

        // 첫 번째 조회는 DB 접근(100ms) 시간이 걸림
        assertTrue(duration1 >= 100, "첫 번째 조회는 100ms 이상 걸려야 함 (실제: ${duration1}ms)")

        // 두 번째 조회는 캐시에서 가져오므로 훨씬 빠름
        assertTrue(duration2 < duration1, "두 번째 조회가 더 빨라야 함 (첫 번째: ${duration1}ms, 두 번째: ${duration2}ms)")

        println("캐시 미스: ${duration1}ms, 캐시 적중: ${duration2}ms")
    }

    @Test
    fun `존재하지 않는 제품 조회 - 캐시 관통 방지`() {
        // Given
        val nonExistentId = 999L

        // When - 첫 번째 조회 (캐시 미스, DB 조회)
        val startTime1 = System.currentTimeMillis()
        val product1 = productService.getProductById(nonExistentId)
        val duration1 = System.currentTimeMillis() - startTime1

        // When - 두 번째 조회 (캐시 적중, NOT_FOUND 객체 반환)
        val startTime2 = System.currentTimeMillis()
        val product2 = productService.getProductById(nonExistentId)
        val duration2 = System.currentTimeMillis() - startTime2

        // Then
        assertTrue(product1.isNotFound(), "존재하지 않는 제품은 NOT_FOUND 객체 반환")
        assertTrue(product2.isNotFound(), "캐시에서도 NOT_FOUND 객체 반환")

        // 첫 번째는 DB 조회 시간 소요
        assertTrue(duration1 >= 100, "첫 번째 조회는 100ms 이상 걸려야 함")

        // 두 번째는 캐시에서 NOT_FOUND 반환 (DB 조회 안 함)
        assertTrue(duration2 < duration1, "두 번째 조회는 캐시에서 가져오므로 빨라야 함")

        println("캐시 관통 방지 - 첫 조회: ${duration1}ms, 두 번째 조회: ${duration2}ms")
    }

    @Test
    fun `제품 생성 - 캐시 갱신 확인`() {
        // Given
        val newProduct = Product(
            id = 0L,
            name = "New Product",
            price = BigDecimal("99.99")
        )

        // When
        val savedProduct = productService.createProduct(newProduct)

        // Then - 캐시에 저장되었는지 확인
        val cachedProduct = cacheManager.getCache("products")?.get(savedProduct.id, Product::class.java)
        assertNotNull(cachedProduct)
        assertEquals(savedProduct.id, cachedProduct?.id)
        assertEquals(savedProduct.name, cachedProduct?.name)

        println("제품 생성 후 캐시 갱신 확인: $cachedProduct")
    }

    @Test
    fun `제품 수정 - 캐시 갱신 확인`() {
        // Given
        val productId = 1L
        productService.getProductById(productId) // 캐시에 로드

        val updatedData = Product(
            id = productId,
            name = "Updated Product",
            price = BigDecimal("199.99")
        )

        // When
        val updatedProduct = productService.updateProduct(productId, updatedData)

        // Then - 캐시에 업데이트된 데이터가 있는지 확인
        val cachedProduct = cacheManager.getCache("products")?.get(productId, Product::class.java)
        assertNotNull(cachedProduct)
        assertEquals("Updated Product", cachedProduct?.name)
        assertEquals(BigDecimal("199.99"), cachedProduct?.price)

        println("제품 수정 후 캐시 갱신 확인: $cachedProduct")
    }

    @Test
    fun `제품 삭제 - 캐시 무효화 확인`() {
        // Given
        val productId = 1L
        productService.getProductById(productId) // 캐시에 로드

        // 캐시에 있는지 확인
        val cachedBeforeDelete = cacheManager.getCache("products")?.get(productId, Product::class.java)
        assertNotNull(cachedBeforeDelete)

        // When
        productService.deleteProduct(productId)

        // Then - 캐시에서 제거되었는지 확인
        val cachedAfterDelete = cacheManager.getCache("products")?.get(productId)
        assertNull(cachedAfterDelete, "삭제 후 캐시에서도 제거되어야 함")

        println("제품 삭제 후 캐시 무효화 확인 완료")
    }

    @Test
    fun `모든 제품 조회 - 캐시 동작 확인`() {
        // When - 첫 번째 조회
        val startTime1 = System.currentTimeMillis()
        val products1 = productService.getAllProducts()
        val duration1 = System.currentTimeMillis() - startTime1

        // When - 두 번째 조회
        val startTime2 = System.currentTimeMillis()
        val products2 = productService.getAllProducts()
        val duration2 = System.currentTimeMillis() - startTime2

        // Then
        assertNotNull(products1)
        assertNotNull(products2)
        assertEquals(products1.size, products2.size)

        assertTrue(duration1 >= 100, "첫 번째 조회는 DB 접근 시간 소요")
        assertTrue(duration2 < duration1, "두 번째 조회는 캐시에서 가져옴")

        println("전체 조회 - 첫 조회: ${duration1}ms, 두 번째 조회: ${duration2}ms")
    }
}
