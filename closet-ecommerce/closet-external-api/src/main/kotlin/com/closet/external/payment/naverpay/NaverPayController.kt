package com.closet.external.payment.naverpay

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
 * Mock Naver Pay API
 * @see https://developer.pay.naver.com/docs/v2/api
 */
@RestController
@RequestMapping("/naverpay/payments")
class NaverPayController(
    private val pgService: PaymentPgService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/v2.2/reserve")
    fun reserve(@RequestBody request: Map<String, Any>): ResponseEntity<*> {
        val orderId = request["merchantPayKey"]?.toString()
            ?: return ResponseEntity.badRequest().body(naverResp("InvalidParameter", "merchantPayKey는 필수입니다", null))
        val amount = request["totalPayAmount"]?.toString()?.toLongOrNull()
            ?: return ResponseEntity.badRequest().body(naverResp("InvalidParameter", "totalPayAmount는 필수입니다", null))

        val paymentId = pgService.generateKey("NP")
        pgService.createPayment("NAVER_PAY", paymentId, orderId, amount, request["productName"]?.toString())
        log.info("[NaverPay] 결제 예약: paymentId={}, amount={}", paymentId, amount)

        return ResponseEntity.ok(naverResp("Success", "성공", mapOf(
            "paymentId" to paymentId, "reserveUrl" to "http://localhost:9090/naverpay/mock/redirect?paymentId=$paymentId",
        )))
    }

    @PostMapping("/v2.2/apply/payment")
    fun apply(@RequestBody request: Map<String, Any>): ResponseEntity<*> {
        val paymentId = request["paymentId"]?.toString()
            ?: return ResponseEntity.badRequest().body(naverResp("InvalidParameter", "paymentId는 필수입니다", null))
        return try {
            val payment = pgService.approvePayment(paymentId, "CARD")
            log.info("[NaverPay] 결제 승인: paymentId={}", paymentId)
            ResponseEntity.ok(naverResp("Success", "성공", toNaverBody(payment)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(naverResp("NotFound", e.message ?: "", null))
        }
    }

    @PostMapping("/v1/cancel")
    fun cancel(@RequestBody request: Map<String, Any>): ResponseEntity<*> {
        val paymentId = request["paymentId"]?.toString()
            ?: return ResponseEntity.badRequest().body(naverResp("InvalidParameter", "paymentId는 필수입니다", null))
        val cancelAmount = request["cancelAmount"]?.toString()?.toLongOrNull()
        return try {
            val payment = pgService.cancelPayment(paymentId, request["cancelReason"]?.toString() ?: "고객 요청", cancelAmount)
            log.info("[NaverPay] 결제 취소: paymentId={}", paymentId)
            ResponseEntity.ok(naverResp("Success", "성공", toNaverBody(payment)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(naverResp("NotFound", e.message ?: "", null))
        }
    }

    @GetMapping("/v2.2/list/history")
    fun history(@RequestParam paymentId: String): ResponseEntity<*> {
        val payment = pgService.findByPaymentKey(paymentId) ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(naverResp("Success", "성공", mapOf("list" to listOf(toNaverBody(payment)))))
    }

    private fun toNaverBody(p: MockPayment) = mapOf(
        "paymentId" to p.paymentKey, "merchantPayKey" to p.orderId, "productName" to p.orderName,
        "totalPayAmount" to p.totalAmount, "primaryPayMeans" to (p.method ?: "CARD"),
        "status" to if (p.status == "DONE") "PAYMENT_APPROVED" else if (p.status == "CANCELED") "CANCELED" else p.status,
        "approvedAt" to p.approvedAt?.toString(), "canceledAt" to p.canceledAt?.toString(),
    )

    private fun naverResp(code: String, message: String, body: Any?) = mapOf("code" to code, "message" to message, "body" to body)
}
