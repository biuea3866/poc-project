package com.biuea.batch.domain

import org.springframework.stereotype.Service

/**
 * 발송 도메인 서비스. 묶음/단건 메시지 변환은 NotificationGroup 에 캡슐화돼 있고,
 * 여기서는 변환된 메시지를 Gateway 로 발송하는 행위만 오케스트레이션한다.
 *
 * Repository 영속화(sent 갱신)는 chunk 경계에서 Writer 가 수행하므로 여기서 다루지 않는다(ADR-0001).
 */
@Service
class NotificationDispatchService(
    private val gateway: NotificationSendGateway,
) {
    fun dispatch(group: NotificationGroup) {
        gateway.send(group.toMessage())
    }
}
