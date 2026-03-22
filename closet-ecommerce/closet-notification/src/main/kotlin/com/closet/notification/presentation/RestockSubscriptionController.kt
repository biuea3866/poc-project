package com.closet.notification.presentation

import com.closet.common.response.ApiResponse
import com.closet.notification.application.RestockSubscriptionService
import com.closet.notification.presentation.dto.RestockSubscribeRequest
import com.closet.notification.presentation.dto.RestockSubscriptionResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications/restock-subscriptions")
class RestockSubscriptionController(
    private val restockSubscriptionService: RestockSubscriptionService,
) {

    /** 재입고 알림 구독 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun subscribe(
        @RequestHeader("X-Member-Id") memberId: Long,
        @Valid @RequestBody request: RestockSubscribeRequest,
    ): ApiResponse<RestockSubscriptionResponse> {
        return ApiResponse.created(
            restockSubscriptionService.subscribe(memberId, request.productOptionId)
        )
    }

    /** 재입고 알림 구독 취소 */
    @DeleteMapping("/{productOptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unsubscribe(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable productOptionId: Long,
    ) {
        restockSubscriptionService.unsubscribe(memberId, productOptionId)
    }

    /** 내 재입고 구독 목록 */
    @GetMapping("/my")
    fun getMySubscriptions(
        @RequestHeader("X-Member-Id") memberId: Long,
    ): ApiResponse<List<RestockSubscriptionResponse>> {
        return ApiResponse.ok(restockSubscriptionService.findByMember(memberId))
    }
}
