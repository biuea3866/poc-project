package com.biuea.concurrency.waitingnumber.domain

import java.time.LocalDateTime

data class WaitingNumberResult(
    val productId: Long,
    val waitingNumber: String,
    val userId: Long,
    val timestamp: LocalDateTime
)
