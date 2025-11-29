package com.biuea.concurrency.cinema.domain

import java.time.LocalDateTime

data class Movie(
    val id: Long,
    val title: String,
    val showTime: LocalDateTime
)
