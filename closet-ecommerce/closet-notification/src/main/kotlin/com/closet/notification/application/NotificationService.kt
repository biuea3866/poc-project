package com.closet.notification.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import com.closet.notification.domain.repository.NotificationRepository
import com.closet.notification.presentation.dto.NotificationResponse
import com.closet.notification.presentation.dto.UnreadCountResponse
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationDispatcher: NotificationDispatcher,
) {
    /** 알림 발송: DB 저장 후 채널별 Sender로 디스패치 */
    @Transactional
    fun send(
        memberId: Long,
        type: NotificationType,
        channel: NotificationChannel,
        title: String,
        content: String,
    ): NotificationResponse {
        val notification =
            Notification.create(
                memberId = memberId,
                channel = channel,
                type = type,
                title = title,
                content = content,
            )

        val saved = notificationRepository.save(notification)

        // 채널별 Strategy 디스패치 (실패해도 알림 저장은 유지, 재시도는 별도 처리)
        try {
            notificationDispatcher.dispatch(saved)
        } catch (e: Exception) {
            logger.error(e) { "알림 디스패치 실패: notificationId=${saved.id}, channel=$channel" }
        }

        return NotificationResponse.from(saved)
    }

    /** 회원별 알림 목록 조회 (페이징) */
    fun findByMember(
        memberId: Long,
        pageable: Pageable,
    ): Page<NotificationResponse> {
        return notificationRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            memberId,
            pageable,
        ).map { NotificationResponse.from(it) }
    }

    /** 읽음 처리 */
    @Transactional
    fun markAsRead(notificationId: Long): NotificationResponse {
        val notification =
            notificationRepository.findByIdAndDeletedAtIsNull(notificationId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "알림을 찾을 수 없습니다")

        notification.markAsRead()
        return NotificationResponse.from(notification)
    }

    /** 미읽음 카운트 조회 */
    fun getUnreadCount(memberId: Long): UnreadCountResponse {
        val count = notificationRepository.countByMemberIdAndIsReadFalseAndDeletedAtIsNull(memberId)
        return UnreadCountResponse(count = count)
    }
}
