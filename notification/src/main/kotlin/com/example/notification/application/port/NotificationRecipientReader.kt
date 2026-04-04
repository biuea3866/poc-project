// 패턴: 통합 알림 시스템의 수신자 조회 포트
package com.example.notification.application.port

import com.example.notification.domain.model.NotificationRecipient

interface NotificationRecipientReader {
    fun findByOrderId(orderId: Long): List<NotificationRecipient>
    fun findByStoreId(storeId: Long): List<NotificationRecipient>
}
