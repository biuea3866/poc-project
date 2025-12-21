package com.biuea.concurrency.waitingnumber.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "product_waiting",
    indexes = [Index(name = "idx_product_id", columnList = "product_id")]
)
data class ProductWaiting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "curr_no")
    val currentNumber: Int,

    @Column(name = "product_id", unique = true)
    val productId: Long,

    @Column(nullable = false, name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)