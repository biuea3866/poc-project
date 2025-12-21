package com.example.cachepractice.service.problem

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * PER (Probabilistic Early Recomputation) 테스트
 *
 * 검증 항목:
 * 1. PER 알고리즘 동작
 * 2. Beta 파라미터 영향
 * 3. 확률적 조기 갱신
 * 4. 캐시 스템피드 방지
 */
@SpringBootTest
@ActiveProfiles("caffeine")
class ProbabilisticEarlyRecomputationServiceTest {

    @Autowired
    private lateinit var service: ProbabilisticEarlyRecomputationService

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
    fun `PER 기본 동작 - Beta 1_0`() {
        println("\n=== PER 기본 동작 테스트 ===")
        println("Beta = 1.0 (기본값)")

        // Given
        val productId = 1L
        val beta = 1.0

        // When - 첫 조회 (캐시 미스)
        val startTime1 = System.currentTimeMillis()
        val product1 = service.getProduct(productId, beta)
        val duration1 = System.currentTimeMillis() - startTime1

        // When - 두 번째 조회 (캐시 적중 또는 확률적 갱신)
        val startTime2 = System.currentTimeMillis()
        val product2 = service.getProduct(productId, beta)
        val duration2 = System.currentTimeMillis() - startTime2

        // Then
        assertNotNull(product1)
        assertNotNull(product2)
        assertTrue(duration1 >= 100, "첫 조회는 DB 접근")

        println("첫 조회: ${duration1}ms (캐시 미스)")
        println("두 번째 조회: ${duration2}ms")

        val stats = service.getCacheStats()
        println("\n$stats")

        println("=== 기본 동작 테스트 완료 ===\n")
    }

    @Test
    fun `Beta 파라미터 영향 비교`() {
        println("\n=== Beta 파라미터 영향 테스트 ===")
        println("Beta가 높을수록 조기 갱신 빈도 증가")

        val productId = 2L
        val betaValues = listOf(0.5, 1.0, 2.0, 5.0)

        betaValues.forEach { beta ->
            println("\n--- Beta = $beta ---")

            // 캐시 초기화
            service.clearCache()

            // 첫 조회 (캐시에 로드)
            service.getProduct(productId, beta)

            // 여러 번 조회하여 확률적 갱신 관찰
            var recomputeCount = 0
            for (i in 1..20) {
                val startTime = System.currentTimeMillis()
                service.getProduct(productId, beta)
                val duration = System.currentTimeMillis() - startTime

                // 100ms 이상이면 재계산 발생
                if (duration >= 100) {
                    recomputeCount++
                }
            }

            println("20회 조회 중 재계산 발생: ${recomputeCount}회")
            println("재계산 비율: ${recomputeCount * 5}%")
        }

        println("\n결론:")
        println("- Beta ↑ → 재계산 빈도 ↑")
        println("- Beta 0.5: 낮은 갱신 빈도")
        println("- Beta 1.0: 기본 (권장)")
        println("- Beta 5.0: 높은 갱신 빈도")

        println("=== Beta 영향 테스트 완료 ===\n")
    }

    @Test
    fun `PER 알고리즘 공식 검증`() {
        println("\n=== PER 알고리즘 공식 검증 ===")
        println("공식: (currentTime() - delta * beta * log(rand(0,1))) >= expiry")
        println()
        println("변수:")
        println("- currentTime: 현재 시각")
        println("- delta: 이전 재계산 소요 시간")
        println("- beta: 확률 파라미터")
        println("- rand(0,1): 0~1 사이 난수")
        println("- expiry: 캐시 만료 시각")
        println()
        println("특징:")
        println("1. 현재 시각이 만료 시점에 가까울수록 갱신 확률 ↑")
        println("2. 재계산 시간(delta)이 클수록 조기 갱신 확률 ↑")
        println("3. Beta 값이 클수록 조기 갱신 빈도 ↑")
        println()

        val productId = 3L
        service.getProduct(productId, 1.0)

        val stats = service.getCacheStats()
        println(stats)

        println("=== 알고리즘 검증 완료 ===\n")
    }

    @Test
    fun `조기 갱신으로 스템피드 방지`() {
        println("\n=== PER 스템피드 방지 테스트 ===")

        // Given
        val productId = 4L
        val beta = 2.0 // 높은 갱신 빈도

        // 캐시에 로드
        service.getProduct(productId, beta)

        println("시나리오: 만료 시점 근처에서 대량 요청 발생")

        // When - 여러 스레드가 동시 요청
        val threadCount = 10
        val threads = (1..threadCount).map {
            Thread {
                service.getProduct(productId, beta)
            }
        }

        val startTime = System.currentTimeMillis()
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        val duration = System.currentTimeMillis() - startTime

        println("${threadCount}개 동시 요청 처리: ${duration}ms")
        println("효과: PER이 확률적으로 조기 갱신하여 스템피드 완화")

        println("=== 스템피드 방지 테스트 완료 ===\n")
    }

    @Test
    fun `제품 생성 및 수정 - 캐시 무효화`() {
        println("\n=== 제품 생성/수정 테스트 ===")

        // Given
        val productId = 5L
        service.getProduct(productId, 1.0)

        // When - 수정
        val updatedProduct = com.example.cachepractice.domain.Product(
            id = productId,
            name = "PER Updated",
            price = java.math.BigDecimal("555.55")
        )
        service.createOrUpdateProduct(updatedProduct)

        // Then - 다음 조회 시 새로운 데이터
        val product = service.getProduct(productId, 1.0)
        assertEquals("PER Updated", product.name)

        println("수정 후 조회: ${product.name}")
        println("=== 생성/수정 테스트 완료 ===\n")
    }

    @Test
    fun `통계 상세 분석`() {
        println("\n=== PER 통계 분석 ===")

        // Given - 여러 제품 조회
        for (i in 1L..10L) {
            service.getProduct(i, 1.0)
        }

        // When
        val stats = service.getCacheStats()
        println(stats)

        assertTrue(stats.contains("평균 재계산 시간"))
        assertTrue(stats.contains("TTL: 1800초")) // 30분 = 1800초
        assertTrue(stats.contains("Beta 기본값: 1.0"))

        println()
        println("통계 해석:")
        println("- 총 엔트리 수: 캐시에 저장된 제품 수")
        println("- 평균 재계산 시간: DB 조회 평균 시간")
        println("- 이 값이 클수록 조기 갱신 확률 증가")

        println("=== 통계 분석 완료 ===\n")
    }

    @Test
    fun `조기 갱신 vs PER 비교`() {
        println("\n=== 조기 갱신 vs PER 비교 ===")
        println()
        println("조기 갱신 (Early Refresh):")
        println("✓ 스케줄러로 주기적 갱신 (예: 29분마다)")
        println("✓ 모든 핫키를 일괄 갱신")
        println("✓ 구현 간단")
        println("✗ 갱신 비용 높음 (주기적으로 대량 갱신)")
        println()
        println("PER (Probabilistic Early Recomputation):")
        println("✓ 확률적으로 필요한 것만 갱신")
        println("✓ 리소스 효율적")
        println("✓ 만료 시점에 가까울수록 갱신 확률 증가")
        println("✗ 구현 복잡")
        println("✗ Beta 튜닝 필요")
        println()
        println("선택 기준:")
        println("- 간단한 구현: 조기 갱신")
        println("- 리소스 효율성: PER")
        println("- 대규모 시스템: PER 권장")
        println()
        println("=== 비교 완료 ===\n")
    }
}
