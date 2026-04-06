package com.closet.external.payment.danal

import com.closet.external.domain.MockPayment
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
 * Mock Danal Payment API
 * @see https://docs.danal.co.kr
 */
@RestController
@RequestMapping("/danal/payments")
class DanalController(
    private val pgService: PaymentPgService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/ready")
    fun ready(
        @RequestBody request: Map<String, Any>,
    ): ResponseEntity<*> {
        val orderId =
            request["orderId"]?.toString()
                ?: return ResponseEntity.badRequest().body(danalResp("4000", "orderId는 필수입니다", null))
        val amount =
            request["amount"]?.toString()?.toLongOrNull()
                ?: return ResponseEntity.badRequest().body(danalResp("4000", "amount는 필수입니다", null))

        val tid = pgService.generateKey("DN")
        val payment =
            pgService.createPayment(
                "DANAL",
                tid,
                orderId,
                amount,
                request["itemName"]?.toString(),
                request["buyerName"]?.toString(),
                request["buyerTel"]?.toString(),
            )
        log.info("[Danal] 결제 준비: tid={}, orderId={}, amount={}", tid, orderId, amount)

        return ResponseEntity.ok(danalResp("0000", "성공", toDanalBody(payment)))
    }

    @PostMapping("/approve")
    fun approve(
        @RequestBody request: Map<String, Any>,
    ): ResponseEntity<*> {
        val tid =
            request["tid"]?.toString()
                ?: return ResponseEntity.badRequest().body(danalResp("4000", "tid는 필수입니다", null))
        return try {
            val payment = pgService.approvePayment(tid, request["payMethod"]?.toString() ?: "PHONE")
            log.info("[Danal] 결제 승인: tid={}", tid)
            ResponseEntity.ok(danalResp("0000", "성공", toDanalBody(payment)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(danalResp("4004", e.message ?: "", null))
        }
    }

    @PostMapping("/{tid}/cancel")
    fun cancel(
        @PathVariable tid: String,
        @RequestBody request: Map<String, Any>,
    ): ResponseEntity<*> {
        return try {
            val payment =
                pgService.cancelPayment(
                    tid,
                    request["cancelReason"]?.toString() ?: "고객 요청",
                    request["cancelAmount"]?.toString()?.toLongOrNull(),
                )
            log.info("[Danal] 결제 취소: tid={}", tid)
            ResponseEntity.ok(danalResp("0000", "성공", toDanalBody(payment)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(danalResp("4004", e.message ?: "", null))
        }
    }

    @GetMapping("/{tid}")
    fun get(
        @PathVariable tid: String,
    ): ResponseEntity<*> {
        val payment = pgService.findByPaymentKey(tid) ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(danalResp("0000", "성공", toDanalBody(payment)))
    }

    private fun toDanalBody(p: MockPayment) =
        mapOf(
            "tid" to p.paymentKey, "orderId" to p.orderId, "amount" to p.totalAmount,
            "status" to if (p.status == "DONE") "PAID" else p.status,
            "payMethod" to (p.method ?: "PHONE"), "itemName" to p.orderName,
            "authNo" to p.approveNo, "approvedAt" to p.approvedAt?.toString(),
            "canceledAt" to p.canceledAt?.toString(), "cancelAmount" to p.cancelAmount,
        )

    private fun danalResp(
        code: String,
        msg: String,
        data: Any?,
    ) = mapOf("resultCode" to code, "resultMessage" to msg, "data" to data)
}
