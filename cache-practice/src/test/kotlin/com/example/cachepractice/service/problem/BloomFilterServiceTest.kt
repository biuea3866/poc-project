package com.example.cachepractice.service.problem

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
 * Bloom Filter 테스트
 *
 * 검증 항목:
 * 1. 존재하지 않는 데이터의 DB 접근 차단
 * 2. False Positive 확인
 * 3. False Negative 불가능 확인
 * 4. 메모리 효율성
 */
@SpringBootTest
@ActiveProfiles("caffeine")
class BloomFilterServiceTest {

    @Autowired
    private lateinit var service: BloomFilterService

    @Autowired
    private lateinit var repository: com.example.cachepractice.repository.ProductRepository

    @BeforeEach
    fun setup() {
        repository.resetToInitialState()
        service.clearAll()
    }

    @AfterEach
    fun cleanup() {
        service.clearAll()
    }

    @Test
    fun `블룸 필터 - 존재하지 않는 데이터 차단`() {
        println("\n=== 블룸 필터 차단 테스트 ===")

        // Given - 존재하지 않는 ID
        val nonExistentId = 9999L

        println("존재하지 않는 ID 조회: $nonExistentId")

        // When - 첫 조회
        val startTime1 = System.currentTimeMillis()
        val product1 = service.getProduct(nonExistentId)
        val duration1 = System.currentTimeMillis() - startTime1

        // When - 두 번째 조회
        val startTime2 = System.currentTimeMillis()
        val product2 = service.getProduct(nonExistentId)
        val duration2 = System.currentTimeMillis() - startTime2

        // Then
        assertTrue(product1.isNotFound())
        assertTrue(product2.isNotFound())

        println("첫 조회: ${duration1}ms (블룸 필터 차단)")
        println("두 번째 조회: ${duration2}ms (블룸 필터 차단)")

        // 블룸 필터로 차단되어 DB 접근하지 않음
        assertTrue(duration1 < 10, "블룸 필터로 즉시 차단")
        assertTrue(duration2 < 10, "블룸 필터로 즉시 차단")

        val stats = service.getStats()
        println("\n$stats")

        assertTrue(stats.contains("차단된 요청"), "차단 통계 확인")

        println("=== 차단 테스트 완료 ===\n")
    }

    @Test
    fun `블룸 필터 - 존재하는 데이터 통과`() {
        println("\n=== 블룸 필터 통과 테스트 ===")

        // Given - 존재하는 ID
        val existingId = 1L

        // When - 조회
        val startTime = System.currentTimeMillis()
        val product = service.getProduct(existingId)
        val duration = System.currentTimeMillis() - startTime

        // Then - 블룸 필터 통과하여 캐시/DB 확인
        assertFalse(product.isNotFound())
        assertTrue(duration >= 100, "블룸 필터 통과 후 DB 조회")

        println("존재하는 제품 조회: ${duration}ms (블룸 필터 통과, DB 조회)")
        println("제품: ${product.name}")

        println("=== 통과 테스트 완료 ===\n")
    }

    @Test
    fun `False Positive 가능성 확인`() {
        println("\n=== False Positive 테스트 ===")
        println("블룸 필터는 False Positive 가능 (없는데 있다고 판단)")

        // Given - 많은 제품 추가하여 블룸 필터 충전
        for (i in 1000L..1100L) {
            val product = Product(
                id = i,
                name = "Product $i",
                price = BigDecimal("${i}.00")
            )
            service.createProduct(product)
        }

        // When - 존재하지 않는 ID 조회 (False Positive 발생 가능)
        var falsePositiveCount = 0
        val testIds = (2000L..2100L)

        testIds.forEach { id ->
            val startTime = System.currentTimeMillis()
            service.getProduct(id)
            val duration = System.currentTimeMillis() - startTime

            // DB 조회 시간이 걸렸으면 False Positive
            if (duration >= 100) {
                falsePositiveCount++
                println("False Positive 발생: ID=$id (블룸 필터 통과했지만 DB에 없음)")
            }
        }

        val falsePositiveRate = (falsePositiveCount.toDouble() / testIds.count()) * 100

        println("\n총 테스트: ${testIds.count()}건")
        println("False Positive: ${falsePositiveCount}건")
        println("False Positive 비율: ${"%.2f".format(falsePositiveRate)}%")

        println()
        println("설명:")
        println("- False Positive는 해시 충돌로 인해 발생")
        println("- 없는 데이터인데 블룸 필터가 '있을 수도 있음'으로 판단")
        println("- 결과적으로 불필요한 DB 조회 발생")
        println("- 비트 배열 크기와 해시 함수 수로 비율 조절 가능")

        println("=== False Positive 테스트 완료 ===\n")
    }

