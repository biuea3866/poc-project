package com.closet.notification.consumer.event

/**
 * event.closet.inventory 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - RESTOCK_NOTIFICATION: 재입고 발생 시 구독 회원에게 알림 발송
 * - LOW_STOCK: 안전재고 이하 (처리하지 않음)
 * - OUT_OF_STOCK: 품절 (처리하지 않음)
 * - INSUFFICIENT: 재고 부족 (처리하지 않음)
 */
data class InventoryEvent(
    val eventType: String,
    val productOptionId: Long,
    val sku: String? = null,
    val availableQuantity: Int? = null,
    val memberIds: List<Long> = emptyList(),
)
