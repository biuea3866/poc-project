package com.example.cachepractice.service.strategy

import com.example.cachepractice.domain.Product
import com.example.cachepractice.repository.ProductRepository
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Read Through + Write Through 전략
 *
 * 읽기 전략 - Read Through:
 * - 애플리케이션은 항상 캐시만 접근
 * - 캐시 미스 시 캐시가 DB에서 데이터를 로드
 *
 * 쓰기 전략 - Write Through:
 * - 캐시와 DB에 동시에 저장
 * - 캐시-DB 일관성을 실시간으로 유지
 *
 * 장점:
 * - 가장 높은 실시간성 (즉시 캐시 업데이트)
 * - 강력한 데이터 일관성
 * - 읽기 시 항상 최신 데이터 제공
 *
 * 단점:
 * - 쓰기 성능 저하 (캐시 + DB 동시 저장)
 * - 쓰기 호출량이 많을수록 부하 증가
 * - 자주 쓰이지 않는 데이터도 캐싱 (메모리 낭비 가능)
 */
@Service
class ReadThroughWriteThroughService(
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Read Through 캐시
     */
    private val cache: Cache<Long, Product> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .recordStats()
        .build()

    /**
     * Read Through 읽기
     * 캐시가 DB 접근을 담당
     */
    fun getProduct(id: Long): Product {
        logger.info("[Read Through] 캐시 조회 시작: productId={}", id)

        val product = cache.get(id) { key ->
            logger.info("[Read Through] 캐시 미스 - 캐시가 DB에서 로드: productId={}", key)
            productRepository.findById(key) ?: Product.NOT_FOUND
        }

        logger.info("[Read Through] 캐시 조회 완료: productId={}", id)
        return product
    }

    /**
     * Write Through 쓰기
     * 캐시와 DB에 동시 저장
     */
    fun createOrUpdateProduct(product: Product): Product {
        logger.info("[Write Through] 캐시와 DB에 동시 저장 시작: {}", product)

        // 1. DB에 저장
        val saved = productRepository.save(product)
        logger.info("[Write Through] DB 저장 완료: {}", saved)

        // 2. 캐시에도 즉시 저장
        cache.put(saved.id, saved)
        logger.info("[Write Through] 캐시 저장 완료: {}", saved)

        logger.info("[Write Through] 실시간 일관성 유지 완료 - 즉시 읽기 가능")
        return saved
    }

    /**
     * 제품 삭제 (Write Through)
     * 캐시와 DB에서 동시 삭제
     */
    fun deleteProduct(id: Long) {
        logger.info("[Write Through] 캐시와 DB에서 동시 삭제: productId={}", id)

        // 1. DB에서 삭제
        productRepository.deleteById(id)
        logger.info("[Write Through] DB 삭제 완료: productId={}", id)

        // 2. 캐시에서도 삭제
        cache.invalidate(id)
        logger.info("[Write Through] 캐시 삭제 완료: productId={}", id)
    }

    /**
     * 캐시 통계 조회
     */
    fun getCacheStats(): String {
        val stats = cache.stats()
        return """
            [Read Through + Write Through] 캐시 통계:
            - 요청 수: ${stats.requestCount()}
            - 적중 수: ${stats.hitCount()}
            - 적중률: ${"%.2f".format(stats.hitRate() * 100)}%
            - 미스 수: ${stats.missCount()}
            - 로드 성공: ${stats.loadSuccessCount()}
            - 평균 로드 시간: ${stats.averageLoadPenalty() / 1_000_000}ms

            특징: 쓰기 시 즉시 캐시 업데이트로 높은 적중률 유지
        """.trimIndent()
    }

    /**
     * 전체 캐시 초기화
     */
    fun clearCache() {
        logger.info("[Read Through + Write Through] 전체 캐시 초기화")
        cache.invalidateAll()
    }
}
