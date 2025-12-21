package com.example.cachepractice.service.problem

import com.example.cachepractice.domain.Product
import com.example.cachepractice.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ln
import kotlin.random.Random

/**
 * 캐시 스템피드 해결 - PER (Probabilistic Early Recomputation)
 *
 * 문제:
 * - 조기 갱신은 모든 캐시를 주기적으로 갱신하여 비용이 큼
 * - Lock 방식은 대기 시간이 발생
 *
 * 해결 방법:
 * - 확률적으로 만료 전에 갱신
 * - 공식: (currentTime() - delta * beta * log(rand(0,1))) >= expiry
 *
 * 변수:
 * - delta: 이전 재계산 소요 시간
 * - beta: 확률 파라미터 (높을수록 갱신 빈도 증가)
 * - expiry: 캐시 만료 시각
 * - 현재 시각이 만료 시점에 가까울수록 갱신 확률 증가
 *
 * 장점:
 * - 조기 갱신보다 효율적 (필요한 것만 갱신)
 * - Lock 방식보다 대기 시간 없음
 * - 만료 시점 근처에서만 갱신
 *
 * 단점:
 * - 구현이 복잡
 * - beta 파라미터 튜닝 필요
 */
@Service
class ProbabilisticEarlyRecomputationService(
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TTL_MILLIS = 30 * 60 * 1000L // 30분
        private const val DEFAULT_BETA = 1.0 // 확률 파라미터
    }

    /**
     * 캐시 엔트리
     */
    data class CacheEntry(
        val value: Product,
        val expiryTime: Long, // 만료 시각 (timestamp)
        val delta: Long // 이전 재계산 소요 시간 (ms)
    )

    private val cache = ConcurrentHashMap<Long, CacheEntry>()

    /**
     * 제품 조회 with PER
     */
    fun getProduct(id: Long, beta: Double = DEFAULT_BETA): Product {
        logger.info("[PER] 제품 조회 시작: productId={}, beta={}", id, beta)

        val entry = cache[id]

        // 캐시 미스 또는 갱신 필요 판정
        if (entry == null || shouldRecompute(entry, beta)) {
            if (entry == null) {
                logger.info("[PER] 캐시 미스 - DB에서 로드")
            } else {
                logger.info("[PER] 확률적 조기 갱신 필요 판정")
                logger.info(
                    "[PER] 현재 시각: {}, 만료 시각: {}, delta: {}ms, beta: {}",
                    System.currentTimeMillis(), entry.expiryTime, entry.delta, beta
                )
            }

            return recomputeAndCache(id)
        }

        // 만료 여부 체크
        if (System.currentTimeMillis() > entry.expiryTime) {
            logger.info("[PER] 캐시 만료 - 재계산")
            return recomputeAndCache(id)
        }

        logger.info("[PER] 캐시 적중: productId={}", id)
        return entry.value
    }

    /**
     * PER 알고리즘: 재계산 필요 여부 판정
     *
     * 공식: (currentTime() - delta * beta * log(rand(0,1))) >= expiry
     *
     * 해석:
     * - log(rand(0,1))는 항상 음수
     * - delta * beta * log(rand(0,1))는 음수
     * - currentTime() - (음수) = currentTime() + (양수)
     * - 즉, 현재 시각을 미래로 이동시켜 만료 시각과 비교
     * - 만료 시점에 가까울수록, delta가 클수록 갱신 확률 증가
     */
    private fun shouldRecompute(entry: CacheEntry, beta: Double): Boolean {
        val currentTime = System.currentTimeMillis()
        val random = Random.nextDouble(0.0, 1.0)

        // log(0)을 피하기 위해 최소값 설정
        val randomValue = random.coerceAtLeast(0.0001)

        val xfetch = currentTime - entry.delta * beta * ln(randomValue)

        val shouldRecompute = xfetch >= entry.expiryTime

        if (shouldRecompute) {
            logger.debug(
                "[PER] 재계산 필요: xfetch={}, expiry={}, " +
                        "current={}, delta={}, beta={}, rand={}",
                xfetch, entry.expiryTime, currentTime, entry.delta, beta, randomValue
            )
        }

        return shouldRecompute
    }

    /**
     * 재계산 및 캐시 업데이트
     */
    private fun recomputeAndCache(id: Long): Product {
        val startTime = System.currentTimeMillis()

        // DB에서 조회
        val product = productRepository.findById(id) ?: Product.NOT_FOUND

        val delta = System.currentTimeMillis() - startTime
        val expiryTime = System.currentTimeMillis() + TTL_MILLIS

        // 캐시에 저장
        val entry = CacheEntry(
            value = product,
            expiryTime = expiryTime,
            delta = delta
        )
        cache[id] = entry

        logger.info(
            "[PER] 캐시 업데이트: productId={}, delta={}ms, expiry={}",
            id, delta, expiryTime
        )

        return product
    }

    /**
     * 제품 생성/수정
     */
    fun createOrUpdateProduct(product: Product): Product {
        logger.info("[PER] 제품 저장 및 캐시 무효화: {}", product)
        val saved = productRepository.save(product)
        cache.remove(saved.id)
        return saved
    }

    /**
     * 제품 삭제
     */
    fun deleteProduct(id: Long) {
        logger.info("[PER] 제품 삭제 및 캐시 무효화: productId={}", id)
        productRepository.deleteById(id)
        cache.remove(id)
    }

    /**
     * 캐시 통계
     */
    fun getCacheStats(): String {
        val now = System.currentTimeMillis()
        val totalEntries = cache.size
        val expiredEntries = cache.values.count { it.expiryTime < now }
        val avgDelta = if (cache.isEmpty()) 0L else cache.values.map { it.delta }.average().toLong()

        return """
            [PER] 캐시 통계:
            - 총 엔트리 수: $totalEntries
            - 만료된 엔트리: $expiredEntries
            - 평균 재계산 시간: ${avgDelta}ms
            - TTL: ${TTL_MILLIS / 1000}초
            - Beta 기본값: $DEFAULT_BETA

            알고리즘 특징:
            - 만료 시점에 가까울수록 갱신 확률 증가
            - 재계산 시간(delta)이 클수록 조기 갱신 확률 증가
            - Beta 값이 클수록 조기 갱신 빈도 증가
        """.trimIndent()
    }

    /**
     * 캐시 초기화
     */
    fun clearCache() {
        logger.info("[PER] 전체 캐시 초기화")
        cache.clear()
    }

    /**
     * Beta 파라미터 테스트용 메서드
     */
    fun testBetaImpact(id: Long, betaValues: List<Double>) {
        logger.info("=== [PER] Beta 파라미터 영향 테스트 ===")

        betaValues.forEach { beta ->
            logger.info("Beta = {}", beta)
            val product = getProduct(id, beta)
            logger.info("결과: {}\n", product)
        }
    }
}
