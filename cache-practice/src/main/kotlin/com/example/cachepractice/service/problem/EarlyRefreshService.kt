package com.example.cachepractice.service.problem

import com.example.cachepractice.domain.Product
import com.example.cachepractice.repository.ProductRepository
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 캐시 스템피드 해결 - 조기 갱신 (Scheduled Early Refresh)
 *
 * 문제:
 * - TTL 만료 시점에 대량의 요청이 동시에 DB로 몰림
 * - 특히 정각마다 TTL이 만료되는 경우 심각한 스템피드 발생
 *
 * 해결 방법:
 * - TTL 만료 전에 스케줄러로 미리 캐시 갱신
 * - 예: TTL 30분이면 29분에 미리 갱신
 *
 * 장점:
 * - 모든 요청이 만료된 캐시를 접근하지 않음
 * - 안정적인 캐시 적중률 유지
 *
 * 단점:
 * - 주기적으로 전체 캐시를 갱신하므로 작업 비용이 큼
 * - 사용하지 않는 데이터도 갱신 (리소스 낭비)
 */
@Service
class EarlyRefreshService(
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CACHE_TTL_MINUTES = 30L
        private const val EARLY_REFRESH_MINUTES = 29L
    }

    private val cache: Cache<Long, Product> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
        .recordStats()
        .build()

    // 핫키로 간주할 제품 ID 목록 (실제로는 모니터링으로 파악)
    private val hotKeys = mutableSetOf<Long>()

    /**
     * 제품 조회
     */
    fun getProduct(id: Long): Product {
        logger.info("[Early Refresh] 제품 조회: productId={}", id)

        // 핫키로 등록 (실제로는 접근 빈도 기반)
        hotKeys.add(id)

        val product = cache.get(id) { key ->
            logger.info("[Early Refresh] 캐시 미스 - DB 조회: productId={}", key)
            productRepository.findById(key) ?: Product.NOT_FOUND
        }

        return product
    }

    /**
     * 조기 갱신 스케줄러
     *
     * TTL: 30분
     * 갱신 주기: 29분마다
     *
     * cron: 매 29분마다 실행
     * fixedDelay: 29분 간격으로 실행 (밀리초)
     */
    @Scheduled(fixedDelay = EARLY_REFRESH_MINUTES * 60 * 1000)
    fun refreshCacheBeforeExpiry() {
        logger.info("=== [Early Refresh] 조기 갱신 시작 ===")
        logger.info("TTL: {}분, 갱신 주기: {}분", CACHE_TTL_MINUTES, EARLY_REFRESH_MINUTES)
        logger.info("핫키 개수: {}", hotKeys.size)

        val startTime = System.currentTimeMillis()
        var refreshedCount = 0

        // 핫키만 선택적으로 갱신 (최적화)
        hotKeys.forEach { productId ->
            try {
                val product = productRepository.findById(productId)
                if (product != null) {
                    cache.put(productId, product)
                    refreshedCount++
                    logger.debug("[Early Refresh] 핫키 갱신: productId={}", productId)
                }
            } catch (e: Exception) {
                logger.error("[Early Refresh] 갱신 실패: productId={}, error={}", productId, e.message)
            }
        }

        val duration = System.currentTimeMillis() - startTime
        logger.info("=== [Early Refresh] 조기 갱신 완료 ===")
        logger.info("갱신된 키: {}/{}, 소요 시간: {}ms", refreshedCount, hotKeys.size, duration)
    }

    /**
     * 전체 캐시 강제 갱신 (필요 시 수동 호출)
     */
    fun refreshAllCache() {
        logger.info("[Early Refresh] 전체 캐시 강제 갱신 시작")

        val allProducts = productRepository.findAll()
        allProducts.forEach { product ->
            cache.put(product.id, product)
        }

        logger.info("[Early Refresh] 전체 캐시 갱신 완료: {}개", allProducts.size)
    }

    /**
     * 핫키 추가
     */
    fun addHotKey(productId: Long) {
        hotKeys.add(productId)
        logger.info("[Early Refresh] 핫키 추가: productId={}, 총 핫키 수: {}", productId, hotKeys.size)
    }

    /**
     * 핫키 제거
     */
    fun removeHotKey(productId: Long) {
        hotKeys.remove(productId)
        logger.info("[Early Refresh] 핫키 제거: productId={}, 총 핫키 수: {}", productId, hotKeys.size)
    }

    /**
     * 캐시 통계
     */
    fun getCacheStats(): String {
        val stats = cache.stats()
        return """
            [Early Refresh] 캐시 통계:
            - 핫키 개수: ${hotKeys.size}
            - TTL: ${CACHE_TTL_MINUTES}분
            - 갱신 주기: ${EARLY_REFRESH_MINUTES}분
            - 요청 수: ${stats.requestCount()}
            - 적중률: ${"%.2f".format(stats.hitRate() * 100)}%
            - 미스 수: ${stats.missCount()}
        """.trimIndent()
    }

    /**
     * 캐시 초기화
     */
    fun clearCache() {
        logger.info("[Early Refresh] 전체 캐시 초기화")
        cache.invalidateAll()
        hotKeys.clear()
    }
}
