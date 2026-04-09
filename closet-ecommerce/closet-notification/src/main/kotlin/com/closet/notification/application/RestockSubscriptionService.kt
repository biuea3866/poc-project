package com.closet.notification.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import com.closet.notification.domain.RestockSubscription
import com.closet.notification.domain.repository.NotificationRepository
import com.closet.notification.domain.repository.RestockSubscriptionRepository
import com.closet.notification.presentation.dto.RestockSubscriptionResponse
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class RestockSubscriptionService(
    private val restockSubscriptionRepository: RestockSubscriptionRepository,
    private val notificationRepository: NotificationRepository,
) {
    /** 재입고 알림 구독 */
    @Transactional
    fun subscribe(
        memberId: Long,
        productOptionId: Long,
    ): RestockSubscriptionResponse {
        if (restockSubscriptionRepository.existsByMemberIdAndProductOptionIdAndDeletedAtIsNull(memberId, productOptionId)) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 재입고 알림을 구독 중입니다")
        }

        val subscription = RestockSubscription.create(memberId, productOptionId)
        val saved = restockSubscriptionRepository.save(subscription)
        return RestockSubscriptionResponse.from(saved)
    }

    /** 재입고 알림 구독 취소 */
    @Transactional
    fun unsubscribe(
        memberId: Long,
        productOptionId: Long,
    ) {
        val subscription =
            restockSubscriptionRepository.findByMemberIdAndProductOptionIdAndDeletedAtIsNull(memberId, productOptionId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "재입고 구독을 찾을 수 없습니다")

        subscription.softDelete()
    }

    /** 재입고 알림 발송 (상품 옵션 재입고 시 호출) */
    @Transactional
    fun notifyRestock(productOptionId: Long): Int {
        val subscriptions = restockSubscriptionRepository.findByProductOptionIdAndIsNotifiedFalseAndDeletedAtIsNull(productOptionId)

        subscriptions.forEach { subscription ->
            subscription.markNotified()

            val notification =
                Notification.create(
                    memberId = subscription.memberId,
                    channel = NotificationChannel.PUSH,
                    type = NotificationType.RESTOCK,
                    title = "재입고 알림",
                    content = "구독하신 상품이 재입고되었습니다. 지금 확인해보세요!",
                )
            notificationRepository.save(notification)

            logger.info { "재입고 알림 발송: memberId=${subscription.memberId}, productOptionId=$productOptionId" }
        }

        return subscriptions.size
    }

    /** 내 재입고 구독 목록 조회 */
    fun findByMember(memberId: Long): List<RestockSubscriptionResponse> {
        return restockSubscriptionRepository.findByMemberIdAndDeletedAtIsNull(memberId).map { RestockSubscriptionResponse.from(it) }
    }
}
