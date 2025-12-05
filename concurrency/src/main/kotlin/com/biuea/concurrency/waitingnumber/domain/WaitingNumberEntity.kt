package com.biuea.concurrency.waitingnumber.domain

import jakarta.persistence.*
import java.time.LocalDateTime

// 유저 발급
@Entity
@Table(name = "waiting_numbers")
data class WaitingNumberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, name = "product_id")
    val productId: Long,

    @Column(nullable = false, name = "waiting_number", length = 50)
    val waitingNumber: String,

    @Column(nullable = false, name = "user_id")
    val userId: Long,

    @Column(nullable = false, name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
