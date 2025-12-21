package com.example.cachepractice.service.problem

import com.example.cachepractice.domain.Product
import com.example.cachepractice.repository.ProductRepository
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.BitSet
import java.util.concurrent.TimeUnit

/**
 * 캐시 관통 해결 - 블룸 필터 (Bloom Filter)
 *
 * 문제:
 * - 존재하지 않는 데이터를 반복 조회 시 매번 DB 접근
 * - 빈 값 캐싱도 메모리 사용
 *
 * 해결 방법:
 * - 블룸 필터로 존재 여부를 먼저 체크
 * - 존재하지 않으면 DB 접근 없이 즉시 반환
 *
 * 블룸 필터 동작:
 * 1. 데이터 삽입: 여러 해시 함수로 비트 배열의 위치를 true로 설정
 * 2. 데이터 조회: 모든 해시 위치가 true면 "존재할 수도 있음"
 * 3. 하나라도 false면 "확실히 존재하지 않음"
 *
 * 특징:
 * - False Positive 가능 (없는데 있다고 판단)
 * - False Negative 불가능 (있는데 없다고 판단 X)
 * - 메모리 효율적
 *
 * 장점:
 * - 존재하지 않는 데이터에 대한 DB 접근 완전 차단
 * - 빈 값 캐싱 대비 메모리 효율적
 * - 빠른 조회 (O(k), k는 해시 함수 개수)
 *
 * 단점:
 * - False Positive로 인한 불필요한 조회 가능
 * - 삭제 연산 미지원 (Counting Bloom Filter 필요)
 */
