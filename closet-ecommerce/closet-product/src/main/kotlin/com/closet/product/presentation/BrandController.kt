package com.closet.product.presentation

import com.closet.common.response.ApiResponse
import com.closet.product.application.dto.BrandCreateRequest
import com.closet.product.application.dto.BrandResponse
import com.closet.product.application.service.BrandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/brands")
class BrandController(
    private val brandService: BrandService,
) {
    @GetMapping
    fun findAll(): ApiResponse<List<BrandResponse>> {
        return ApiResponse.ok(brandService.findAll())
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: BrandCreateRequest,
    ): ApiResponse<BrandResponse> {
        return ApiResponse.created(brandService.create(request))
    }
}
