package com.closet.notification.presentation.dto

import com.closet.notification.domain.RestockSubscription
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime

/** 재입고 구독 요청 */
data class RestockSubscribeRequest(
    @field:NotNull(message = "상품 옵션 ID는 필수입니다")
    val productOptionId: Long,
)

/** 재입고 구독 응답 */
data class RestockSubscriptionResponse(
    val id: Long,
    val memberId: Long,
    val productOptionId: Long,
    val isNotified: Boolean,
    val subscribedAt: ZonedDateTime,
    val notifiedAt: ZonedDateTime?,
) {
    companion object {
        fun from(subscription: RestockSubscription): RestockSubscriptionResponse =
            RestockSubscriptionResponse(
                id = subscription.id,
                memberId = subscription.memberId,
                productOptionId = subscription.productOptionId,
                isNotified = subscription.isNotified,
                subscribedAt = subscription.subscribedAt,
                notifiedAt = subscription.notifiedAt,
            )
    }
}
