package com.example.payment

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val faultInjector: FaultInjector,
) {

    @PostMapping
    fun pay(@RequestBody req: PaymentRequest): PaymentResponse {
        faultInjector.maybeFail("payment")
        return PaymentResponse(
            paymentId = UUID.randomUUID().toString(),
            orderId = req.orderId,
            amount = req.amount,
            status = "APPROVED",
        )
    }
}

data class PaymentRequest(
    val orderId: String,
    val amount: Long,
)

data class PaymentResponse(
    val paymentId: String,
    val orderId: String,
    val amount: Long,
    val status: String,
)
