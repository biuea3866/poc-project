package com.closet.external.payment.kakaopay

import com.closet.external.domain.MockPayment
import com.closet.external.payment.PaymentPgService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Mock Kakao Pay API
 * @see https://developers.kakao.com/docs/latest/ko/kakaopay/single-payment
 */
@RestController
@RequestMapping("/kakaopay/online/v1/payment")
class KakaoPayController(
    private val pgService: PaymentPgService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/ready")
    fun ready(@RequestBody request: Map<String, Any>): ResponseEntity<*> {
        val orderId = request["partner_order_id"]?.toString()
            ?: return ResponseEntity.badRequest().body(err(-1, "partner_order_id는 필수입니다"))
        val amount = request["total_amount"]?.toString()?.toLongOrNull()
            ?: return ResponseEntity.badRequest().body(err(-1, "total_amount는 필수입니다"))

        val tid = pgService.generateKey("T")
        val payment = pgService.createPayment("KAKAO_PAY", tid, orderId, amount, request["item_name"]?.toString())
        log.info("[KakaoPay] 결제 준비: tid={}, orderId={}, amount={}", tid, orderId, amount)

        return ResponseEntity.ok(mapOf(
            "tid" to tid, "next_redirect_pc_url" to "http://localhost:9090/kakaopay/mock/redirect?tid=$tid",
            "next_redirect_mobile_url" to "http://localhost:9090/kakaopay/mock/redirect?tid=$tid",
            "created_at" to payment.createdAt.toString(),
        ))
    }

    @PostMapping("/approve")
    fun approve(@RequestBody request: Map<String, Any>): ResponseEntity<*> {
        val tid = request["tid"]?.toString()
            ?: return ResponseEntity.badRequest().body(err(-1, "tid는 필수입니다"))
        return try {
            val payment = pgService.approvePayment(tid, "MONEY")
            log.info("[KakaoPay] 결제 승인: tid={}", tid)
            ResponseEntity.ok(toKakaoResponse(payment))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(err(-2, e.message ?: ""))
        }
    }

    @PostMapping("/cancel")
    fun cancel(@RequestBody request: Map<String, Any>): ResponseEntity<*> {
        val tid = request["tid"]?.toString()
            ?: return ResponseEntity.badRequest().body(err(-1, "tid는 필수입니다"))
        val cancelAmount = request["cancel_amount"]?.toString()?.toLongOrNull()
        return try {
            val payment = pgService.cancelPayment(tid, "고객 요청", cancelAmount)
            log.info("[KakaoPay] 결제 취소: tid={}", tid)
            ResponseEntity.ok(toKakaoResponse(payment))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(err(-2, e.message ?: ""))
        }
    }

    @GetMapping("/order")
    fun getOrder(@RequestParam tid: String): ResponseEntity<*> {
        val payment = pgService.findByPaymentKey(tid) ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(toKakaoResponse(payment))
    }

    private fun toKakaoResponse(p: MockPayment) = mapOf(
        "tid" to p.paymentKey, "cid" to "TC0ONETIME", "status" to mapKakaoStatus(p.status),
        "partner_order_id" to p.orderId, "payment_method_type" to (p.method ?: "MONEY"),
        "item_name" to p.orderName, "amount" to mapOf("total" to p.totalAmount, "vat" to p.totalAmount / 11),
        "approved_at" to p.approvedAt?.toString(), "canceled_at" to p.canceledAt?.toString(),
        "canceled_amount" to if (p.cancelAmount > 0) mapOf("total" to p.cancelAmount) else null,
    )

    private fun mapKakaoStatus(status: String) = when (status) {
        "READY" -> "READY"
        "DONE" -> "SUCCESS_PAYMENT"
        "CANCELED" -> "CANCEL_PAYMENT"
        else -> status
    }

    private fun err(code: Int, msg: String) = mapOf("error_code" to code, "error_message" to msg)
}
