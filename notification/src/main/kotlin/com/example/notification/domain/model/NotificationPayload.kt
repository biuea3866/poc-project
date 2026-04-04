// 패턴: 통합 알림 시스템의 페이로드 모델
package com.example.notification.domain.model

/**
 * 알림 페이로드. sealed interface로 트리거별 데이터 타입 안전성 보장.
 */
sealed interface NotificationPayload {

    fun toVariables(): Map<String, Any?>

    data class OrderPlaced(
        val orderId: Long,
        val buyerName: String,
        val storeName: String,
        val amount: Long,
        val itemCount: Int,
    ) : NotificationPayload {
        override fun toVariables(): Map<String, Any?> = mapOf(
            "orderId" to orderId,
            "buyerName" to buyerName,
            "storeName" to storeName,
            "amount" to amount,
            "itemCount" to itemCount,
        )
    }

    data class ShipmentStarted(
        val orderId: Long,
        val buyerName: String,
        val trackingNumber: String,
        val carrierName: String,
    ) : NotificationPayload {
        override fun toVariables(): Map<String, Any?> = mapOf(
            "orderId" to orderId,
            "buyerName" to buyerName,
            "trackingNumber" to trackingNumber,
            "carrierName" to carrierName,
        )
    }

    data class ShipmentDelivered(
        val orderId: Long,
        val buyerName: String,
        val deliveredAt: String,
    ) : NotificationPayload {
        override fun toVariables(): Map<String, Any?> = mapOf(
            "orderId" to orderId,
            "buyerName" to buyerName,
            "deliveredAt" to deliveredAt,
        )
    }

    data class PaymentCompleted(
        val orderId: Long,
        val buyerName: String,
        val amount: Long,
        val paymentMethod: String,
    ) : NotificationPayload {
        override fun toVariables(): Map<String, Any?> = mapOf(
            "orderId" to orderId,
            "buyerName" to buyerName,
            "amount" to amount,
            "paymentMethod" to paymentMethod,
        )
    }

    data class ReviewSubmitted(
        val orderId: Long,
        val reviewerName: String,
        val productName: String,
        val rating: Int,
    ) : NotificationPayload {
        override fun toVariables(): Map<String, Any?> = mapOf(
            "orderId" to orderId,
            "reviewerName" to reviewerName,
            "productName" to productName,
            "rating" to rating,
        )
    }

    data class RefundRequested(
        val orderId: Long,
        val buyerName: String,
        val reason: String,
        val amount: Long,
    ) : NotificationPayload {
        override fun toVariables(): Map<String, Any?> = mapOf(
            "orderId" to orderId,
            "buyerName" to buyerName,
            "reason" to reason,
            "amount" to amount,
        )
    }
}
