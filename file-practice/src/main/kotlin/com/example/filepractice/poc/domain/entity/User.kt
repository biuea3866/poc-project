package com.example.filepractice.poc.domain.entity

import java.time.LocalDate

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val address: String?,
    val joinedAt: LocalDate,
    val membershipLevel: MembershipLevel
)

enum class MembershipLevel {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM
}
