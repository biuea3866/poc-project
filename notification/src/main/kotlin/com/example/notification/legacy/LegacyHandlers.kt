// 패턴: 레거시 채널별 핸들러 (채널당 핸들러 클래스 1개씩 생성)
package com.example.notification.legacy

import org.springframework.stereotype.Component

/**
 * 레거시 핸들러: 채널별로 직접 발송.
 * 문제: 채널 추가 시 핸들러 클래스 추가 필요. 설정/우선순위 로직 없음.
 */

@Component
class LegacyEmailHandler {
    fun onOrderPlaced(event: OrderPlacedEmailEvent) {
        println("[LEGACY-EMAIL] 주문 접수: ${event.buyerName}님, ${event.amount}원")
    }

    fun onShipmentStarted(event: ShipmentStartedEmailEvent) {
        println("[LEGACY-EMAIL] 배송 시작: ${event.buyerName}님, 운송장 ${event.trackingNumber}")
    }

    fun onPaymentCompleted(event: PaymentCompletedEmailEvent) {
        println("[LEGACY-EMAIL] 결제 완료: ${event.buyerName}님, ${event.amount}원")
    }
}

@Component
class LegacyPushHandler {
    fun onOrderPlaced(event: OrderPlacedPushEvent) {
        println("[LEGACY-PUSH] 주문 접수: ${event.buyerName}님, ${event.storeName}")
    }

    fun onShipmentStarted(event: ShipmentStartedPushEvent) {
        println("[LEGACY-PUSH] 배송 시작: ${event.buyerName}님, 운송장 ${event.trackingNumber}")
    }

    fun onPaymentCompleted(event: PaymentCompletedPushEvent) {
        println("[LEGACY-PUSH] 결제 완료: ${event.buyerName}님, ${event.amount}원")
    }
}

@Component
class LegacySmsHandler {
    fun onOrderPlaced(event: OrderPlacedSmsEvent) {
        println("[LEGACY-SMS] 주문 접수: ${event.buyerName}님 -> ${event.phone}")
    }

    fun onShipmentStarted(event: ShipmentStartedSmsEvent) {
        println("[LEGACY-SMS] 배송 시작: ${event.buyerName}님, 운송장 ${event.trackingNumber} -> ${event.phone}")
    }

    fun onPaymentCompleted(event: PaymentCompletedSmsEvent) {
        println("[LEGACY-SMS] 결제 완료: ${event.buyerName}님, ${event.amount}원 -> ${event.phone}")
    }
}

@Component
class LegacyInAppHandler {
    // DB(alerts 테이블)에 저장 -> WebSocket 서버가 polling/event로 프론트에 push
    fun onOrderPlaced(event: OrderPlacedInAppEvent) {
        println("[LEGACY IN_APP] DB 저장: orderId=${event.orderId}, data=${event.alertData}")
        println("[LEGACY IN_APP] WebSocket 서버로 이벤트 전파")
    }

    fun onShipmentStarted(event: ShipmentStartedInAppEvent) {
        println("[LEGACY IN_APP] DB 저장: orderId=${event.orderId}, trackingNumber=${event.trackingNumber}")
        println("[LEGACY IN_APP] WebSocket 서버로 이벤트 전파")
    }

    fun onPaymentCompleted(event: PaymentCompletedInAppEvent) {
        println("[LEGACY IN_APP] DB 저장: orderId=${event.orderId}, amount=${event.amount}")
        println("[LEGACY IN_APP] WebSocket 서버로 이벤트 전파")
    }
}
