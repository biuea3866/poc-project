package com.biuea.kafkaretry.common.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PaymentEvent(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val amount: BigDecimal,
    val currency: String = "KRW",
    val status: PaymentStatus = PaymentStatus.PENDING,
    val timestamp: Instant = Instant.now(),
    val simulateFailure: Boolean = false
)

enum class PaymentStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}
