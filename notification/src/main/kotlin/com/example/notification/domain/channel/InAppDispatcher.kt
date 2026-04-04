// 패턴: 인앱 알림 -- DB 저장 후 WebSocket 서버가 프론트에 실시간 push
// 실제 구현: alertRepository.save(alert) -> eventPublisher.publish(AlertAddedEvent) -> WebSocket 서버
package com.example.notification.domain.channel

import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationRecipient
import com.example.notification.domain.model.RenderedMessage

import org.springframework.stereotype.Component

@Component
class InAppDispatcher : NotificationChannelDispatcher {

    override fun supports(channel: NotificationChannel): Boolean =
        channel == NotificationChannel.IN_APP

    override fun dispatch(message: RenderedMessage, recipient: NotificationRecipient, event: NotificationEvent) {
        // DB 저장 (alerts 테이블)
        println("[IN_APP] DB 저장: userId=${recipient.userId}, title=${message.pushTitle}, data=${message.deepLinkUrl}")
        // WebSocket 서버로 이벤트 전파 -> 프론트에 실시간 push
        println("[IN_APP] WebSocket 이벤트 발행: AlertAddedEvent(userId=${recipient.userId})")
    }
}
