package com.closet.notification.infrastructure.repository

import com.closet.notification.domain.NotificationTopicSubscription
import com.closet.notification.domain.QNotificationTopicSubscription.notificationTopicSubscription
import com.closet.notification.domain.TopicType
import com.closet.notification.domain.repository.NotificationTopicSubscriptionRepositoryCustom
import com.querydsl.jpa.impl.JPAQueryFactory

class NotificationTopicSubscriptionRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : NotificationTopicSubscriptionRepositoryCustom {
    override fun findActiveByMemberIdAndTopicType(
        memberId: Long,
        topicType: TopicType,
    ): List<NotificationTopicSubscription> {
        return queryFactory.selectFrom(notificationTopicSubscription)
            .where(
                notificationTopicSubscription.memberId.eq(memberId),
                notificationTopicSubscription.topicType.eq(topicType),
                notificationTopicSubscription.isSubscribed.isTrue,
                notificationTopicSubscription.deletedAt.isNull,
            )
            .orderBy(notificationTopicSubscription.subscribedAt.desc())
            .fetch()
    }

    override fun findSubscribedMemberIdsByTopic(
        topicType: TopicType,
        topicId: Long,
    ): List<Long> {
        return queryFactory.select(notificationTopicSubscription.memberId)
            .from(notificationTopicSubscription)
            .where(
                notificationTopicSubscription.topicType.eq(topicType),
                notificationTopicSubscription.topicId.eq(topicId),
                notificationTopicSubscription.isSubscribed.isTrue,
                notificationTopicSubscription.deletedAt.isNull,
            )
            .fetch()
    }
}
