package com.closet.display.presentation

import com.closet.common.response.ApiResponse
import com.closet.display.application.dto.ExhibitionCreateRequest
import com.closet.display.application.dto.ExhibitionProductCreateRequest
import com.closet.display.application.dto.ExhibitionProductResponse
import com.closet.display.application.dto.ExhibitionResponse
import com.closet.display.application.service.ExhibitionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/display/exhibitions")
class ExhibitionController(
    private val exhibitionService: ExhibitionService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: ExhibitionCreateRequest,
    ): ApiResponse<ExhibitionResponse> {
        return ApiResponse.created(exhibitionService.create(request))
    }

    @GetMapping("/active")
    fun findActive(): ApiResponse<List<ExhibitionResponse>> {
        return ApiResponse.ok(exhibitionService.findActive())
    }

    @PatchMapping("/{id}/activate")
    fun activate(
        @PathVariable id: Long,
    ): ApiResponse<ExhibitionResponse> {
        return ApiResponse.ok(exhibitionService.activate(id))
    }

    @PatchMapping("/{id}/end")
    fun end(
        @PathVariable id: Long,
    ): ApiResponse<ExhibitionResponse> {
        return ApiResponse.ok(exhibitionService.end(id))
    }

    @GetMapping("/{id}/products")
    fun getProducts(
        @PathVariable id: Long,
    ): ApiResponse<List<ExhibitionProductResponse>> {
        return ApiResponse.ok(exhibitionService.getProducts(id))
    }

    @PostMapping("/{id}/products")
    @ResponseStatus(HttpStatus.CREATED)
    fun addProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: ExhibitionProductCreateRequest,
    ): ApiResponse<ExhibitionProductResponse> {
        return ApiResponse.created(exhibitionService.addProduct(id, request))
    }

    @DeleteMapping("/{exhibitionId}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeProduct(
        @PathVariable exhibitionId: Long,
        @PathVariable productId: Long,
    ) {
        exhibitionService.removeProduct(exhibitionId, productId)
    }
}
