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
 * Read Through + Write Through 전략 테스트
 *
 * 검증 항목:
 * 1. Read Through: 캐시가 DB 접근 담당
 * 2. Write Through: 캐시와 DB에 동시 저장
 * 3. 강한 데이터 일관성
 * 4. 즉시 읽기 가능 (캐시 미스 없음)
 */
@SpringBootTest
@ActiveProfiles("caffeine")
class ReadThroughWriteThroughServiceTest {

    @Autowired
    private lateinit var service: ReadThroughWriteThroughService

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
    fun `Write Through - 쓰기 후 즉시 읽기 가능`() {
        // Given
        println("\n=== Write Through 테스트 시작 ===")
        println("특징: 캐시와 DB에 동시 저장하여 즉시 읽기 가능")

        val newProduct = Product(
            id = 100L,
            name = "Write Through Product",
            price = BigDecimal("777.77")
        )

        // When - Write Through (캐시와 DB 동시 저장)
        service.createOrUpdateProduct(newProduct)

        // Then - 즉시 읽기 가능 (캐시 적중)
        val startTime = System.currentTimeMillis()
        val readProduct = service.getProduct(100L)
        val duration = System.currentTimeMillis() - startTime

        assertEquals("Write Through Product", readProduct.name)
        assertTrue(duration < 10, "캐시에서 즉시 읽기 (캐시 미스 없음)")

        println("쓰기 후 즉시 읽기: ${duration}ms (캐시 적중)")
        println("=== Write Through 테스트 완료 ===\n")
    }

    @Test
    fun `Write Around와 Write Through 차이 비교`() {
        println("\n=== Write Around vs Write Through 비교 ===")

        // Write Through 방식
        val product1 = Product(id = 200L, name = "Product A", price = BigDecimal("100.00"))
        service.createOrUpdateProduct(product1)

        val startTime1 = System.currentTimeMillis()
        service.getProduct(200L)
        val duration1 = System.currentTimeMillis() - startTime1

        println("Write Through:")
        println("- 쓰기: 캐시 + DB 동시 저장")
        println("- 쓰기 후 첫 읽기: ${duration1}ms (캐시 적중)")
        println()

        println("Write Around (비교):")
        println("- 쓰기: DB만 저장, 캐시 무효화")
        println("- 쓰기 후 첫 읽기: ~100ms (캐시 미스, DB 조회)")
        println()

        assertTrue(duration1 < 10, "Write Through는 쓰기 후 즉시 캐시에서 읽기 가능")

        println("결론: Write Through는 실시간성이 뛰어나지만 쓰기 비용이 높음")
        println("=== 비교 테스트 완료 ===\n")
    }

    @Test
    fun `강한 데이터 일관성 검증`() {
        // Given
        val productId = 1L

        println("\n=== 강한 데이터 일관성 테스트 ===")

        // When - 여러 번 수정
        for (i in 1..5) {
            val product = Product(
                id = productId,
                name = "Version $i",
                price = BigDecimal("${i}00.00")
            )
            service.createOrUpdateProduct(product)

            // 즉시 읽기
            val read = service.getProduct(productId)
            assertEquals("Version $i", read.name, "항상 최신 버전 읽기")
            println("수정 #$i: ${read.name} - 즉시 최신 데이터 읽기 성공")
        }

        println("=== 일관성 테스트 완료 ===\n")
    }

    @Test
    fun `제품 삭제 - 캐시와 DB 동시 삭제`() {
        // Given
        val productId = 3L
        service.getProduct(productId) // 캐시에 로드

        println("\n=== 제품 삭제 테스트 ===")

        // When - 삭제 (캐시와 DB 동시 삭제)
        service.deleteProduct(productId)

        // Then - 즉시 NOT_FOUND 반환
        val startTime = System.currentTimeMillis()
        val product = service.getProduct(productId)
        val duration = System.currentTimeMillis() - startTime

        assertTrue(product.isNotFound())
        assertTrue(duration >= 100, "삭제 후 캐시에 없으므로 DB 조회 발생")

        println("삭제 후 조회: ${duration}ms (NOT_FOUND)")
        println("=== 삭제 테스트 완료 ===\n")
    }

    @Test
    fun `캐시 통계 - 높은 적중률 확인`() {
        println("\n=== 캐시 통계 테스트 ===")

        // Given - Write Through로 여러 제품 생성
        for (i in 1..10) {
            val product = Product(
                id = 1000L + i,
                name = "Product $i",
                price = BigDecimal("${i}.00")
            )
            service.createOrUpdateProduct(product)
        }

        // When - 생성한 제품들 즉시 읽기 (모두 캐시 적중)
        for (i in 1..10) {
            service.getProduct(1000L + i)
        }

        // Then
        val stats = service.getCacheStats()
        println(stats)

        assertTrue(stats.contains("특징: 쓰기 시 즉시 캐시 업데이트로 높은 적중률 유지"))

        println("=== 통계 테스트 완료 ===\n")
    }

    @Test
    fun `쓰기 성능 부하 시뮬레이션`() {
        println("\n=== 쓰기 성능 테스트 ===")
        println("Write Through의 단점: 캐시 + DB 동시 쓰기로 인한 성능 저하")

        // When - 연속 쓰기
        val writeCount = 100
        val startTime = System.currentTimeMillis()

        for (i in 1..writeCount) {
            val product = Product(
                id = 5000L + i,
                name = "Bulk Product $i",
                price = BigDecimal("${i}.00")
            )
            service.createOrUpdateProduct(product)
        }

        val duration = System.currentTimeMillis() - startTime
        val avgWriteTime = duration.toDouble() / writeCount

        println("${writeCount}개 제품 쓰기: ${duration}ms")
        println("평균 쓰기 시간: ${"%.2f".format(avgWriteTime)}ms/건")
        println()
        println("참고:")
        println("- Write Around: DB만 저장 (더 빠름)")
        println("- Write Through: 캐시 + DB 저장 (느리지만 즉시 읽기 가능)")

        println("=== 성능 테스트 완료 ===\n")
    }

    @Test
    fun `전략 특징 요약`() {
        println("\n=== Read Through + Write Through 전략 요약 ===")
        println()
        println("장점:")
        println("✓ 가장 높은 실시간성")
        println("✓ 강한 데이터 일관성")
        println("✓ 쓰기 후 즉시 읽기 가능 (캐시 미스 없음)")
        println()
        println("단점:")
        println("✗ 쓰기 성능 저하 (캐시 + DB 동시 저장)")
        println("✗ 쓰기 호출량이 많으면 부하 증가")
        println("✗ 사용하지 않는 데이터도 캐싱 (메모리 낭비 가능)")
        println()
        println("사용 시나리오:")
        println("- 강한 일관성이 필요한 경우")
        println("- 쓰기 후 즉시 읽기가 빈번한 경우")
        println("- 쓰기 빈도가 낮은 경우")
        println()
        println("=== 요약 완료 ===\n")
    }
}
