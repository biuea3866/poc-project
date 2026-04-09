package com.closet.notification.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationPreference
import com.closet.notification.domain.NotificationTopicSubscription
import com.closet.notification.domain.TopicType
import com.closet.notification.domain.repository.NotificationPreferenceRepository
import com.closet.notification.domain.repository.NotificationTopicSubscriptionRepository
import com.closet.notification.presentation.dto.NotificationPreferenceResponse
import com.closet.notification.presentation.dto.TopicSubscriptionResponse
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class NotificationPreferenceService(
    private val preferenceRepository: NotificationPreferenceRepository,
    private val topicSubscriptionRepository: NotificationTopicSubscriptionRepository,
) {
    /** 회원의 알림 수신 설정을 조회하거나 기본값으로 생성한다 */
    @Transactional
    fun getOrCreatePreference(memberId: Long): NotificationPreferenceResponse {
        val preference = findOrCreatePreference(memberId)
        return NotificationPreferenceResponse.from(preference)
    }

    /** 알림 수신 설정 업데이트 */
    @Transactional
    fun updatePreference(
        memberId: Long,
        emailEnabled: Boolean? = null,
        smsEnabled: Boolean? = null,
        pushEnabled: Boolean? = null,
        marketingEnabled: Boolean? = null,
        nightEnabled: Boolean? = null,
    ): NotificationPreferenceResponse {
        val preference = findOrCreatePreference(memberId)

        preference.updateChannelSetting(
            emailEnabled = emailEnabled,
            smsEnabled = smsEnabled,
            pushEnabled = pushEnabled,
            marketingEnabled = marketingEnabled,
            nightEnabled = nightEnabled,
        )

        logger.info { "알림 설정 업데이트: memberId=$memberId" }
        return NotificationPreferenceResponse.from(preference)
    }

    /** 특정 채널이 활성화되어 있는지 확인 */
    fun isChannelEnabled(
        memberId: Long,
        channel: NotificationChannel,
    ): Boolean {
        val preference = findOrCreatePreference(memberId)
        return preference.isChannelEnabled(channel)
    }

    /** DND(방해금지) 시간인지 확인 */
    fun isDndTime(
        memberId: Long,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): Boolean {
        val preference = findOrCreatePreference(memberId)
        return preference.isDndTime(now)
    }

    /** 토픽 구독. 이미 활성 구독이 있으면 예외, 비활성이면 재활성화 */
    @Transactional
    fun subscribe(
        memberId: Long,
        topicType: TopicType,
        topicId: Long,
    ): TopicSubscriptionResponse {
        val existing =
            topicSubscriptionRepository.findByMemberIdAndTopicTypeAndTopicIdAndDeletedAtIsNull(
                memberId,
                topicType,
                topicId,
            )

        if (existing != null) {
            if (existing.isSubscribed) {
                throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 해당 토픽을 구독 중입니다")
            }

            existing.resubscribe()
            logger.info { "토픽 구독 재활성화: memberId=$memberId, topicType=$topicType, topicId=$topicId" }
            return TopicSubscriptionResponse.from(existing)
        }

        val subscription = NotificationTopicSubscription.create(memberId, topicType, topicId)
        val saved = topicSubscriptionRepository.save(subscription)
        logger.info { "토픽 구독 생성: memberId=$memberId, topicType=$topicType, topicId=$topicId" }
        return TopicSubscriptionResponse.from(saved)
    }

    /** 토픽 구독 해제 */
    @Transactional
    fun unsubscribe(
        memberId: Long,
        topicType: TopicType,
        topicId: Long,
    ) {
        val subscription =
            topicSubscriptionRepository.findByMemberIdAndTopicTypeAndTopicIdAndDeletedAtIsNull(
                memberId,
                topicType,
                topicId,
            ) ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "해당 토픽 구독을 찾을 수 없습니다")

        subscription.unsubscribe()
        logger.info { "토픽 구독 해제: memberId=$memberId, topicType=$topicType, topicId=$topicId" }
    }

    /** 회원의 토픽 구독 목록 조회 */
    fun getSubscriptions(memberId: Long): List<TopicSubscriptionResponse> {
        return topicSubscriptionRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .map { TopicSubscriptionResponse.from(it) }
    }

    private fun findOrCreatePreference(memberId: Long): NotificationPreference {
        return preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            ?: run {
                val defaultPreference = NotificationPreference.createDefault(memberId)
                preferenceRepository.save(defaultPreference)
            }
    }
}
