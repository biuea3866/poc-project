package com.closet.notification.application

import com.closet.notification.domain.Notification
import com.closet.notification.domain.sender.NotificationSender
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

/**
 * 알림 채널 디스패처 (Strategy 패턴).
 *
 * 등록된 NotificationSender 구현체 목록에서 channel에 맞는 Sender를 찾아 발송한다.
 * 발송 전 회원의 알림 수신 설정(채널 활성화, DND)을 확인하여
 * 비활성화된 채널이거나 DND 시간이면 발송을 차단한다.
 */
@Component
class NotificationDispatcher(
    private val senders: List<NotificationSender>,
    private val notificationPreferenceService: NotificationPreferenceService,
) {
    /**
     * 알림을 적합한 채널 Sender로 디스패치한다.
     *
     * 1. 회원의 해당 채널 수신 설정 확인 (비활성이면 BLOCKED)
     * 2. DND(방해금지) 시간 확인 (야간 알림 비동의 + 21:00~08:00이면 BLOCKED)
     * 3. 적합한 Sender를 찾아 발송
     *
     * @return 발송 성공 여부 (차단 시 false)
     * @throws IllegalStateException 지원하는 Sender가 없는 경우
     */
    fun dispatch(notification: Notification): Boolean {
        // 채널 수신 설정 확인
        if (!notificationPreferenceService.isChannelEnabled(notification.memberId, notification.channel)) {
            logger.info {
                "알림 차단 (채널 비활성): channel=${notification.channel}, " +
                    "memberId=${notification.memberId}"
            }
            return false
        }

        // DND 시간 확인
        if (notificationPreferenceService.isDndTime(notification.memberId, ZonedDateTime.now())) {
            logger.info {
                "알림 차단 (DND 시간): channel=${notification.channel}, " +
                    "memberId=${notification.memberId}"
            }
            return false
        }

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
