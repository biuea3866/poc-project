package com.closet.common.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.math.BigDecimal

@Embeddable
data class Money(
    @Column(columnDefinition = "DECIMAL(15,2)")
    val amount: BigDecimal
) : Comparable<Money> {

    init {
        require(amount >= BigDecimal.ZERO) { "금액은 0 이상이어야 합니다" }
    }

    operator fun plus(other: Money): Money = Money(amount + other.amount)

    operator fun minus(other: Money): Money = Money(amount - other.amount)

    operator fun times(quantity: Int): Money = Money(amount * BigDecimal(quantity))

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    override fun compareTo(other: Money): Int = amount.compareTo(other.amount)

    companion object {
        val ZERO = Money(BigDecimal.ZERO)
        fun of(amount: Long): Money = Money(BigDecimal(amount))
        fun of(amount: String): Money = Money(BigDecimal(amount))
    }
}
