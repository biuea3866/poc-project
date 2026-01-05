package com.biuea.kotlinpractice.async

import com.biuea.kotlinpractice.domain.Order
import com.biuea.kotlinpractice.domain.OrderStatus
import com.biuea.kotlinpractice.domain.Product
import com.biuea.kotlinpractice.repository.OrderRepository
import com.biuea.kotlinpractice.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * 트랜잭션 처리를 담당하는 별도 서비스
 * Self-invocation 문제 해결을 위해 분리
 */
@Service
class OrderTransactionService(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) {

    /**
     * 새 트랜잭션에서 주문 생성
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createOrderInNewTransaction(product: Product, quantity: Int) {
        val order = Order(
            customerName = "Customer-${System.currentTimeMillis() % 1000}",
            product = product,
            quantity = quantity,
            totalPrice = product.price.multiply(BigDecimal.valueOf(quantity.toLong())),
            status = OrderStatus.PENDING
        )
        orderRepository.save(order)
    }

    /**
     * 새 트랜잭션에서 주문 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processOrderInNewTransaction(request: DatabasePerformanceExample.OrderRequest) {
        // 상품 조회
        val product = productRepository.findById(request.productId)
            .orElseThrow { IllegalArgumentException("Product not found: ${request.productId}") }

        // 재고 확인
        if (product.stock < request.quantity) {
            throw IllegalStateException("Insufficient stock for product: ${product.name}")
        }

        // 재고 차감
        product.stock -= request.quantity

        // 주문 생성
        val order = Order(
            customerName = request.customerName,
            product = product,
            quantity = request.quantity,
            totalPrice = product.price.multiply(BigDecimal.valueOf(request.quantity.toLong())),
            status = OrderStatus.CONFIRMED
        )

        productRepository.save(product)
        orderRepository.save(order)

        // 비즈니스 로직 시뮬레이션 (외부 API 호출 등)
        Thread.sleep(10)
    }
}
