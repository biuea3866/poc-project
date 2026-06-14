package com.biuea.batch.domain

/**
 * 알림 발송 외부 시스템 추상화(Gateway). 구현체는 infrastructure 에 위치한다.
 * 멀티스레드 전략에서 동시 호출되므로 구현체는 stateless 여야 한다.
 */
interface NotificationSendGateway {
    fun send(message: NotificationMessage)
}
