package com.closet.notification.presentation

import com.closet.common.response.ApiResponse
import com.closet.notification.application.NotificationTemplateService
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import com.closet.notification.presentation.dto.CreateTemplateRequest
import com.closet.notification.presentation.dto.TemplateResponse
import com.closet.notification.presentation.dto.UpdateTemplateRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications/templates")
class NotificationTemplateController(
    private val templateService: NotificationTemplateService,
) {

    /** 템플릿 조회 (유형+채널) */
    @GetMapping
    fun getTemplates(
        @RequestParam type: NotificationType,
        @RequestParam channel: NotificationChannel,
    ): ApiResponse<List<TemplateResponse>> {
        return ApiResponse.ok(templateService.findByTypeAndChannel(type, channel))
    }

    /** 템플릿 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTemplate(@Valid @RequestBody request: CreateTemplateRequest): ApiResponse<TemplateResponse> {
        return ApiResponse.created(templateService.create(request))
    }

    /** 템플릿 수정 */
    @PutMapping("/{id}")
    fun updateTemplate(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTemplateRequest,
    ): ApiResponse<TemplateResponse> {
        return ApiResponse.ok(templateService.update(id, request))
    }
}
