package com.example.cachepractice.service.strategy

import com.example.cachepractice.domain.Product
import com.example.cachepractice.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Look Aside + Write Around 전략
 *
 * 읽기 전략 - Look Aside:
 * - 애플리케이션이 캐시에서 데이터를 먼저 조회
 * - 캐시 미스 시 DB에서 조회 후 캐시 업데이트
 * - 애플리케이션이 캐시와 DB 접근을 모두 담당
 *
 * 쓰기 전략 - Write Around:
 * - DB에만 저장하고 캐시는 업데이트하지 않음
 * - 다음 읽기 요청 시 캐시 미스 발생하여 새 데이터 로드
 *
 * 장점:
 * - 쓰기 시 캐시 업데이트 비용 없음
 * - 읽기 중심 워크로드에 적합
 *
 * 단점:
 * - 쓰기 후 즉시 읽을 때 캐시 미스 발생 (약간의 지연)
 * - 캐시 스템피드 현상 가능성
 */
@Service
class LookAsideWriteAroundService(
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Look Aside 읽기
     * 캐시 미스 시 DB 조회 후 자동으로 캐시 업데이트
     */
    @Cacheable(value = ["lookAsideProducts"], key = "#id")
    fun getProduct(id: Long): Product {
        logger.info("[Look Aside] 캐시 미스 - DB 조회: productId={}", id)
        return productRepository.findById(id) ?: Product.NOT_FOUND
    }

    /**
     * Write Around 쓰기
     * DB에만 저장, 캐시는 무효화
     */
    @CacheEvict(value = ["lookAsideProducts"], key = "#product.id")
    fun createOrUpdateProduct(product: Product): Product {
        logger.info("[Write Around] DB에만 저장, 캐시 무효화: {}", product)
        val saved = productRepository.save(product)
        logger.info("[Write Around] 다음 읽기 시 캐시 미스 발생하여 새 데이터 로드 예정")
        return saved
    }

    /**
     * 캐시 무효화
     */
    @CacheEvict(value = ["lookAsideProducts"], key = "#id")
    fun deleteProduct(id: Long) {
        logger.info("[Write Around] 제품 삭제 및 캐시 무효화: productId={}", id)
        productRepository.deleteById(id)
    }

    /**
     * 모든 캐시 초기화
     */
    @CacheEvict(value = ["lookAsideProducts"], allEntries = true)
    fun clearCache() {
        logger.info("[Look Aside + Write Around] 전체 캐시 초기화")
    }
}
