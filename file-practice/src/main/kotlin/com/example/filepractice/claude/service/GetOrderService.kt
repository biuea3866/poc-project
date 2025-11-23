package com.example.filepractice.claude.service

import com.example.filepractice.claude.domain.Coupon
import com.example.filepractice.claude.domain.Order
import com.example.filepractice.claude.domain.Product
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * 주문 서비스
 *
 * 주문 데이터를 조회하고 관리하는 서비스입니다.
 * 대용량 데이터 처리를 위해 Sequence 기반 스트리밍을 지원합니다.
 */
@Service
class GetOrderService {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 1000 // 기본 페이지 크기
    }

    /**
     * 사용자의 주문 내역 조회 (List 방식 - 소량 데이터용)
     *
     * @param userId 사용자 ID
     * @return 주문 목록
     */
    fun getOrdersByUserId(userId: Long): List<Order> {
        // 실제 환경에서는 데이터베이스에서 조회
        // 현재는 더미 데이터를 생성하여 반환
        return generateDummyOrders(userId, 5).toList()
    }

    /**
     * 사용자의 주문 내역을 Sequence로 조회 (대용량 데이터용)
     *
     * Sequence를 사용하여 지연 평가(lazy evaluation)로 메모리 효율적으로 처리
     * 실제 환경에서는 커서 기반 조회나 페이징을 사용
     *
     * @param userId 사용자 ID
     * @param pageSize 페이지 크기 (기본 1000)
     * @return 주문 Sequence
     */
    fun getOrdersByUserIdAsSequence(userId: Long, pageSize: Int = DEFAULT_PAGE_SIZE): Sequence<Order> {
        // 실제 환경에서는 DB 커서나 페이징을 사용하여 스트리밍 조회
        // 예: JPA의 Stream, Exposed의 Query.fetchBatchedResults 등

        return sequence {
            var offset = 0
            var hasMore = true

            while (hasMore) {
                // 페이지 단위로 데이터 조회 (실제로는 DB 쿼리)
                val orders = fetchOrdersPage(userId, offset, pageSize)

                if (orders.isEmpty()) {
                    hasMore = false
                } else {
                    yieldAll(orders)
                    offset += pageSize
                }
            }
        }
    }

    /**
     * 페이지 단위로 주문 조회 (실제 환경에서는 DB 쿼리)
     *
     * @param userId 사용자 ID
     * @param offset 시작 위치
     * @param limit 조회 개수
     * @return 주문 목록
     */
    private fun fetchOrdersPage(userId: Long, offset: Int, limit: Int): List<Order> {
        // 실제로는 DB에서 LIMIT/OFFSET 또는 커서 기반 조회
        // SELECT * FROM orders WHERE user_id = ? LIMIT ? OFFSET ?

        // 더미 데이터 생성 (테스트용)
        val totalOrders = 5 // 실제로는 COUNT 쿼리로 전체 개수 확인
        val start = offset
        val end = minOf(offset + limit, totalOrders)

        if (start >= totalOrders) {
            return emptyList()
        }

        return generateDummyOrders(userId, end - start, start).toList()
    }

    /**
     * 더미 주문 데이터 생성 (Sequence로 반환)
     *
     * 테스트 및 예제를 위한 더미 데이터를 Sequence로 생성합니다.
     * Sequence를 사용하여 필요한 만큼만 메모리에 로드됩니다.
     */
    private fun generateDummyOrders(userId: Long, count: Int, startIndex: Int = 0): Sequence<Order> {
        return sequence {
            repeat(count) { index ->
                val orderId = (startIndex + index + 1).toLong()
                val orderNumber = "ORD-2025-${String.format("%06d", orderId)}"

                // 상품 생성 (2-3개)
                val productCount = Random.nextInt(2, 4)
                val products = (1..productCount).map { productIndex ->
                    val productId = orderId * 100 + productIndex
                    val price = BigDecimal(Random.nextInt(10_000, 100_000))

                    Product(
                        id = productId,
                        name = "상품 ${productId}",
                        price = price,
                        quantity = Random.nextInt(1, 5),
                        category = listOf("전자기기", "의류", "식품", "생활용품", "도서").random()
                    )
                }

                // 총 금액 계산
                val totalAmount = products.sumOf { it.price.multiply(it.quantity.toBigDecimal()) }

                // 쿠폰 생성 (50% 확률)
                val coupon = if (Random.nextBoolean()) {
                    val discountRate = BigDecimal("0.${Random.nextInt(5, 20)}")
                    val discountAmount = totalAmount.multiply(discountRate)
                    Coupon(
                        id = orderId,
                        code = "COUPON-${orderId}",
                        name = "${(discountRate.multiply(BigDecimal(100))).toInt()}% 할인 쿠폰",
                        discountRate = discountRate,
                        discountAmount = discountAmount
                    )
                } else null

                val discountedAmount = coupon?.let { totalAmount.subtract(it.discountAmount) } ?: totalAmount

                val order = Order(
                    id = orderId,
                    orderNumber = orderNumber,
                    userId = userId,
                    products = products,
                    coupon = coupon,
                    totalAmount = totalAmount,
                    discountedAmount = discountedAmount,
                    orderDate = LocalDateTime.now().minusDays(Random.nextLong(1, 30)),
                    status = Order.OrderStatus.entries.random()
                )

                yield(order)
            }
        }
    }
}