@Service
class BloomFilterService(
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BLOOM_FILTER_SIZE = 10000 // 비트 배열 크기
        private const val HASH_FUNCTION_COUNT = 3 // 해시 함수 개수
    }

    /**
     * 간단한 블룸 필터 구현
     */
    class SimpleBloomFilter(
        private val size: Int,
        private val hashCount: Int
    ) {
        private val bitSet = BitSet(size)

        /**
         * 요소 추가
         */
        fun add(value: Long) {
            getHashPositions(value).forEach { pos ->
                bitSet.set(pos)
            }
        }

        /**
         * 요소 존재 여부 확인
         * true: 존재할 수도 있음 (False Positive 가능)
         * false: 확실히 존재하지 않음
         */
        fun mightContain(value: Long): Boolean {
            return getHashPositions(value).all { pos ->
                bitSet.get(pos)
            }
        }

        /**
         * 해시 위치 계산
         */
        private fun getHashPositions(value: Long): List<Int> {
            val positions = mutableListOf<Int>()

            for (i in 0 until hashCount) {
                // 간단한 해시 함수: (value * prime + seed) % size
                val prime = 31L + i
                val seed = i * 17L
                val hash = ((value * prime + seed) % size).toInt()
                positions.add(hash.coerceIn(0, size - 1))
            }

            return positions
        }

        /**
         * 블룸 필터 초기화
         */
        fun clear() {
            bitSet.clear()
        }

        /**
         * 통계
         */
        fun getStats(): String {
            val setBits = bitSet.cardinality()
            val fillRate = (setBits.toDouble() / size) * 100

            return """
                Bloom Filter 통계:
                - 비트 배열 크기: $size
                - 해시 함수 개수: $hashCount
                - 설정된 비트: $setBits
                - 충전율: ${"%.2f".format(fillRate)}%
            """.trimIndent()
        }
    }

    private val bloomFilter = SimpleBloomFilter(BLOOM_FILTER_SIZE, HASH_FUNCTION_COUNT)

    private val cache: Cache<Long, Product> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .recordStats()
        .build()

    // 통계
    private var bloomFilterHits = 0L // 블룸 필터로 차단한 요청
    private var bloomFilterMisses = 0L // 블룸 필터 통과 (False Positive 포함)

    init {
        // 초기 데이터 로드 및 블룸 필터 초기화
        initializeBloomFilter()
    }

    /**
     * 블룸 필터 초기화
     * 기존 모든 제품 ID를 블룸 필터에 추가
     */
    private fun initializeBloomFilter() {
        logger.info("[Bloom Filter] 초기화 시작")

        val allProducts = productRepository.findAll()
        allProducts.forEach { product ->
            bloomFilter.add(product.id)
        }

        logger.info("[Bloom Filter] 초기화 완료: {}개 제품 등록", allProducts.size)
    }

    /**
     * 제품 조회 with Bloom Filter
     */
    fun getProduct(id: Long): Product {
        logger.info("[Bloom Filter] 제품 조회: productId={}", id)

        // 1. 블룸 필터로 먼저 체크
        if (!bloomFilter.mightContain(id)) {
            // 확실히 존재하지 않음
            bloomFilterHits++
            logger.info("[Bloom Filter] 존재하지 않음 확정 - DB 접근 차단: productId={}", id)
            return Product.NOT_FOUND
        }

        bloomFilterMisses++
        logger.info("[Bloom Filter] 존재 가능성 있음 - 캐시/DB 확인: productId={}", id)

        // 2. 캐시 확인
        val product = cache.get(id) { key ->
            logger.info("[Bloom Filter] 캐시 미스 - DB 조회: productId={}", key)
            val result = productRepository.findById(key)

            if (result == null) {
                logger.warn("[Bloom Filter] False Positive 발생 - DB에 없는데 블룸 필터 통과: productId={}", key)
                Product.NOT_FOUND
            } else {
                result
            }
        }

        return product
    }

    /**
     * 제품 생성
     */
    fun createProduct(product: Product): Product {
        logger.info("[Bloom Filter] 제품 생성: {}", product)

        val saved = productRepository.save(product)

        // 블룸 필터에 추가
        bloomFilter.add(saved.id)
        logger.info("[Bloom Filter] 블룸 필터에 추가: productId={}", saved.id)

        // 캐시에도 추가
        cache.put(saved.id, saved)

        return saved
    }

    /**
     * 제품 수정
     */
    fun updateProduct(id: Long, product: Product): Product {
        logger.info("[Bloom Filter] 제품 수정: productId={}", id)

        val updated = product.copy(id = id)
        val saved = productRepository.save(updated)

        // 캐시 업데이트
        cache.put(id, saved)

        return saved
    }

    /**
     * 제품 삭제
     * 주의: 블룸 필터는 삭제를 지원하지 않음
     */
    fun deleteProduct(id: Long) {
        logger.warn(
            "[Bloom Filter] 제품 삭제: productId={}, " +
                    "블룸 필터는 삭제를 지원하지 않아 False Positive 가능",
            id
        )

        productRepository.deleteById(id)
        cache.invalidate(id)

        // 블룸 필터는 그대로 유지 (삭제 미지원)
        // Counting Bloom Filter를 사용하면 삭제 가능
    }

    /**
     * 블룸 필터 및 캐시 통계
     */
    fun getStats(): String {
        val cacheStats = cache.stats()
        val bloomFilterStats = bloomFilter.getStats()

        val totalBloomRequests = bloomFilterHits + bloomFilterMisses
        val blockRate = if (totalBloomRequests > 0) {
            (bloomFilterHits.toDouble() / totalBloomRequests) * 100
        } else {
            0.0
        }

        return """
            [Bloom Filter Service] 통계:

            블룸 필터 효과:
            - 차단된 요청: $bloomFilterHits
            - 통과한 요청: $bloomFilterMisses (False Positive 포함)
            - 총 요청: $totalBloomRequests
            - 차단률: ${"%.2f".format(blockRate)}%

            $bloomFilterStats

            캐시 통계:
            - 요청 수: ${cacheStats.requestCount()}
            - 적중률: ${"%.2f".format(cacheStats.hitRate() * 100)}%
        """.trimIndent()
    }

    /**
     * 캐시 및 블룸 필터 초기화
     */
    fun clearAll() {
        logger.info("[Bloom Filter] 캐시 및 블룸 필터 초기화")
        cache.invalidateAll()
        bloomFilter.clear()
        bloomFilterHits = 0
        bloomFilterMisses = 0
        initializeBloomFilter()
    }
}
