package com.example.cachepractice.service

import com.example.cachepractice.domain.Product
import com.example.cachepractice.repository.ProductRepository
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Caffeine을 직접 사용하는 서비스
 *
 * Spring 캐싱 어노테이션 대신 Caffeine API를 직접 사용하여
 * 캐시 스템피드 방지 메커니즘을 명시적으로 시연
 *
 * get(key, mappingFunction) 메서드:
 * - 동일 키에 대한 동시 요청 시 하나의 스레드만 mappingFunction 실행
 * - 나머지 스레드는 첫 번째 스레드가 완료될 때까지 대기
 * - 캐시 스템피드 자동 방지
 */
@Service
@Profile("caffeine", "default")
class CaffeineDirectService(
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val cache: Cache<Long, Product> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .recordStats()
        .build()

    /**
     * 캐시 스템피드 방지 메커니즘 시연
     *
     * get(key, mappingFunction) 사용:
     * - 캐시에 값이 없으면 mappingFunction 실행
     * - 동일 키에 대한 동시 요청은 하나의 스레드만 실행
     * - 나머지 스레드는 결과를 공유
     *
     * @param id 제품 ID
     * @return 제품 (없으면 Product.NOT_FOUND)
     */
    fun getProductByIdWithStampedeProtection(id: Long): Product {
        logger.info("캐시 조회 시작 (Stampede Protection): productId={}", id)

        val product = cache.get(id) { key ->
            logger.info("캐시 미스 - DB 조회 시작: productId={}", key)
            logger.info("현재 스레드: {}", Thread.currentThread().name)

            // 여러 스레드가 동시에 호출해도 이 블록은 한 번만 실행됨
            val result = productRepository.findById(key) ?: Product.NOT_FOUND

            logger.info("DB 조회 완료: productId={}, result={}", key, result)
            result
        }

        logger.info("캐시 조회 완료: productId={}, product={}", id, product)
        return product
    }

    /**
     * 캐시 통계 조회
     */
    fun getCacheStats(): String {
        val stats = cache.stats()
        return """
            캐시 통계:
            - 요청 수: ${stats.requestCount()}
            - 적중 수: ${stats.hitCount()}
            - 적중률: ${"%.2f".format(stats.hitRate() * 100)}%
            - 미스 수: ${stats.missCount()}
            - 미스율: ${"%.2f".format(stats.missRate() * 100)}%
            - 로드 성공: ${stats.loadSuccessCount()}
            - 평균 로드 시간: ${stats.averageLoadPenalty() / 1_000_000}ms
        """.trimIndent()
    }

    /**
     * 캐시 초기화
     */
    fun clearCache() {
        logger.info("캐시 초기화")
        cache.invalidateAll()
    }
}
