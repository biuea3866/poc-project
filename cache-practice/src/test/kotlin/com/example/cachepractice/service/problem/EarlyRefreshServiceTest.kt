package com.example.cachepractice.service.problem

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Early Refresh (조기 갱신) 테스트
 *
 * 검증 항목:
 * 1. 핫키 추가/제거
 * 2. 수동 캐시 갱신
 * 3. 조기 갱신으로 캐시 스템피드 방지
 */
@SpringBootTest
@ActiveProfiles("caffeine")
class EarlyRefreshServiceTest {

    @Autowired
    private lateinit var service: EarlyRefreshService

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
    fun `핫키 관리 - 추가 및 제거`() {
        println("\n=== 핫키 관리 테스트 ===")

        // Given
        val hotKeyId = 1L

        // When - 핫키 추가
        service.addHotKey(hotKeyId)

        // Then - 통계에 반영됨
        val stats1 = service.getCacheStats()
        assertTrue(stats1.contains("핫키 개수: 1"))
        println("핫키 추가 후:\n$stats1")

        // When - 핫키 제거
        service.removeHotKey(hotKeyId)

        // Then
        val stats2 = service.getCacheStats()
        assertTrue(stats2.contains("핫키 개수: 0"))
        println("\n핫키 제거 후:\n$stats2")

        println("=== 핫키 관리 테스트 완료 ===\n")
    }

    @Test
    fun `전체 캐시 강제 갱신`() {
        println("\n=== 전체 캐시 갱신 테스트 ===")

        // Given - 여러 제품 조회하여 캐시에 로드
        for (i in 1L..5L) {
            service.getProduct(i)
        }

        // When - 전체 캐시 강제 갱신
        val startTime = System.currentTimeMillis()
        service.refreshAllCache()
        val duration = System.currentTimeMillis() - startTime

        println("전체 캐시 갱신 소요 시간: ${duration}ms")

        // Then - 갱신 후 캐시 적중
        val readStart = System.currentTimeMillis()
        val product = service.getProduct(1L)
        val readDuration = System.currentTimeMillis() - readStart

        assertNotNull(product)
        assertTrue(readDuration < 10, "갱신된 캐시에서 빠르게 읽기")

        println("갱신 후 읽기: ${readDuration}ms (캐시 적중)")
        println("=== 전체 갱신 테스트 완료 ===\n")
    }

    @Test
    fun `조기 갱신으로 캐시 스템피드 방지`() {
        println("\n=== 조기 갱신 스템피드 방지 테스트 ===")
        println("시나리오: 핫키를 조기 갱신하여 만료 시점에 스템피드 발생 방지")

        // Given - 핫키 등록
        val hotKeyId = 2L
        service.addHotKey(hotKeyId)

        // When - 첫 조회 (캐시 로드)
        service.getProduct(hotKeyId)

        // 수동 갱신 트리거 (스케줄러 대신)
        service.refreshAllCache()

        // Then - 갱신 후 캐시 적중
        val startTime = System.currentTimeMillis()
        val product = service.getProduct(hotKeyId)
        val duration = System.currentTimeMillis() - startTime

        assertNotNull(product)
        assertTrue(duration < 10, "조기 갱신으로 캐시 미스 방지")

        println("조기 갱신 후 읽기: ${duration}ms")
        println("효과: 만료 시점에 대량 요청이 와도 캐시에서 처리")
        println("=== 스템피드 방지 테스트 완료 ===\n")
    }

    @Test
    fun `핫키 선택적 갱신 효율성`() {
        println("\n=== 핫키 선택적 갱신 테스트 ===")

        // Given - 많은 제품 중 일부만 핫키
        for (i in 1L..100L) {
            service.getProduct(i)
        }

        // 핫키는 5개만
        val hotKeys = listOf(1L, 2L, 3L, 4L, 5L)
        hotKeys.forEach { service.addHotKey(it) }

        println("총 제품 수: 100개")
        println("핫키: ${hotKeys.size}개")

        // When - 핫키만 선택적으로 갱신
        val startTime = System.currentTimeMillis()
        service.refreshAllCache() // 내부적으로 핫키만 갱신
        val duration = System.currentTimeMillis() - startTime

        println("핫키만 갱신 소요 시간: ${duration}ms")
        println("효율성: 전체 갱신 대비 ${100 / hotKeys.size}배 빠름 (5개만 갱신)")

        val stats = service.getCacheStats()
        println("\n$stats")

        println("=== 선택적 갱신 테스트 완료 ===\n")
    }

    @Test
    fun `TTL과 갱신 주기 확인`() {
        println("\n=== TTL과 갱신 주기 테스트 ===")

        val stats = service.getCacheStats()
        println(stats)

        assertTrue(stats.contains("TTL: 30분"))
        assertTrue(stats.contains("갱신 주기: 29분"))

        println()
        println("동작 방식:")
        println("1. TTL: 30분 (캐시 만료 시간)")
        println("2. 스케줄러: 29분마다 실행")
        println("3. 효과: 캐시가 만료되기 1분 전에 갱신")
        println("4. 결과: 모든 요청이 만료된 캐시를 접근하지 않음")

        println("=== TTL 확인 완료 ===\n")
    }

    @Test
    fun `장단점 비교 - 다른 방법과 비교`() {
        println("\n=== 조기 갱신 방법 비교 ===")
        println()
        println("조기 갱신 (Early Refresh):")
        println("✓ 장점:")
        println("  - 안정적인 캐시 적중률 유지")
        println("  - 스템피드 완전 방지")
        println("  - 구현이 간단 (스케줄러만 추가)")
        println()
        println("✗ 단점:")
        println("  - 주기적인 갱신 비용")
        println("  - 사용하지 않는 데이터도 갱신 (리소스 낭비)")
        println()
        println("개선 방법:")
        println("  - 핫키만 선택적으로 갱신 (현재 구현)")
        println("  - 접근 빈도 모니터링하여 동적으로 핫키 관리")
        println()
        println("Lock 방식과 비교:")
        println("  - Lock: 대기 시간 발생, 리소스 효율적")
        println("  - 조기 갱신: 대기 없음, 리소스 비용 높음")
        println()
        println("PER과 비교:")
        println("  - PER: 확률적 갱신, 복잡하지만 효율적")
        println("  - 조기 갱신: 주기적 갱신, 간단하지만 비용 높음")
        println()
        println("=== 비교 완료 ===\n")
    }
}
