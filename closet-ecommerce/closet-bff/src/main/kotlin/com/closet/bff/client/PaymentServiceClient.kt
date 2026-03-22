package com.closet.bff.client

import com.closet.bff.dto.PaymentResponse
import com.closet.common.response.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "payment-service", url = "\${service.payment.url}")
interface PaymentServiceClient {

    @GetMapping("/payments/orders/{orderId}")
    fun getPaymentByOrderId(@PathVariable orderId: Long): ApiResponse<PaymentResponse>

    @PostMapping("/payments/confirm")
    fun confirmPayment(@RequestBody request: Any): ApiResponse<PaymentResponse>

    @PostMapping("/payments/{id}/cancel")
    fun cancelPayment(@PathVariable id: Long, @RequestBody request: Any): ApiResponse<PaymentResponse>
}
