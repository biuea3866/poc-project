package com.closet.content.presentation

import com.closet.common.response.ApiResponse
import com.closet.content.application.dto.CoordinationCreateRequest
import com.closet.content.application.dto.CoordinationListResponse
import com.closet.content.application.dto.CoordinationProductAddRequest
import com.closet.content.application.dto.CoordinationProductResponse
import com.closet.content.application.dto.CoordinationResponse
import com.closet.content.application.service.CoordinationService
import com.closet.content.domain.enums.CoordinationStyle
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/content/coordinations")
class CoordinationController(
    private val coordinationService: CoordinationService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CoordinationCreateRequest): ApiResponse<CoordinationResponse> {
        return ApiResponse.created(coordinationService.create(request))
    }

    @GetMapping
    fun findAll(
        @RequestParam(required = false) style: CoordinationStyle?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<Page<CoordinationListResponse>> {
        val result = if (style != null) {
            coordinationService.findByStyle(style, pageable)
        } else {
            coordinationService.findAll(pageable)
        }
        return ApiResponse.ok(result)
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ApiResponse<CoordinationResponse> {
        return ApiResponse.ok(coordinationService.findById(id))
    }

    @PostMapping("/{id}/products")
    @ResponseStatus(HttpStatus.CREATED)
    fun addProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: CoordinationProductAddRequest
    ): ApiResponse<CoordinationProductResponse> {
        return ApiResponse.created(coordinationService.addProduct(id, request))
    }
}
