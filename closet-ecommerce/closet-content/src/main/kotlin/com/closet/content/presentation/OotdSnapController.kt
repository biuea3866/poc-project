package com.closet.content.presentation

import com.closet.common.response.ApiResponse
import com.closet.content.application.dto.OotdSnapCreateRequest
import com.closet.content.application.dto.OotdSnapListResponse
import com.closet.content.application.dto.OotdSnapResponse
import com.closet.content.application.service.OotdSnapService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/content/ootd-snaps")
class OotdSnapController(
    private val ootdSnapService: OotdSnapService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestHeader("X-Member-Id") memberId: Long,
        @Valid @RequestBody request: OotdSnapCreateRequest
    ): ApiResponse<OotdSnapResponse> {
        return ApiResponse.created(ootdSnapService.create(memberId, request))
    }

    @GetMapping
    fun findAll(
        @RequestParam(required = false) memberId: Long?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<Page<OotdSnapListResponse>> {
        val result = if (memberId != null) {
            ootdSnapService.findByMember(memberId, pageable)
        } else {
            ootdSnapService.findAll(pageable)
        }
        return ApiResponse.ok(result)
    }

    @PostMapping("/{id}/like")
    fun like(@PathVariable id: Long): ApiResponse<OotdSnapResponse> {
        return ApiResponse.ok(ootdSnapService.like(id))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        ootdSnapService.delete(id)
    }
}
