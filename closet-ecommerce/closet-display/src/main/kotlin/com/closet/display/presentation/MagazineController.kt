package com.closet.display.presentation

import com.closet.common.response.ApiResponse
import com.closet.display.application.dto.MagazineCreateRequest
import com.closet.display.application.dto.MagazineProductCreateRequest
import com.closet.display.application.dto.MagazineProductResponse
import com.closet.display.application.dto.MagazineResponse
import com.closet.display.application.dto.MagazineTagCreateRequest
import com.closet.display.application.dto.MagazineTagResponse
import com.closet.display.application.dto.MagazineUpdateRequest
import com.closet.display.application.service.MagazineService
import jakarta.validation.Valid
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

@RestController
@RequestMapping("/api/v1/display/magazines")
class MagazineController(
    private val magazineService: MagazineService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: MagazineCreateRequest,
    ): ApiResponse<MagazineResponse> {
        return ApiResponse.created(magazineService.create(request))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: MagazineUpdateRequest,
    ): ApiResponse<MagazineResponse> {
        return ApiResponse.ok(magazineService.update(id, request))
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): ApiResponse<MagazineResponse> {
        return ApiResponse.ok(magazineService.getById(id))
    }

    @GetMapping
    fun getPublishedList(
        @RequestParam(required = false) category: String?,
    ): ApiResponse<List<MagazineResponse>> {
        return ApiResponse.ok(magazineService.getPublishedList(category))
    }

    @PatchMapping("/{id}/publish")
    fun publish(
        @PathVariable id: Long,
    ): ApiResponse<MagazineResponse> {
        return ApiResponse.ok(magazineService.publish(id))
    }

    @PatchMapping("/{id}/unpublish")
    fun unpublish(
        @PathVariable id: Long,
    ): ApiResponse<MagazineResponse> {
        return ApiResponse.ok(magazineService.unpublish(id))
    }

    @PostMapping("/{id}/products")
    @ResponseStatus(HttpStatus.CREATED)
    fun addProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: MagazineProductCreateRequest,
    ): ApiResponse<MagazineProductResponse> {
        return ApiResponse.created(magazineService.addProduct(id, request))
    }

    @DeleteMapping("/{magazineId}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeProduct(
        @PathVariable magazineId: Long,
        @PathVariable productId: Long,
    ) {
        magazineService.removeProduct(magazineId, productId)
    }

    @PostMapping("/{id}/tags")
    @ResponseStatus(HttpStatus.CREATED)
    fun addTag(
        @PathVariable id: Long,
        @Valid @RequestBody request: MagazineTagCreateRequest,
    ): ApiResponse<MagazineTagResponse> {
        return ApiResponse.created(magazineService.addTag(id, request))
    }

    @DeleteMapping("/{magazineId}/tags/{tagName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeTag(
        @PathVariable magazineId: Long,
        @PathVariable tagName: String,
    ) {
        magazineService.removeTag(magazineId, tagName)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: Long,
    ) {
        magazineService.delete(id)
    }
}
