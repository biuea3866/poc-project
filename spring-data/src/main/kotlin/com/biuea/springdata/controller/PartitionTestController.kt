package com.biuea.springdata.controller

import com.biuea.springdata.dto.ProductDto
import com.biuea.springdata.dto.ProductWithCommentsDto
import com.biuea.springdata.service.ProductService
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/partition-test")
class PartitionTestController(
    private val productService: ProductService
) {

    /**
     * API 1: 파티셔닝 미적용 상품 조회
     * GET /api/partition-test/products/non-partitioned
     */
    @GetMapping("/products/non-partitioned")
    fun getProductsNonPartitioned(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?
    ): ResponseEntity<Page<ProductDto>> {
        val products = productService.getProducts(page, size, startDate, endDate)
        return ResponseEntity.ok(products)
    }

    /**
     * API 2: 파티셔닝 적용 상품 조회
     * GET /api/partition-test/products/partitioned
     */
    @GetMapping("/products/partitioned")
    fun getProductsPartitioned(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?
    ): ResponseEntity<Page<ProductDto>> {
        val products = productService.getProductsPartitioned(page, size, startDate, endDate)
        return ResponseEntity.ok(products)
    }

    /**
     * API 3: JOIN 파티션 키 미포함
     * GET /api/partition-test/products-with-comments/without-partition-key
     *
     * 상품은 날짜 범위로 조회하지만, 댓글은 productId만으로 조회
     * -> 댓글 테이블의 모든 파티션을 스캔해야 함 (파티션 프루닝 미적용)
     */
    @GetMapping("/products-with-comments/without-partition-key")
    fun getProductsWithCommentsWithoutPartitionKey(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") productStartDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") productEndDate: LocalDate?
    ): ResponseEntity<List<ProductWithCommentsDto>> {
        val result = productService.getProductsWithCommentsWithoutPartitionKey(
            page, size, productStartDate, productEndDate
        )
        return ResponseEntity.ok(result)
    }

    /**
     * API 4: JOIN 파티션 키 포함
     * GET /api/partition-test/products-with-comments/with-partition-key
     *
     * 상품과 댓글 모두 날짜 범위로 조회
     * -> 특정 파티션만 스캔 (파티션 프루닝 적용)
     */
    @GetMapping("/products-with-comments/with-partition-key")
    fun getProductsWithCommentsWithPartitionKey(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate
    ): ResponseEntity<List<ProductWithCommentsDto>> {
        val result = productService.getProductsWithCommentsWithPartitionKey(
            page, size, startDate, endDate
        )
        return ResponseEntity.ok(result)
    }

    /**
     * Health Check
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP"))
    }
}
