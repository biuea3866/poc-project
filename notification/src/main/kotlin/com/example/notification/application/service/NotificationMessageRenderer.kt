// 패턴: 통합 알림 시스템의 메시지 렌더러
package com.example.notification.application.service

import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationPayload
import com.example.notification.domain.model.RenderedMessage

import org.springframework.stereotype.Component

/**
 * payload별 메시지 렌더링.
 * 단건: render(event)
 * 벌크 요약: renderCorrelatedSummary(events)
 */
@Component
class NotificationMessageRenderer {

    fun render(event: NotificationEvent): RenderedMessage {
        return when (val payload = event.payload) {
            is NotificationPayload.OrderPlaced -> renderOrderPlaced(payload)
            is NotificationPayload.ShipmentStarted -> renderShipmentStarted(payload)
            is NotificationPayload.ShipmentDelivered -> renderShipmentDelivered(payload)
            is NotificationPayload.PaymentCompleted -> renderPaymentCompleted(payload)
            is NotificationPayload.ReviewSubmitted -> renderReviewSubmitted(payload)
            is NotificationPayload.RefundRequested -> renderRefundRequested(payload)
        }
    }

    fun renderCorrelatedSummary(events: List<NotificationEvent>): RenderedMessage {
        val count = events.size
        val firstEvent = events.first()
        val storeName = when (val payload = firstEvent.payload) {
            is NotificationPayload.OrderPlaced -> payload.storeName
            else -> ""
        }

        return RenderedMessage(
            subject = "[$storeName] ${count}건의 새 주문이 접수되었습니다",
            body = "$storeName 매장에 ${count}건의 새 주문이 접수되었습니다. 확인해 주세요.",
            pushTitle = "새 주문 ${count}건",
            pushBody = "$storeName 매장에 ${count}건의 주문이 접수되었습니다",
            smsText = "[$storeName] 새 주문 ${count}건 접수",
            deepLinkUrl = "/stores/${firstEvent.storeId}/orders",
        )
    }

    private fun renderOrderPlaced(payload: NotificationPayload.OrderPlaced) = RenderedMessage(
        subject = "[${payload.storeName}] 새 주문이 접수되었습니다 (${payload.buyerName}님)",
        body = "${payload.buyerName}님이 ${payload.storeName}에서 ${payload.itemCount}개 상품, ${payload.amount}원 주문을 접수했습니다.",
        pushTitle = "새 주문 접수",
        pushBody = "${payload.buyerName}님의 주문 (${payload.amount}원)",
        smsText = "[${payload.storeName}] 새 주문: ${payload.buyerName}님, ${payload.amount}원",
        deepLinkUrl = "/orders/${payload.orderId}",
    )

    private fun renderShipmentStarted(payload: NotificationPayload.ShipmentStarted) = RenderedMessage(
        subject = "주문하신 상품이 발송되었습니다",
        body = "${payload.buyerName}님, 주문하신 상품이 ${payload.carrierName}(${payload.trackingNumber})으로 발송되었습니다.",
        pushTitle = "배송 시작",
        pushBody = "${payload.carrierName}으로 발송 (${payload.trackingNumber})",
        smsText = "배송시작: ${payload.carrierName} ${payload.trackingNumber}",
        deepLinkUrl = "/orders/${payload.orderId}/tracking",
    )

    private fun renderShipmentDelivered(payload: NotificationPayload.ShipmentDelivered) = RenderedMessage(
        subject = "주문하신 상품이 배송 완료되었습니다",
        body = "${payload.buyerName}님, 주문하신 상품이 ${payload.deliveredAt}에 배송 완료되었습니다.",
        pushTitle = "배송 완료",
        pushBody = "상품이 배송 완료되었습니다",
        smsText = "배송완료: ${payload.deliveredAt}",
        deepLinkUrl = "/orders/${payload.orderId}",
    )

    private fun renderPaymentCompleted(payload: NotificationPayload.PaymentCompleted) = RenderedMessage(
        subject = "결제가 완료되었습니다 (${payload.amount}원)",
        body = "${payload.buyerName}님의 ${payload.paymentMethod} 결제 ${payload.amount}원이 완료되었습니다.",
        pushTitle = "결제 완료",
        pushBody = "${payload.amount}원 결제 완료",
        smsText = "결제완료: ${payload.amount}원 (${payload.paymentMethod})",
        deepLinkUrl = "/orders/${payload.orderId}",
    )

    private fun renderReviewSubmitted(payload: NotificationPayload.ReviewSubmitted) = RenderedMessage(
        subject = "${payload.productName}에 새 리뷰가 등록되었습니다",
        body = "${payload.reviewerName}님이 ${payload.productName}에 ${payload.rating}점 리뷰를 남겼습니다.",
        pushTitle = "새 리뷰",
        pushBody = "${payload.productName}: ${payload.rating}점 (${payload.reviewerName}님)",
        smsText = "새리뷰: ${payload.productName} ${payload.rating}점",
        deepLinkUrl = "/products/${payload.orderId}/reviews",
    )

    private fun renderRefundRequested(payload: NotificationPayload.RefundRequested) = RenderedMessage(
        subject = "환불 요청이 접수되었습니다 (${payload.amount}원)",
        body = "${payload.buyerName}님이 ${payload.amount}원 환불을 요청했습니다. 사유: ${payload.reason}",
        pushTitle = "환불 요청",
        pushBody = "${payload.buyerName}님 환불 요청 (${payload.amount}원)",
        smsText = "환불요청: ${payload.buyerName}님, ${payload.amount}원",
        deepLinkUrl = "/orders/${payload.orderId}/refund",
    )
}
