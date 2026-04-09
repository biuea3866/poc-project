package com.closet.notification.application

import com.closet.notification.domain.Notification
import com.closet.notification.domain.sender.NotificationSender
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 알림 채널 디스패처 (Strategy 패턴).
 *
 * 등록된 NotificationSender 구현체 목록에서 channel에 맞는 Sender를 찾아 발송한다.
 * CarrierAdapterFactory 패턴과 동일한 구조.
 */
@Component
class NotificationDispatcher(
    private val senders: List<NotificationSender>,
) {
    /**
     * 알림을 적합한 채널 Sender로 디스패치한다.
     *
     * @return 발송 성공 여부
     * @throws IllegalStateException 지원하는 Sender가 없는 경우
     */
    fun dispatch(notification: Notification): Boolean {
        val sender =
            senders.find { it.supports(notification.channel) }
                ?: throw IllegalStateException(
                    "지원하지 않는 알림 채널입니다: ${notification.channel}",
                )

        return try {
            val result = sender.send(notification)
            logger.info {
                "알림 디스패치 완료: channel=${notification.channel}, " +
                    "memberId=${notification.memberId}, success=$result"
            }
            result
        } catch (e: Exception) {
            logger.error(e) {
                "알림 디스패치 실패: channel=${notification.channel}, " +
                    "memberId=${notification.memberId}"
            }
            false
        }
    }
}
