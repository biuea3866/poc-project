package com.closet.display.presentation

import com.closet.common.response.ApiResponse
import com.closet.display.application.dto.BannerCreateRequest
import com.closet.display.application.dto.BannerResponse
import com.closet.display.application.dto.BannerUpdateRequest
import com.closet.display.application.service.BannerService
import com.closet.display.domain.enums.BannerPosition
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
@RequestMapping("/api/v1/display/banners")
class BannerController(
    private val bannerService: BannerService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: BannerCreateRequest): ApiResponse<BannerResponse> {
        return ApiResponse.created(bannerService.create(request))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: BannerUpdateRequest
    ): ApiResponse<BannerResponse> {
        return ApiResponse.ok(bannerService.update(id, request))
    }

    @GetMapping
    fun findActive(@RequestParam position: BannerPosition): ApiResponse<List<BannerResponse>> {
        return ApiResponse.ok(bannerService.findActive(position))
    }

    @PatchMapping("/{id}/visibility")
    fun toggleVisibility(@PathVariable id: Long): ApiResponse<BannerResponse> {
        return ApiResponse.ok(bannerService.toggleVisibility(id))
    }
}
