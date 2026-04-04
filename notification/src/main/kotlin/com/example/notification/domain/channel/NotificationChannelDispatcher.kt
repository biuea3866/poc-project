// 패턴: 통합 알림 시스템의 채널별 발송 전략
package com.example.notification.domain.channel

import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationRecipient
import com.example.notification.domain.model.RenderedMessage

interface NotificationChannelDispatcher {
    fun supports(channel: NotificationChannel): Boolean
    fun dispatch(message: RenderedMessage, recipient: NotificationRecipient, event: NotificationEvent)
}
