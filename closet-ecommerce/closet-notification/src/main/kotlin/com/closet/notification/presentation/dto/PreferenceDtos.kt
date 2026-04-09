package com.closet.notification.presentation.dto

import com.closet.notification.domain.NotificationPreference
import com.closet.notification.domain.NotificationTopicSubscription
import com.closet.notification.domain.TopicType
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime

/** 알림 수신 설정 업데이트 요청 */
data class UpdatePreferenceRequest(
    val emailEnabled: Boolean? = null,
    val smsEnabled: Boolean? = null,
    val pushEnabled: Boolean? = null,
    val marketingEnabled: Boolean? = null,
    val nightEnabled: Boolean? = null,
)

/** 알림 수신 설정 응답 */
data class NotificationPreferenceResponse(
    val id: Long,
    val memberId: Long,
    val emailEnabled: Boolean,
    val smsEnabled: Boolean,
    val pushEnabled: Boolean,
    val marketingEnabled: Boolean,
    val nightEnabled: Boolean,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(preference: NotificationPreference): NotificationPreferenceResponse =
            NotificationPreferenceResponse(
                id = preference.id,
                memberId = preference.memberId,
                emailEnabled = preference.emailEnabled,
                smsEnabled = preference.smsEnabled,
                pushEnabled = preference.pushEnabled,
                marketingEnabled = preference.marketingEnabled,
                nightEnabled = preference.nightEnabled,
                createdAt = preference.createdAt,
                updatedAt = preference.updatedAt,
            )
    }
}

/** 토픽 구독 요청 */
data class TopicSubscribeRequest(
    @field:NotNull(message = "토픽 유형은 필수입니다")
    val topicType: TopicType,
    @field:NotNull(message = "토픽 ID는 필수입니다")
    val topicId: Long,
)

/** 토픽 구독 해제 요청 */
data class TopicUnsubscribeRequest(
    @field:NotNull(message = "토픽 유형은 필수입니다")
    val topicType: TopicType,
    @field:NotNull(message = "토픽 ID는 필수입니다")
    val topicId: Long,
)

/** 토픽 구독 응답 */
data class TopicSubscriptionResponse(
    val id: Long,
    val memberId: Long,
    val topicType: TopicType,
    val topicId: Long,
    val isSubscribed: Boolean,
    val subscribedAt: ZonedDateTime,
    val unsubscribedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(subscription: NotificationTopicSubscription): TopicSubscriptionResponse =
            TopicSubscriptionResponse(
                id = subscription.id,
                memberId = subscription.memberId,
                topicType = subscription.topicType,
                topicId = subscription.topicId,
                isSubscribed = subscription.isSubscribed,
                subscribedAt = subscription.subscribedAt,
                unsubscribedAt = subscription.unsubscribedAt,
                createdAt = subscription.createdAt,
            )
    }
}