    @Test
    fun `False Negative 불가능 확인`() {
        println("\n=== False Negative 불가능 테스트 ===")
        println("블룸 필터는 False Negative 불가능 (있는데 없다고 판단 X)")

        // Given - 존재하는 모든 제품
        val existingIds = listOf(1L, 2L, 3L, 4L, 5L)

        // When - 모든 제품 조회
        var falseNegativeCount = 0

        existingIds.forEach { id ->
            val product = service.getProduct(id)
            if (product.isNotFound()) {
                falseNegativeCount++
                println("⚠ False Negative 발생: ID=$id (있는데 없다고 판단)")
            } else {
                println("✓ 정상: ID=$id, ${product.name}")
            }
        }

        // Then - False Negative는 0이어야 함
        assertEquals(0, falseNegativeCount, "False Negative는 발생하지 않아야 함")

        println("\n결과: False Negative 0건 (블룸 필터 특성)")
        println("설명: 블룸 필터는 '확실히 존재하지 않음'만 보장")

        println("=== False Negative 테스트 완료 ===\n")
    }

    @Test
    fun `제품 생성 시 블룸 필터 자동 추가`() {
        println("\n=== 제품 생성 테스트 ===")

        // Given
        val newProduct = Product(
            id = 100L,
            name = "New Product",
            price = BigDecimal("100.00")
        )

        // When - 생성 (블룸 필터에 자동 추가)
        service.createProduct(newProduct)

        // Then - 블룸 필터 통과
        val product = service.getProduct(100L)
        assertFalse(product.isNotFound())
        assertEquals("New Product", product.name)

        println("제품 생성 후 조회 성공: ${product.name}")
        println("블룸 필터에 자동 추가됨")

        println("=== 생성 테스트 완료 ===\n")
    }

    @Test
    fun `제품 삭제 - 블룸 필터 한계`() {
        println("\n=== 제품 삭제 테스트 ===")
        println("블룸 필터는 삭제를 지원하지 않음")

        // Given
        val productId = 1L
        service.getProduct(productId) // 확인

        // When - 삭제
        service.deleteProduct(productId)

        // Then - 블룸 필터는 여전히 '존재 가능'으로 판단 (False Positive)
        val startTime = System.currentTimeMillis()
        val product = service.getProduct(productId)
        val duration = System.currentTimeMillis() - startTime

        assertTrue(product.isNotFound())
        println("삭제 후 조회: ${duration}ms")

        if (duration >= 100) {
            println("⚠ False Positive 발생: 블룸 필터는 여전히 '있을 수도'라고 판단")
            println("  → DB 조회 발생 (비효율)")
        }

        println()
        println("한계:")
        println("- 블룸 필터는 삭제를 지원하지 않음")
        println("- 삭제된 항목도 블룸 필터에 남아있음")
        println("- Counting Bloom Filter를 사용하면 삭제 가능")

        println("=== 삭제 테스트 완료 ===\n")
    }

    @Test
    fun `메모리 효율성 비교`() {
        println("\n=== 메모리 효율성 테스트 ===")

        // Given - 많은 제품 생성
        val productCount = 100
        for (i in 1..productCount) {
            val product = Product(
                id = 500L + i,
                name = "Product $i",
                price = BigDecimal("${i}.00")
            )
            service.createProduct(product)
        }

        // When
        val stats = service.getStats()
        println(stats)

        println()
        println("메모리 비교:")
        println("빈 값 캐싱:")
        println("  - 각 NOT_FOUND 객체를 캐시에 저장")
        println("  - 예: 1000개 NOT_FOUND → 1000개 객체 메모리 사용")
        println()
        println("블룸 필터:")
        println("  - 비트 배열만 사용 (10000 bits = 1.25KB)")
        println("  - 1000개든 10000개든 비트 배열 크기 동일")
        println("  - 메모리 효율적!")

        println("=== 효율성 테스트 완료 ===\n")
    }

    @Test
    fun `차단률 통계 확인`() {
        println("\n=== 차단률 통계 테스트 ===")

        // Given - 존재하지 않는 ID 대량 조회
        for (i in 8000L..8100L) {
            service.getProduct(i)
        }

        // When
        val stats = service.getStats()
        println(stats)

        assertTrue(stats.contains("차단률"))
        assertTrue(stats.contains("차단된 요청"))

        println()
        println("효과:")
        println("- 존재하지 않는 데이터의 DB 접근 완전 차단")
        println("- DB 부하 크게 감소")
        println("- 빠른 응답 시간")

        println("=== 통계 테스트 완료 ===\n")
    }

    @Test
    fun `블룸 필터 vs 빈 값 캐싱 비교`() {
        println("\n=== 블룸 필터 vs 빈 값 캐싱 비교 ===")
        println()
        println("빈 값 캐싱:")
        println("✓ 구현 간단")
        println("✓ False Positive 없음")
        println("✗ 메모리 사용량 높음")
        println("✗ 각 NOT_FOUND 객체를 캐시에 저장")
        println()
        println("블룸 필터:")
        println("✓ 메모리 효율적")
        println("✓ 대량의 존재하지 않는 데이터 처리에 유리")
        println("✓ 비트 배열만 사용")
        println("✗ False Positive 가능")
        println("✗ 삭제 미지원")
        println()
        println("선택 기준:")
        println("- 데이터 양이 적음: 빈 값 캐싱")
        println("- 대량의 악의적 요청 대응: 블룸 필터")
        println("- False Positive 허용 불가: 빈 값 캐싱")
        println()
        println("=== 비교 완료 ===\n")
    }
}
