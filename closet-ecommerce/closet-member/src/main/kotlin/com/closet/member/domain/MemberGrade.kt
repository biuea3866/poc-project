package com.closet.member.domain

import java.math.BigDecimal

/**
 * 회원 등급
 */
enum class MemberGrade {
    NORMAL, SILVER, GOLD, PLATINUM;

    fun canTransitionTo(target: MemberGrade): Boolean = when (this) {
        NORMAL -> target == SILVER
        SILVER -> target in listOf(NORMAL, GOLD)
        GOLD -> target in listOf(SILVER, PLATINUM)
        PLATINUM -> target == GOLD
    }

    fun validateTransitionTo(target: MemberGrade) {
        require(canTransitionTo(target)) {
            "회원 등급을 ${this.name}에서 ${target.name}으로 변경할 수 없습니다"
        }
    }

    fun getPointRate(): BigDecimal = when (this) {
        NORMAL -> BigDecimal("0.01")
        SILVER -> BigDecimal("0.02")
        GOLD -> BigDecimal("0.03")
        PLATINUM -> BigDecimal("0.05")
    }
}
