package com.closet.notification.domain.repository

import com.closet.notification.domain.NotificationTopicSubscription
import com.closet.notification.domain.TopicType

/**
 * NotificationTopicSubscription QueryDSL 커스텀 레포지토리 인터페이스
 */
interface NotificationTopicSubscriptionRepositoryCustom {
    /** 회원의 특정 토픽 유형 구독 목록 조회 (활성 구독만) */
    fun findActiveByMemberIdAndTopicType(
        memberId: Long,
        topicType: TopicType,
    ): List<NotificationTopicSubscription>

    /** 특정 토픽에 대한 활성 구독 회원 ID 목록 조회 */
    fun findSubscribedMemberIdsByTopic(
        topicType: TopicType,
        topicId: Long,
    ): List<Long>
}
