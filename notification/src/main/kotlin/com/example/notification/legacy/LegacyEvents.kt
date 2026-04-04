// 패턴: 레거시 채널별 이벤트 (채널당 이벤트 클래스 1개씩 생성)
package com.example.notification.legacy

/**
 * 레거시 이벤트: 채널별 이벤트 클래스.
 * 문제: 알림 1종 추가 시 이벤트 3개 + 핸들러 3개 필요 (이벤트 폭발)
 *
 * 주문 접수 하나에: OrderPlacedEmailEvent + OrderPlacedPushEvent + OrderPlacedSmsEvent
 * 배송 시작 하나에: ShipmentStartedEmailEvent + ShipmentStartedPushEvent + ShipmentStartedSmsEvent
 * ...
 */

// -- 주문 접수 (3개 이벤트) --
data class OrderPlacedEmailEvent(
    val orderId: Long,
    val buyerName: String,
    val storeName: String,
    val amount: Long,
)

data class OrderPlacedPushEvent(
    val orderId: Long,
    val buyerName: String,
    val storeName: String,
)

data class OrderPlacedSmsEvent(
    val orderId: Long,
    val buyerName: String,
    val phone: String,
)

// -- 배송 시작 (3개 이벤트) --
data class ShipmentStartedEmailEvent(
    val orderId: Long,
    val buyerName: String,
    val trackingNumber: String,
)

data class ShipmentStartedPushEvent(
    val orderId: Long,
    val buyerName: String,
    val trackingNumber: String,
)

data class ShipmentStartedSmsEvent(
    val orderId: Long,
    val buyerName: String,
    val phone: String,
    val trackingNumber: String,
)

// -- 결제 완료 (3개 이벤트) --
data class PaymentCompletedEmailEvent(
    val orderId: Long,
    val buyerName: String,
    val amount: Long,
)

data class PaymentCompletedPushEvent(
    val orderId: Long,
    val buyerName: String,
    val amount: Long,
)

data class PaymentCompletedSmsEvent(
    val orderId: Long,
    val buyerName: String,
    val phone: String,
    val amount: Long,
)

// -- 인앱 알림 이벤트 (레거시: DB 저장 -> WebSocket 서버가 프론트에 push) --
data class OrderPlacedInAppEvent(
    val orderId: Long,
    val buyerName: String,
    val storeName: String,
    val alertData: Map<String, Any>,
)

data class ShipmentStartedInAppEvent(
    val orderId: Long,
    val trackingNumber: String,
)

data class PaymentCompletedInAppEvent(
    val orderId: Long,
    val amount: Long,
    val storeName: String,
)
