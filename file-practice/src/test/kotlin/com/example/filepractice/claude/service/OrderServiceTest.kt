package com.example.filepractice.claude.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * 주문 서비스 테스트
 */
class OrderServiceTest {

    private val getOrderService = GetOrderService()

    @Test
    fun `사용자 주문 내역 조회 테스트`() {
        // Given
        val userId = 100L

        // When
        val orders = getOrderService.getOrdersByUserId(userId)

        // Then
        assertNotNull(orders)
        assertTrue(orders.isNotEmpty(), "주문 목록이 비어있지 않아야 합니다")
        assertEquals(5, orders.size, "5개의 더미 주문이 생성되어야 합니다")

        // 각 주문이 올바르게 생성되었는지 확인
        orders.forEach { order ->
            assertEquals(userId, order.userId, "주문의 사용자 ID가 일치해야 합니다")
            assertTrue(order.products.isNotEmpty(), "주문에 상품이 있어야 합니다")
            assertTrue(order.totalAmount > BigDecimal.ZERO, "총 금액이 0보다 커야 합니다")
            assertTrue(order.discountedAmount > BigDecimal.ZERO, "할인 후 금액이 0보다 커야 합니다")
            assertNotNull(order.orderNumber, "주문 번호가 있어야 합니다")
        }
    }

    @Test
    fun `사용자 주문 내역 조회 테스트 - 상품 개수 확인`() {
        // Given
        val userId = 100L

        // When
        val orders = getOrderService.getOrdersByUserId(userId)

        // Then
        orders.forEach { order ->
            assertTrue(order.products.size in 2..3, "각 주문은 2-3개의 상품을 포함해야 합니다")

            // 상품 정보 검증
            order.products.forEach { product ->
                assertNotNull(product.name, "상품명이 있어야 합니다")
                assertTrue(product.price > BigDecimal.ZERO, "상품 가격이 0보다 커야 합니다")
                assertTrue(product.quantity > 0, "상품 수량이 0보다 커야 합니다")
                assertNotNull(product.category, "상품 카테고리가 있어야 합니다")
            }
        }
    }

    @Test
    fun `사용자 주문 내역 조회 테스트 - 쿠폰 적용 확인`() {
        // Given
        val userId = 100L

        // When
        val orders = getOrderService.getOrdersByUserId(userId)

        // Then
        val ordersWithCoupon = orders.filter { it.coupon != null }
        val ordersWithoutCoupon = orders.filter { it.coupon == null }

        // 쿠폰이 있는 주문과 없는 주문이 섞여 있어야 함 (확률적이므로 여러 번 실행)
        assertTrue(orders.isNotEmpty(), "주문 목록이 있어야 합니다")

        // 쿠폰이 있는 주문의 경우
        ordersWithCoupon.forEach { order ->
            assertNotNull(order.coupon)
            assertTrue(
                order.discountedAmount < order.totalAmount,
                "쿠폰이 있으면 할인 후 금액이 총 금액보다 작아야 합니다"
            )
        }

        // 쿠폰이 없는 주문의 경우
        ordersWithoutCoupon.forEach { order ->
            assertNull(order.coupon)
            assertEquals(
                order.totalAmount,
                order.discountedAmount,
                "쿠폰이 없으면 할인 후 금액이 총 금액과 같아야 합니다"
            )
        }
    }
}
