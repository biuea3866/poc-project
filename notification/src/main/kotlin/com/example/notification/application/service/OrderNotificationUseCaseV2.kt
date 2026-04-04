// 패턴: 통합 알림 시스템의 주문 알림 UseCase V2
package com.example.notification.application.service

import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.NotificationPayload
import com.example.notification.legacy.LegacyOrderNotificationUseCase

import org.springframework.stereotype.Service

/**
 * 주문 알림 V2.
 * Feature Flag ON -> NotificationEvent 1건 발행 (새 시스템)
 * Feature Flag OFF -> 레거시 3개 이벤트 발행 (기존 시스템)
 *
 * Feature Flag 기반 V2/레거시 분기 패턴.
 */
@Service
class OrderNotificationUseCaseV2(
    private val bridge: NotificationLegacyBridge,
    private val legacyUseCase: LegacyOrderNotificationUseCase,
) {
    fun notifyOrderPlaced(order: LegacyOrderNotificationUseCase.Order) {
        val payload = NotificationPayload.OrderPlaced(
            orderId = order.id,
            buyerName = order.buyerName,
            storeName = order.storeName,
            amount = order.amount,
            itemCount = 1,
        )

        if (bridge.tryPublishV2(
                triggerType = NotificationTriggerType.ORDER_PLACED,
                storeId = order.storeId,
                orderId = order.id,
                payload = payload,
            )
        ) return

        // 레거시 폴백
        legacyUseCase.notifyOrderPlaced(order)
    }

    fun notifyShipmentStarted(order: LegacyOrderNotificationUseCase.Order, trackingNumber: String, carrierName: String) {
        val payload = NotificationPayload.ShipmentStarted(
            orderId = order.id,
            buyerName = order.buyerName,
            trackingNumber = trackingNumber,
            carrierName = carrierName,
        )

        if (bridge.tryPublishV2(
                triggerType = NotificationTriggerType.SHIPMENT_STARTED,
                storeId = order.storeId,
                orderId = order.id,
                payload = payload,
            )
        ) return

        // 레거시 폴백
        legacyUseCase.notifyShipmentStarted(order, trackingNumber)
    }

    fun notifyPaymentCompleted(order: LegacyOrderNotificationUseCase.Order, paymentMethod: String) {
        val payload = NotificationPayload.PaymentCompleted(
            orderId = order.id,
            buyerName = order.buyerName,
            amount = order.amount,
            paymentMethod = paymentMethod,
        )

        if (bridge.tryPublishV2(
                triggerType = NotificationTriggerType.PAYMENT_COMPLETED,
                storeId = order.storeId,
                orderId = order.id,
                payload = payload,
            )
        ) return

        // 레거시 폴백
        legacyUseCase.notifyPaymentCompleted(order)
    }
}
