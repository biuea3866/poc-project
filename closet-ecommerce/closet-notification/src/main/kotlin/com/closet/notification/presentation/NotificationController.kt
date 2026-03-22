package com.closet.notification.presentation

import com.closet.common.response.ApiResponse
import com.closet.notification.application.NotificationService
import com.closet.notification.presentation.dto.NotificationResponse
import com.closet.notification.presentation.dto.SendNotificationRequest
import com.closet.notification.presentation.dto.UnreadCountResponse
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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {

    /** 회원별 알림 목록 조회 */
    @GetMapping
    fun getNotifications(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PageableDefault(size = 10) pageable: Pageable,
    ): ApiResponse<Page<NotificationResponse>> {
        return ApiResponse.ok(notificationService.findByMember(memberId, pageable))
    }

    /** 미읽음 카운트 조회 */
    @GetMapping("/unread-count")
    fun getUnreadCount(
        @RequestHeader("X-Member-Id") memberId: Long,
    ): ApiResponse<UnreadCountResponse> {
        return ApiResponse.ok(notificationService.getUnreadCount(memberId))
    }

    /** 알림 읽음 처리 */
    @PatchMapping("/{id}/read")
    fun markAsRead(@PathVariable id: Long): ApiResponse<NotificationResponse> {
        return ApiResponse.ok(notificationService.markAsRead(id))
    }

    /** 알림 발송 (관리자용) */
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    fun send(@Valid @RequestBody request: SendNotificationRequest): ApiResponse<NotificationResponse> {
        return ApiResponse.created(
            notificationService.send(
                memberId = request.memberId,
                type = request.type,
                channel = request.channel,
                title = request.title,
                content = request.content,
            )
        )
    }
}
