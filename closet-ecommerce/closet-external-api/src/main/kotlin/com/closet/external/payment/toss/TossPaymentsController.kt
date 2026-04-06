package com.closet.external.payment.toss

import com.closet.external.payment.PaymentPgService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Mock Toss Payments API
 * @see https://docs.tosspayments.com/reference
 */
@RestController
@RequestMapping("/toss/v1/payments")
class TossPaymentsController(
    private val pgService: PaymentPgService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/confirm")
    fun confirm(
        @RequestBody request: Map<String, Any>,
    ): ResponseEntity<*> {
        val paymentKey =
            request["paymentKey"]?.toString()
                ?: return ResponseEntity.badRequest().body(err("INVALID_REQUEST", "paymentKey는 필수입니다"))
        val orderId =
            request["orderId"]?.toString()
                ?: return ResponseEntity.badRequest().body(err("INVALID_REQUEST", "orderId는 필수입니다"))
        val amount =
            request["amount"]?.toString()?.toLongOrNull()
                ?: return ResponseEntity.badRequest().body(err("INVALID_REQUEST", "amount는 필수입니다"))

        pgService.createPayment("TOSS", paymentKey, orderId, amount, request["orderName"]?.toString())
        pgService.approvePayment(paymentKey, "CARD")
        val approved = pgService.findByPaymentKey(paymentKey)!!

        log.info("[Toss] 결제 승인: paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount)
        return ResponseEntity.ok(toTossResponse(approved))
    }

    @PostMapping("/{paymentKey}/cancel")
    fun cancel(
        @PathVariable paymentKey: String,
        @RequestBody request: Map<String, Any>,
    ): ResponseEntity<*> {
        val reason = request["cancelReason"]?.toString() ?: "고객 요청"
        val cancelAmount = request["cancelAmount"]?.toString()?.toLongOrNull()
        return try {
            val payment = pgService.cancelPayment(paymentKey, reason, cancelAmount)
            log.info("[Toss] 결제 취소: paymentKey={}", paymentKey)
            ResponseEntity.ok(toTossResponse(payment))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build<Any>()
        }
    }

    @GetMapping("/{paymentKey}")
    fun getByPaymentKey(
        @PathVariable paymentKey: String,
    ): ResponseEntity<*> {
        val payment = pgService.findByPaymentKey(paymentKey) ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(toTossResponse(payment))
    }

    @GetMapping("/orders/{orderId}")
    fun getByOrderId(
        @PathVariable orderId: String,
    ): ResponseEntity<*> {
        val payment = pgService.findByOrderId(orderId) ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(toTossResponse(payment))
    }

    private fun toTossResponse(p: com.closet.external.domain.MockPayment) =
        mapOf(
            "paymentKey" to p.paymentKey, "orderId" to p.orderId, "orderName" to p.orderName,
            "status" to
                if (p.status == "DONE") {
                    "DONE"
                } else if (p.status == "CANCELED") {
                    "CANCELED"
                } else {
                    p.status
                },
            "method" to "카드", "totalAmount" to p.totalAmount, "balanceAmount" to p.balanceAmount,
            "suppliedAmount" to (p.totalAmount * 10 / 11), "vat" to (p.totalAmount / 11),
            "requestedAt" to p.createdAt.toString(), "approvedAt" to p.approvedAt?.toString(),
            "currency" to "KRW",
            "card" to mapOf("number" to p.cardNumber, "approveNo" to p.approveNo, "cardType" to p.cardType),
        )

    private fun err(
        code: String,
        msg: String,
    ) = mapOf("code" to code, "message" to msg)
}
