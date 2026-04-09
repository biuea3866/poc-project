package com.closet.notification.presentation

import com.closet.common.response.ApiResponse
import com.closet.notification.application.NotificationPreferenceService
import com.closet.notification.presentation.dto.NotificationPreferenceResponse
import com.closet.notification.presentation.dto.TopicSubscribeRequest
import com.closet.notification.presentation.dto.TopicSubscriptionResponse
import com.closet.notification.presentation.dto.TopicUnsubscribeRequest
import com.closet.notification.presentation.dto.UpdatePreferenceRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationPreferenceController(
    private val notificationPreferenceService: NotificationPreferenceService,
) {
    /** 내 알림 수신 설정 조회 */
    @GetMapping("/preferences/me")
    fun getMyPreference(
        @RequestHeader("X-Member-Id") memberId: Long,
    ): ApiResponse<NotificationPreferenceResponse> {
        return ApiResponse.ok(notificationPreferenceService.getOrCreatePreference(memberId))
    }

    /** 내 알림 수신 설정 수정 */
    @PutMapping("/preferences/me")
    fun updateMyPreference(
        @RequestHeader("X-Member-Id") memberId: Long,
        @Valid @RequestBody request: UpdatePreferenceRequest,
    ): ApiResponse<NotificationPreferenceResponse> {
        return ApiResponse.ok(
            notificationPreferenceService.updatePreference(
                memberId = memberId,
                emailEnabled = request.emailEnabled,
                smsEnabled = request.smsEnabled,
                pushEnabled = request.pushEnabled,
                marketingEnabled = request.marketingEnabled,
                nightEnabled = request.nightEnabled,
            ),
        )
    }

    /** 토픽 구독 */
    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    fun subscribe(
        @RequestHeader("X-Member-Id") memberId: Long,
        @Valid @RequestBody request: TopicSubscribeRequest,
    ): ApiResponse<TopicSubscriptionResponse> {
        return ApiResponse.created(
            notificationPreferenceService.subscribe(
                memberId = memberId,
                topicType = request.topicType,
                topicId = request.topicId,
            ),
        )
    }

    /** 토픽 구독 해제 */
    @DeleteMapping("/subscriptions")
    fun unsubscribe(
        @RequestHeader("X-Member-Id") memberId: Long,
        @Valid @RequestBody request: TopicUnsubscribeRequest,
    ): ApiResponse<Unit> {
        notificationPreferenceService.unsubscribe(
            memberId = memberId,
            topicType = request.topicType,
            topicId = request.topicId,
        )
        return ApiResponse.ok(Unit)
    }

    /** 내 토픽 구독 목록 조회 */
    @GetMapping("/subscriptions/me")
    fun getMySubscriptions(
        @RequestHeader("X-Member-Id") memberId: Long,
    ): ApiResponse<List<TopicSubscriptionResponse>> {
        return ApiResponse.ok(notificationPreferenceService.getSubscriptions(memberId))
    }
}
