package com.biuea.kafkaretry.common.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderEvent(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val productName: String,
    val quantity: Int,
    val totalAmount: BigDecimal,
    val timestamp: Instant = Instant.now(),
    val simulateFailure: Boolean = false
)
