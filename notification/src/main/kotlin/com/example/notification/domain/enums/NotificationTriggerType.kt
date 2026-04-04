// 패턴: 통합 알림 시스템의 트리거 타입
package com.example.notification.domain.enums

/**
 * 알림 트리거 타입.
 */
enum class NotificationTriggerType(
    val category: NotificationCategory,
    val description: String,
    val defaultPriority: NotificationPriority,
) {
    ORDER_PLACED(NotificationCategory.ORDER, "주문 접수", NotificationPriority.HIGH),
    SHIPMENT_STARTED(NotificationCategory.SHIPMENT, "배송 시작", NotificationPriority.NORMAL),
    SHIPMENT_DELIVERED(NotificationCategory.SHIPMENT, "배송 완료", NotificationPriority.NORMAL),
    PAYMENT_COMPLETED(NotificationCategory.PAYMENT, "결제 완료", NotificationPriority.NORMAL),
    REVIEW_SUBMITTED(NotificationCategory.REVIEW, "리뷰 등록", NotificationPriority.LOW),
    REFUND_REQUESTED(NotificationCategory.SYSTEM, "환불 요청", NotificationPriority.CRITICAL),
    ;

    companion object {
        fun findByCategory(category: NotificationCategory): List<NotificationTriggerType> =
            entries.filter { it.category == category }
    }
}
