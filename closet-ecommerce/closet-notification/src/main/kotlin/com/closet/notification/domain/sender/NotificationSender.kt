package com.closet.notification.domain.sender

import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel

/**
 * 알림 발송 Strategy 인터페이스.
 *
 * 각 채널(EMAIL, SMS, PUSH)별 구현체가 이 인터페이스를 구현한다.
 * NotificationDispatcher가 channel에 맞는 구현체를 선택하여 발송한다.
 */
interface NotificationSender {
    /** 해당 채널을 지원하는지 여부 */
    fun supports(channel: NotificationChannel): Boolean

    /** 알림 발송 (성공: true, 실패: false) */
    fun send(notification: Notification): Boolean
}
