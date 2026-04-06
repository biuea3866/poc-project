package com.closet.product.presentation

import com.closet.common.response.ApiResponse
import com.closet.product.application.dto.CategoryCreateRequest
import com.closet.product.application.dto.CategoryResponse
import com.closet.product.application.service.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val categoryService: CategoryService,
) {
    @GetMapping
    fun findAll(): ApiResponse<List<CategoryResponse>> {
        return ApiResponse.ok(categoryService.findAll())
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CategoryCreateRequest,
    ): ApiResponse<CategoryResponse> {
        return ApiResponse.created(categoryService.create(request))
    }
}
