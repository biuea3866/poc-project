package com.closet.product.presentation

import com.closet.common.response.ApiResponse
import com.closet.product.application.dto.ProductCreateRequest
import com.closet.product.application.dto.ProductListResponse
import com.closet.product.application.dto.ProductOptionCreateRequest
import com.closet.product.application.dto.ProductOptionResponse
import com.closet.product.application.dto.ProductResponse
import com.closet.product.application.dto.ProductStatusChangeRequest
import com.closet.product.application.dto.ProductUpdateRequest
import com.closet.product.application.service.ProductService
import com.closet.product.domain.enums.ProductStatus
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val productService: ProductService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: ProductCreateRequest): ApiResponse<ProductResponse> {
        return ApiResponse.created(productService.create(request))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductUpdateRequest
    ): ApiResponse<ProductResponse> {
        return ApiResponse.ok(productService.update(id, request))
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ApiResponse<ProductResponse> {
        return ApiResponse.ok(productService.findById(id))
    }

    @GetMapping
    fun findAll(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) status: ProductStatus?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<Page<ProductListResponse>> {
        return ApiResponse.ok(productService.findAll(categoryId, brandId, minPrice, maxPrice, status, pageable))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        productService.delete(id)
    }

    @PatchMapping("/{id}/status")
    fun changeStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductStatusChangeRequest
    ): ApiResponse<ProductResponse> {
        return ApiResponse.ok(productService.changeStatus(id, request.status))
    }

    @PostMapping("/{id}/options")
    @ResponseStatus(HttpStatus.CREATED)
    fun addOption(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductOptionCreateRequest
    ): ApiResponse<ProductOptionResponse> {
        return ApiResponse.created(productService.addOption(id, request))
    }

    @DeleteMapping("/{id}/options/{optionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeOption(
        @PathVariable id: Long,
        @PathVariable optionId: Long
    ) {
        productService.removeOption(id, optionId)
    }
}
