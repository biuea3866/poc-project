package com.example.filepractice.poc.domain.entity

import java.math.BigDecimal
import java.time.LocalDate

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: BigDecimal,
    val stock: Int,
    val manufacturer: String?,
    val description: String?,
    val registeredAt: LocalDate
)
