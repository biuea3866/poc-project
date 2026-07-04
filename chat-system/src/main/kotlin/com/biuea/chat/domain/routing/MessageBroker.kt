package com.biuea.chat.domain.routing

/**
 * 서버 간 메시지 전파 통로 (Redis Pub/Sub 등). 수신자가 붙은 서버로만 지향 전달한다.
 */
interface MessageBroker {
    fun forward(serverId: String, envelope: DeliveryEnvelope)
}
