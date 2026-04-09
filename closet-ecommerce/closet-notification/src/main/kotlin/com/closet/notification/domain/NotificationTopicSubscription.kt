package com.closet.notification.domain

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

/**
 * 상품/카테고리/브랜드/이벤트 단위 알림 구독.
 *
 * 회원이 특정 토픽(상품, 카테고리, 브랜드, 이벤트)에 대해
 * 알림을 수신하겠다고 구독하는 엔티티이다.
 */
@Entity
@Table(name = "notification_topic_subscription")
class NotificationTopicSubscription(
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "topic_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val topicType: TopicType,
    @Column(name = "topic_id", nullable = false)
    val topicId: Long,
    @Column(name = "is_subscribed", nullable = false)
    var isSubscribed: Boolean = true,
    @Column(name = "subscribed_at", nullable = false, columnDefinition = "DATETIME(6)")
    val subscribedAt: ZonedDateTime = ZonedDateTime.now(),
    @Column(name = "unsubscribed_at", columnDefinition = "DATETIME(6)")
    var unsubscribedAt: ZonedDateTime? = null,
) : BaseEntity() {
    companion object {
        fun create(
            memberId: Long,
            topicType: TopicType,
            topicId: Long,
        ): NotificationTopicSubscription {
            return NotificationTopicSubscription(
                memberId = memberId,
                topicType = topicType,
                topicId = topicId,
                isSubscribed = true,
                subscribedAt = ZonedDateTime.now(),
            )
        }
    }

    /** 구독 해제 */
    fun unsubscribe() {
        this.isSubscribed = false
        this.unsubscribedAt = ZonedDateTime.now()
    }

    /** 구독 재활성화 */
    fun resubscribe() {
        this.isSubscribed = true
        this.unsubscribedAt = null
    }
}
