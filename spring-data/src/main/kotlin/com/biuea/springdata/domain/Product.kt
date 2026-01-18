package com.biuea.springdata.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "product")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, precision = 10, scale = 2)
    val price: BigDecimal,

    @Column(nullable = false, length = 100)
    val category: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "stock_quantity", nullable = false)
    val stockQuantity: Int = 0,

    @Column(name = "created_date", nullable = false)
    val createdDate: LocalDate,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
