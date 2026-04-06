package com.closet.display.presentation

import com.closet.common.response.ApiResponse
import com.closet.display.application.dto.SnapCreateRequest
import com.closet.display.application.dto.SnapLikeRequest
import com.closet.display.application.dto.SnapReportRequest
import com.closet.display.application.dto.SnapResponse
import com.closet.display.application.service.SnapService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/display/snaps")
class SnapController(
    private val snapService: SnapService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @Valid @RequestBody request: SnapCreateRequest,
    ): ApiResponse<SnapResponse> {
        return ApiResponse.created(snapService.upload(request))
    }

    @GetMapping("/feed")
    fun getFeed(): ApiResponse<List<SnapResponse>> {
        return ApiResponse.ok(snapService.getFeed())
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): ApiResponse<SnapResponse> {
        return ApiResponse.ok(snapService.getById(id))
    }

    @GetMapping("/my")
    fun getMySnaps(
        @RequestParam memberId: Long,
    ): ApiResponse<List<SnapResponse>> {
        return ApiResponse.ok(snapService.getByMember(memberId))
    }

    @PostMapping("/{id}/like")
    fun like(
        @PathVariable id: Long,
        @Valid @RequestBody request: SnapLikeRequest,
    ): ApiResponse<SnapResponse> {
        return ApiResponse.ok(snapService.like(id, request.memberId))
    }

    @DeleteMapping("/{id}/like")
    fun unlike(
        @PathVariable id: Long,
        @RequestParam memberId: Long,
    ): ApiResponse<SnapResponse> {
        return ApiResponse.ok(snapService.unlike(id, memberId))
    }

    @PostMapping("/{id}/report")
    fun report(
        @PathVariable id: Long,
        @Valid @RequestBody request: SnapReportRequest,
    ): ApiResponse<SnapResponse> {
        return ApiResponse.ok(snapService.report(id, request.memberId))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: Long,
    ) {
        snapService.delete(id)
    }
}
