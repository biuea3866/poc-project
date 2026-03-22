package com.closet.bff.presentation

import com.closet.bff.client.PaymentServiceClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentProxyController(
    private val paymentClient: PaymentServiceClient,
) {

    @PostMapping("/confirm")
    fun confirm(@RequestBody request: Any) =
        paymentClient.confirmPayment(request)

    @PostMapping("/{id}/cancel")
    fun cancel(@PathVariable id: Long, @RequestBody request: Any) =
        paymentClient.cancelPayment(id, request)

    @GetMapping("/orders/{orderId}")
    fun getByOrderId(@PathVariable orderId: Long) =
        paymentClient.getPaymentByOrderId(orderId)
}
