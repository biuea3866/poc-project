package com.closet.payment.presentation

import com.closet.common.response.ApiResponse
import com.closet.payment.application.CancelPaymentRequest
import com.closet.payment.application.ConfirmPaymentRequest
import com.closet.payment.application.PaymentResponse
import com.closet.payment.application.PaymentService
import com.closet.payment.application.RefundPaymentRequest
import com.closet.payment.infrastructure.PaymentType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentService: PaymentService,
) {
    @GetMapping("/orders/{orderId}")
    fun getByOrderId(
        @PathVariable orderId: Long,
    ): ApiResponse<PaymentResponse> {
        return ApiResponse.ok(paymentService.getByOrderId(orderId))
    }

    @PostMapping("/confirm")
    fun confirm(
        @RequestBody request: ConfirmPaymentRequest,
    ): ApiResponse<PaymentResponse> {
        return ApiResponse.ok(paymentService.confirm(request))
    }

    @PostMapping("/{id}/cancel")
    fun cancel(
        @PathVariable id: Long,
        @RequestBody request: CancelPaymentRequest,
        @RequestParam(defaultValue = "TOSS") paymentType: PaymentType,
    ): ApiResponse<PaymentResponse> {
        return ApiResponse.ok(paymentService.cancel(id, request, paymentType))
    }

    /**
     * 부분 환불 (PD-12).
     * POST /api/v1/payments/{id}/refund
     */
    @PostMapping("/{id}/refund")
    fun refund(
        @PathVariable id: Long,
        @RequestBody request: RefundPaymentRequest,
        @RequestParam(defaultValue = "TOSS") paymentType: PaymentType,
    ): ApiResponse<PaymentResponse> {
        return ApiResponse.ok(paymentService.refund(id, request, paymentType))
    }
}
