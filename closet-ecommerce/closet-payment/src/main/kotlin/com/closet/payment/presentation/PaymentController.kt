package com.closet.payment.presentation

import com.closet.common.response.ApiResponse
import com.closet.payment.application.CancelPaymentRequest
import com.closet.payment.application.ConfirmPaymentRequest
import com.closet.payment.application.PaymentResponse
import com.closet.payment.application.PaymentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentService: PaymentService,
) {

    @GetMapping("/orders/{orderId}")
    fun getByOrderId(@PathVariable orderId: Long): ApiResponse<PaymentResponse> {
        return ApiResponse.ok(paymentService.getByOrderId(orderId))
    }

    @PostMapping("/confirm")
    fun confirm(@RequestBody request: ConfirmPaymentRequest): ApiResponse<PaymentResponse> {
        return ApiResponse.ok(paymentService.confirm(request))
    }

    @PostMapping("/{id}/cancel")
    fun cancel(
        @PathVariable id: Long,
        @RequestBody request: CancelPaymentRequest,
    ): ApiResponse<PaymentResponse> {
        return ApiResponse.ok(paymentService.cancel(id, request))
    }
}
