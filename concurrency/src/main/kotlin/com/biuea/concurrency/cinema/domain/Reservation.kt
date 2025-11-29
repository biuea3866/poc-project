package com.biuea.concurrency.cinema.domain

import java.time.LocalDateTime

data class Reservation(
    val id: Long,
    val userId: String,
    val movieId: Long,
    val seatId: Long,
    val reservedAt: LocalDateTime = LocalDateTime.now()
)
