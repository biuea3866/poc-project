package com.example.cachepractice.controller

import com.example.cachepractice.domain.Product
import com.example.cachepractice.service.CaffeineDirectService
import com.example.cachepractice.service.ProductService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

/**
 * Product REST API 컨트롤러
 *
 * 엔드포인트:
 * - GET /api/products/{id}: 제품 조회
 * - GET /api/products: 모든 제품 조회
 * - POST /api/products: 제품 생성
 * - PUT /api/products/{id}: 제품 수정
 * - DELETE /api/products/{id}: 제품 삭제
 * - GET /api/products/stampede-test/{id}: 캐시 스템피드 방지 테스트
 * - GET /api/products/cache/stats: 캐시 통계 조회
 */
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired(required = false)
    private var caffeineDirectService: CaffeineDirectService? = null

    /**
     * 제품 조회 (Look Aside 패턴)
     */
    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: Long): ResponseEntity<Product> {
        logger.info("제품 조회 요청: productId={}", id)
        val product = productService.getProductById(id)

        return if (product.isNotFound()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(product)
        }
    }

    /**
     * 모든 제품 조회
     */
    @GetMapping
    fun getAllProducts(): ResponseEntity<List<Product>> {
        logger.info("모든 제품 조회 요청")
        val products = productService.getAllProducts()
        return ResponseEntity.ok(products)
    }

    /**
     * 제품 생성 (Write Through 패턴)
     */
    @PostMapping
    fun createProduct(@RequestBody request: CreateProductRequest): ResponseEntity<Product> {
        logger.info("제품 생성 요청: {}", request)

        val product = Product(
            id = 0L, // Repository에서 자동 생성
            name = request.name,
            price = request.price
        )

        val savedProduct = productService.createProduct(product)
        return ResponseEntity.ok(savedProduct)
    }

    /**
     * 제품 수정 (Write Through 패턴)
     */
    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: Long,
        @RequestBody request: UpdateProductRequest
    ): ResponseEntity<Product> {
        logger.info("제품 수정 요청: productId={}, request={}", id, request)

        return try {
            val product = Product(
                id = id,
                name = request.name,
                price = request.price
            )

            val updatedProduct = productService.updateProduct(id, product)
            ResponseEntity.ok(updatedProduct)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 제품 삭제 (Invalidate 패턴)
     */
    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: Long): ResponseEntity<Void> {
        logger.info("제품 삭제 요청: productId={}", id)
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * 캐시 스템피드 방지 테스트
     * Caffeine 직접 사용 (get with mappingFunction)
     */
    @GetMapping("/stampede-test/{id}")
    fun stampedeTest(@PathVariable id: Long): ResponseEntity<Product> {
        logger.info("캐시 스템피드 테스트 요청: productId={}", id)

        val service = caffeineDirectService
            ?: return ResponseEntity.badRequest().build()

        val product = service.getProductByIdWithStampedeProtection(id)

        return if (product.isNotFound()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(product)
        }
    }

    /**
     * 캐시 통계 조회 (Caffeine만 지원)
     */
    @GetMapping("/cache/stats")
    fun getCacheStats(): ResponseEntity<String> {
        logger.info("캐시 통계 조회 요청")

        val service = caffeineDirectService
            ?: return ResponseEntity.ok("캐시 통계는 Caffeine 프로파일에서만 사용 가능합니다.")

        val stats = service.getCacheStats()
        return ResponseEntity.ok(stats)
    }

    /**
     * 캐시 초기화 (Caffeine 직접 사용)
     */
    @DeleteMapping("/cache")
    fun clearCache(): ResponseEntity<String> {
        logger.info("캐시 초기화 요청")

        caffeineDirectService?.clearCache()
            ?: return ResponseEntity.ok("캐시 초기화는 Caffeine 프로파일에서만 사용 가능합니다.")

        return ResponseEntity.ok("캐시가 초기화되었습니다.")
    }
}

/**
 * 제품 생성 요청 DTO
 */
data class CreateProductRequest(
    val name: String,
    val price: BigDecimal
)

/**
 * 제품 수정 요청 DTO
 */
data class UpdateProductRequest(
    val name: String,
    val price: BigDecimal
)
