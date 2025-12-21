package com.example.filepractice.poc.domain.entity

import java.time.LocalDateTime

data class Review(
    val id: Int,
    val productId: Int,
    val productName: String,
    val userId: Int,
    val userName: String,
    val rating: Int,
    val title: String,
    val content: String,
    val isVerifiedPurchase: Boolean,
    val createdAt: LocalDateTime,
    val helpfulCount: Int
)
