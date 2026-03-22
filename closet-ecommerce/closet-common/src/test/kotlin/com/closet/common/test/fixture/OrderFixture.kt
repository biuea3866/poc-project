package com.closet.common.test.fixture

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Order 도메인 테스트 Fixture.
 *
 * 테스트에서 Order 관련 객체를 간편하게 생성한다.
 * 엔티티가 아직 구현되지 않은 시점에서 Map 기반으로 데이터를 표현하며,
 * 엔티티 구현 후 실제 타입으로 교체한다.
 */
object OrderFixture {

    fun createOrder(
        memberId: Long = 1L,
        orderNumber: String = generateOrderNumber(),
        status: String = "PENDING",
        totalAmount: BigDecimal = BigDecimal("54800"),
        shippingFee: BigDecimal = BigDecimal("3000"),
        shippingAddressId: Long = 1L,
        items: List<Map<String, Any?>> = listOf(createOrderItem()),
    ): Map<String, Any?> = mapOf(
        "memberId" to memberId,
        "orderNumber" to orderNumber,
        "status" to status,
        "totalAmount" to totalAmount,
        "shippingFee" to shippingFee,
        "shippingAddressId" to shippingAddressId,
        "items" to items,
        "orderedAt" to LocalDateTime.now(),
        "createdAt" to LocalDateTime.now(),
    )

    fun createOrderItem(
        productId: Long = 1L,
        productOptionId: Long = 1L,
        productName: String = "오버핏 코튼 티셔츠",
        optionDescription: String = "BLACK / L",
        quantity: Int = 2,
        unitPrice: BigDecimal = BigDecimal("24900"),
        totalPrice: BigDecimal = BigDecimal("49800"),
        status: String = "ORDERED",
    ): Map<String, Any?> = mapOf(
        "productId" to productId,
        "productOptionId" to productOptionId,
        "productName" to productName,
        "optionDescription" to optionDescription,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "totalPrice" to totalPrice,
        "status" to status,
    )

    fun createCartItem(
        memberId: Long = 1L,
        productId: Long = 1L,
        productOptionId: Long = 1L,
        quantity: Int = 1,
    ): Map<String, Any?> = mapOf(
        "memberId" to memberId,
        "productId" to productId,
        "productOptionId" to productOptionId,
        "quantity" to quantity,
        "addedAt" to LocalDateTime.now(),
    )

    fun createOrderWithMultipleItems(
        memberId: Long = 1L,
        itemCount: Int = 3,
    ): Map<String, Any?> {
        val items = (1..itemCount).map { idx ->
            createOrderItem(
                productId = idx.toLong(),
                productOptionId = idx.toLong(),
                productName = "테스트 상품 $idx",
                optionDescription = "옵션 $idx",
                unitPrice = BigDecimal("${10000 + idx * 5000}"),
                totalPrice = BigDecimal("${10000 + idx * 5000}"),
                quantity = 1,
            )
        }
        val totalAmount = items.sumOf { (it["totalPrice"] as BigDecimal) }
        return createOrder(
            memberId = memberId,
            totalAmount = totalAmount + BigDecimal("3000"),
            items = items,
        )
    }

    private fun generateOrderNumber(): String {
        val now = LocalDateTime.now()
        val datePart = "%d%02d%02d".format(now.year, now.monthValue, now.dayOfMonth)
        val randomPart = UUID.randomUUID().toString().take(8).uppercase()
        return "ORD-$datePart-$randomPart"
    }
}
