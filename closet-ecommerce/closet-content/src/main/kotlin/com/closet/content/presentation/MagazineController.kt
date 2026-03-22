package com.closet.content.presentation

import com.closet.common.response.ApiResponse
import com.closet.content.application.dto.MagazineCreateRequest
import com.closet.content.application.dto.MagazineListResponse
import com.closet.content.application.dto.MagazineResponse
import com.closet.content.application.service.MagazineService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/content/magazines")
class MagazineController(
    private val magazineService: MagazineService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: MagazineCreateRequest): ApiResponse<MagazineResponse> {
        return ApiResponse.created(magazineService.create(request))
    }

    @GetMapping
    fun findAll(
        @RequestParam(required = false) tag: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<Page<MagazineListResponse>> {
        val result = if (tag != null) {
            magazineService.findByTag(tag, pageable)
        } else {
            magazineService.findAll(pageable)
        }
        return ApiResponse.ok(result)
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ApiResponse<MagazineResponse> {
        return ApiResponse.ok(magazineService.findById(id))
    }

    @PatchMapping("/{id}/publish")
    fun publish(@PathVariable id: Long): ApiResponse<MagazineResponse> {
        return ApiResponse.ok(magazineService.publish(id))
    }

    @PatchMapping("/{id}/archive")
    fun archive(@PathVariable id: Long): ApiResponse<MagazineResponse> {
        return ApiResponse.ok(magazineService.archive(id))
    }
}
