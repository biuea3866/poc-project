package com.closet.notification.domain.repository

import com.closet.notification.domain.NotificationTopicSubscription
import com.closet.notification.domain.TopicType
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationTopicSubscriptionRepository :
    JpaRepository<NotificationTopicSubscription, Long>,
    NotificationTopicSubscriptionRepositoryCustom {
    fun findByMemberIdAndDeletedAtIsNull(memberId: Long): List<NotificationTopicSubscription>

    fun findByMemberIdAndTopicTypeAndTopicIdAndDeletedAtIsNull(
        memberId: Long,
        topicType: TopicType,
        topicId: Long,
    ): NotificationTopicSubscription?

    fun existsByMemberIdAndTopicTypeAndTopicIdAndIsSubscribedTrueAndDeletedAtIsNull(
        memberId: Long,
        topicType: TopicType,
        topicId: Long,
    ): Boolean
}
