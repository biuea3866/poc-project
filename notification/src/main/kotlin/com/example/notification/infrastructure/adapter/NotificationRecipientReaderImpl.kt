package com.example.notification.infrastructure.adapter

import com.example.notification.application.port.NotificationRecipientReader
import com.example.notification.domain.model.NotificationRecipient
import org.springframework.stereotype.Repository

/**
 * 수신자 조회 구현체.
 * 실제 운영에서는 UserRepository, OrderRepository 등을 주입받아 조회.
 * 현재는 스텁 구현 (TODO: 실제 연동 시 교체).
 */
@Repository
class NotificationRecipientReaderImpl : NotificationRecipientReader {

    override fun findByOrderId(orderId: Long): List<NotificationRecipient> {
        // TODO: orderRepository.findById(orderId) -> seller/buyer 조회
        println("[NotificationRecipientReaderImpl] findByOrderId called with orderId=$orderId (stub)")
        return emptyList()
    }

    override fun findByStoreId(storeId: Long): List<NotificationRecipient> {
        // TODO: storeRepository.findById(storeId) -> owner/staff 조회
        println("[NotificationRecipientReaderImpl] findByStoreId called with storeId=$storeId (stub)")
        return emptyList()
    }
}
