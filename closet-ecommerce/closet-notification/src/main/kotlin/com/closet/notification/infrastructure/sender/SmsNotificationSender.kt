package com.closet.notification.infrastructure.sender

import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.sender.NotificationSender
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * SMS 채널 알림 발송 구현체.
 *
 * 현재는 로그로 발송을 기록하며, 향후 SMS 게이트웨이(NHN Cloud 등) 연동으로 교체한다.
 */
@Component
class SmsNotificationSender : NotificationSender {
    override fun supports(channel: NotificationChannel): Boolean {
        return channel == NotificationChannel.SMS
    }

    override fun send(notification: Notification): Boolean {
        logger.info {
            "[SMS] 알림 발송: memberId=${notification.memberId}, " +
                "title=${notification.title}, content=${notification.content}"
        }
        return true
    }
}
