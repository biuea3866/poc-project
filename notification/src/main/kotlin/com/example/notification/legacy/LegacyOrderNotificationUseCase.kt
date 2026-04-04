// 패턴: 레거시 주문 알림 UseCase (채널별 이벤트 직접 발행)
package com.example.notification.legacy

import org.springframework.stereotype.Component

/**
 * 레거시 주문 알림 UseCase.
 * 문제:
 * - 3개 이벤트를 각각 발행 -> 채널 추가 시 이 메서드도 수정 필요
 * - 설정 체크 로직이 각 핸들러에 분산
 * - 수신자 결정 로직 중복
 */
@Component
class LegacyOrderNotificationUseCase(
    private val emailHandler: LegacyEmailHandler,
    private val pushHandler: LegacyPushHandler,
    private val smsHandler: LegacySmsHandler,
    private val inAppHandler: LegacyInAppHandler,
) {
    data class Order(
        val id: Long,
        val storeId: Long,
        val buyerName: String,
        val storeName: String,
        val amount: Long,
        val buyerPhone: String,
    )

    fun notifyOrderPlaced(order: Order) {
        // 문제: 채널 추가 시 여기에 한 줄씩 추가해야 함
        emailHandler.onOrderPlaced(
            OrderPlacedEmailEvent(order.id, order.buyerName, order.storeName, order.amount)
        )
        pushHandler.onOrderPlaced(
            OrderPlacedPushEvent(order.id, order.buyerName, order.storeName)
        )
        smsHandler.onOrderPlaced(
            OrderPlacedSmsEvent(order.id, order.buyerName, order.buyerPhone)
        )
        inAppHandler.onOrderPlaced(
            OrderPlacedInAppEvent(order.id, order.buyerName, order.storeName, mapOf("amount" to order.amount))
        )
    }

    fun notifyShipmentStarted(order: Order, trackingNumber: String) {
        emailHandler.onShipmentStarted(
            ShipmentStartedEmailEvent(order.id, order.buyerName, trackingNumber)
        )
        pushHandler.onShipmentStarted(
            ShipmentStartedPushEvent(order.id, order.buyerName, trackingNumber)
        )
        smsHandler.onShipmentStarted(
            ShipmentStartedSmsEvent(order.id, order.buyerName, order.buyerPhone, trackingNumber)
        )
        inAppHandler.onShipmentStarted(
            ShipmentStartedInAppEvent(order.id, trackingNumber)
        )
    }

    fun notifyPaymentCompleted(order: Order) {
        emailHandler.onPaymentCompleted(
            PaymentCompletedEmailEvent(order.id, order.buyerName, order.amount)
        )
        pushHandler.onPaymentCompleted(
            PaymentCompletedPushEvent(order.id, order.buyerName, order.amount)
        )
        smsHandler.onPaymentCompleted(
            PaymentCompletedSmsEvent(order.id, order.buyerName, order.buyerPhone, order.amount)
        )
        inAppHandler.onPaymentCompleted(
            PaymentCompletedInAppEvent(order.id, order.amount, order.storeName)
        )
    }
}
