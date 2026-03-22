package com.closet.bff.presentation

import com.closet.bff.client.ProductServiceClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ProductProxyController(
    private val productClient: ProductServiceClient,
) {

    @GetMapping("/products")
    fun getProducts(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false) minPrice: Long?,
        @RequestParam(required = false) maxPrice: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "newest") sort: String,
    ) = productClient.getProducts(categoryId, brandId, minPrice, maxPrice, page, size, sort)

    @GetMapping("/products/{id}")
    fun getProduct(@PathVariable id: Long) =
        productClient.getProduct(id)

    @PostMapping("/products")
    fun createProduct(@RequestBody request: Any) =
        productClient.createProduct(request)

    @PutMapping("/products/{id}")
    fun updateProduct(@PathVariable id: Long, @RequestBody request: Any) =
        productClient.updateProduct(id, request)

    @PatchMapping("/products/{id}/status")
    fun changeStatus(@PathVariable id: Long, @RequestBody request: Any) =
        productClient.changeStatus(id, request)

    @PostMapping("/products/{id}/options")
    fun addOption(@PathVariable id: Long, @RequestBody request: Any) =
        productClient.addOption(id, request)

    @DeleteMapping("/products/{id}/options/{optionId}")
    fun removeOption(@PathVariable id: Long, @PathVariable optionId: Long): ResponseEntity<Void> {
        productClient.removeOption(id, optionId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/categories")
    fun getCategories() = productClient.getCategories()

    @GetMapping("/brands")
    fun getBrands() = productClient.getBrands()
}
