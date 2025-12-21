package com.example.cachepractice.service.strategy

import com.example.cachepractice.domain.Product
import com.example.cachepractice.repository.ProductRepository
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Read Through + Write Around 전략
 *
 * 읽기 전략 - Read Through:
 * - 애플리케이션은 항상 캐시만 접근
 * - 캐시 미스 시 캐시가 DB에서 데이터를 로드 (CacheLoader 사용)
 * - 캐시가 DB 접근을 담당하여 애플리케이션 로직 단순화
 *
 * 쓰기 전략 - Write Around:
 * - DB에만 저장하고 캐시는 무효화
 * - 다음 읽기 요청 시 캐시가 자동으로 DB에서 로드
 *
 * 장점:
 * - 캐시가 단일 진입점 (애플리케이션은 캐시만 접근)
 * - DB 접근 로직이 캐시 레벨에 캡슐화
 * - Look Aside 대비 리소스 효율적
 *
 * 단점:
 * - 캐시가 단일 장애 포인트 (SPOF)
 * - 초기 구현이 복잡
 */
@Service
class ReadThroughWriteAroundService(
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Read Through 캐시
     * get(key, mappingFunction)으로 캐시 미스 시 자동 로드
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

        // 캐시가 자동으로 DB에서 로드
        val product = cache.get(id) { key ->
            logger.info("[Read Through] 캐시 미스 - 캐시가 DB에서 로드: productId={}", key)
            logger.info("[Read Through] 애플리케이션은 캐시만 접근, DB 접근은 캐시가 담당")
            productRepository.findById(key) ?: Product.NOT_FOUND
        }

        logger.info("[Read Through] 캐시 조회 완료: productId={}", id)
        return product
    }

    /**
     * Write Around 쓰기
     * DB에만 저장하고 캐시는 무효화
     */
    fun createOrUpdateProduct(product: Product): Product {
        logger.info("[Write Around] DB에만 저장, 캐시 무효화: {}", product)

        val saved = productRepository.save(product)
        cache.invalidate(saved.id)

        logger.info("[Write Around] 다음 읽기 시 캐시가 자동으로 DB에서 로드")
        return saved
    }

    /**
     * 캐시 무효화 및 삭제
     */
    fun deleteProduct(id: Long) {
        logger.info("[Write Around] 제품 삭제 및 캐시 무효화: productId={}", id)
        productRepository.deleteById(id)
        cache.invalidate(id)
    }

    /**
     * 캐시 통계 조회
     */
    fun getCacheStats(): String {
        val stats = cache.stats()
        return """
            [Read Through + Write Around] 캐시 통계:
            - 요청 수: ${stats.requestCount()}
            - 적중 수: ${stats.hitCount()}
            - 적중률: ${"%.2f".format(stats.hitRate() * 100)}%
            - 미스 수: ${stats.missCount()}
            - 로드 성공: ${stats.loadSuccessCount()}
            - 평균 로드 시간: ${stats.averageLoadPenalty() / 1_000_000}ms
        """.trimIndent()
    }

    /**
     * 전체 캐시 초기화
     */
    fun clearCache() {
        logger.info("[Read Through + Write Around] 전체 캐시 초기화")
        cache.invalidateAll()
    }
}
