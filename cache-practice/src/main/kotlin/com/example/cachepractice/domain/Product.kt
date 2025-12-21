package com.example.cachepractice.domain

import java.io.Serializable
import java.math.BigDecimal

/**
 * Product 도메인 엔티티
 * 캐시 가능하도록 Serializable 구현
 */
data class Product(
    val id: Long,
    val name: String,
    val price: BigDecimal
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L

        /**
         * 캐시 관통(Cache Penetration) 문제 해결을 위한 센티널 객체
         * 존재하지 않는 제품을 표현하기 위해 사용
         */
        val NOT_FOUND = Product(
            id = -1L,
            name = "NOT_FOUND",
            price = BigDecimal.ZERO
        )
    }

    fun isNotFound(): Boolean = this == NOT_FOUND
}
