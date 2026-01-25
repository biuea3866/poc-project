package com.biuea.springdata.kafka.dto

import java.time.Instant

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: Instant = Instant.now()
)
