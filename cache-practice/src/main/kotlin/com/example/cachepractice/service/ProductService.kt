package com.example.cachepractice.service

import com.example.cachepractice.domain.Product
import com.example.cachepractice.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service

/**
 * Product 서비스
 *
 * Spring 캐싱 어노테이션을 사용한 Look Aside 패턴 구현:
 * - @Cacheable: 캐시 읽기 (Look Aside)
 * - @CachePut: 캐시 갱신 (Write Through)
 * - @CacheEvict: 캐시 무효화 (Invalidate)
 */
@Service
class ProductService(
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * ID로 제품 조회 (Look Aside 읽기 전략)
     *
     * 캐시 관통(Cache Penetration) 해결:
     * - 존재하지 않는 ID에 대해 Product.NOT_FOUND 센티널 객체를 캐싱
     * - 반복적인 DB 조회 방지
     *
     * 캐시 스템피드(Cache Stampede) 해결:
     * - Caffeine은 자동으로 get(key, mappingFunction) 메커니즘 제공
     * - 동일 키에 대한 동시 요청 시 하나의 스레드만 값을 로드
     *
     * @param id 제품 ID
     * @return 제품 (존재하지 않으면 Product.NOT_FOUND)
     */
    @Cacheable(value = ["products"], key = "#id")
    fun getProductById(id: Long): Product {
        logger.info("캐시 미스 - DB에서 제품 조회: productId={}", id)

        val product = productRepository.findById(id)

        return if (product != null) {
            logger.info("제품 찾음: {}", product)
            product
        } else {
            logger.info("제품 없음 - 센티널 객체 반환: productId={}", id)
            Product.NOT_FOUND // 캐시 관통 방지
        }
    }

    /**
     * 모든 제품 조회
     */
    @Cacheable(value = ["productList"])
    fun getAllProducts(): List<Product> {
        logger.info("캐시 미스 - DB에서 모든 제품 조회")
        return productRepository.findAll()
    }

    /**
     * 제품 생성 (Write Through 전략)
     *
     * @CachePut 사용:
     * - DB에 저장 후 캐시에도 저장
     * - 캐시와 DB의 일관성 유지
     *
     * @param product 생성할 제품
     * @return 저장된 제품
     */
    @Caching(
        put = [CachePut(value = ["products"], key = "#result.id")],
        evict = [CacheEvict(value = ["productList"], allEntries = true)]
    )
    fun createProduct(product: Product): Product {
        logger.info("제품 생성: {}", product)
        val savedProduct = productRepository.save(product)
        logger.info("캐시 갱신: {}", savedProduct)
        return savedProduct
    }

    /**
     * 제품 수정 (Write Through 전략)
     *
     * @CachePut 사용:
     * - DB 업데이트 후 캐시도 갱신
     * - 캐시-DB 일관성 유지
     *
     * @param id 제품 ID
     * @param product 수정할 제품 정보
     * @return 수정된 제품
     */
    @Caching(
        put = [CachePut(value = ["products"], key = "#id")],
        evict = [CacheEvict(value = ["productList"], allEntries = true)]
    )
    fun updateProduct(id: Long, product: Product): Product {
        logger.info("제품 수정: productId={}, newData={}", id, product)

        val existingProduct = productRepository.findById(id)
            ?: throw IllegalArgumentException("제품을 찾을 수 없습니다: id=$id")

        val updatedProduct = existingProduct.copy(
            name = product.name,
            price = product.price
        )

        productRepository.save(updatedProduct)
        logger.info("캐시 갱신: {}", updatedProduct)
        return updatedProduct
    }

    /**
     * 제품 삭제 (Invalidate 전략)
     *
     * @CacheEvict 사용:
     * - DB에서 삭제 후 캐시에서도 제거
     * - 불필요한 캐시 항목 제거
     *
     * @param id 삭제할 제품 ID
     */
    @Caching(
        evict = [
            CacheEvict(value = ["products"], key = "#id"),
            CacheEvict(value = ["productList"], allEntries = true)
        ]
    )
    fun deleteProduct(id: Long) {
        logger.info("제품 삭제: productId={}", id)
        productRepository.deleteById(id)
        logger.info("캐시 무효화 완료: productId={}", id)
    }
}
