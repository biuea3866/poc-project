package com.example.filepractice.service

import com.example.filepractice.domain.Coupon
import com.example.filepractice.domain.Order
import com.example.filepractice.domain.Product
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OrderService {
    fun createDummyOrders(): List<Order> {
        val product1 = Product(1L, "노트북", BigDecimal("1500000"))
        val product2 = Product(2L, "마우스", BigDecimal("50000"))
        val coupon1 = Coupon(1L, "10% 할인 쿠폰", BigDecimal("0.1"))

        return listOf(
            Order(101L, LocalDateTime.now().minusDays(1), product1, coupon1),
            Order(102L, LocalDateTime.now(), product2, null),
            Order(103L, LocalDateTime.now().plusHours(1), product1, null)
        )
    }
}
