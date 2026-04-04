// 패턴: 푸시 채널 발송 전략
package com.example.notification.domain.channel

import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationRecipient
import com.example.notification.domain.model.RenderedMessage

import org.springframework.stereotype.Component

@Component
class PushDispatcher : NotificationChannelDispatcher {

    override fun supports(channel: NotificationChannel): Boolean =
        channel == NotificationChannel.PUSH

    override fun dispatch(message: RenderedMessage, recipient: NotificationRecipient, event: NotificationEvent) {
        println("[PUSH] Token: ${recipient.pushToken}, Title: ${message.pushTitle}, Body: ${message.pushBody}")
    }
}
