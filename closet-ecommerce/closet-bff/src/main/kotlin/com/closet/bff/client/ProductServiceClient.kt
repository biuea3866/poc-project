package com.closet.bff.client

import com.closet.bff.dto.BrandResponse
import com.closet.bff.dto.CategoryResponse
import com.closet.bff.dto.PageResponse
import com.closet.bff.dto.ProductResponse
import com.closet.common.response.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "product-service", url = "\${service.product.url}")
interface ProductServiceClient {

    @GetMapping("/products/{id}")
    fun getProduct(@PathVariable id: Long): ApiResponse<ProductResponse>

    @GetMapping("/products")
    fun getProducts(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false) minPrice: Long?,
        @RequestParam(required = false) maxPrice: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "newest") sort: String,
    ): ApiResponse<PageResponse<ProductResponse>>

    @PostMapping("/products")
    fun createProduct(@RequestBody request: Any): Any

    @PutMapping("/products/{id}")
    fun updateProduct(@PathVariable id: Long, @RequestBody request: Any): Any

    @PatchMapping("/products/{id}/status")
    fun changeStatus(@PathVariable id: Long, @RequestBody request: Any): Any

    @PostMapping("/products/{id}/options")
    fun addOption(@PathVariable id: Long, @RequestBody request: Any): Any

    @DeleteMapping("/products/{id}/options/{optionId}")
    fun removeOption(@PathVariable id: Long, @PathVariable optionId: Long)

    @GetMapping("/categories")
    fun getCategories(): ApiResponse<List<CategoryResponse>>

    @GetMapping("/brands")
    fun getBrands(): ApiResponse<List<BrandResponse>>
}
