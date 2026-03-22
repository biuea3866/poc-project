package com.closet.order.domain.order

import com.closet.common.vo.Money
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "order_item")
class OrderItem(
    @Column(name = "order_id", nullable = false)
    val orderId: Long = 0,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "product_option_id", nullable = false)
    val productOptionId: Long,

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,

    @Column(name = "option_name", nullable = false, length = 200)
    val optionName: String,

    @Column(name = "category_id", nullable = false)
    val categoryId: Long,

    @Column(name = "quantity", nullable = false)
    val quantity: Int,

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "unit_price", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    val unitPrice: Money,

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "total_price", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    val totalPrice: Money,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: OrderItemStatus = OrderItemStatus.ORDERED,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: LocalDateTime

    fun cancel() {
        this.status = OrderItemStatus.CANCELLED
    }

    fun requestReturn() {
        this.status = OrderItemStatus.RETURN_REQUESTED
    }

    companion object {
        fun create(
            orderId: Long = 0,
            productId: Long,
            productOptionId: Long,
            productName: String,
            optionName: String,
            categoryId: Long,
            quantity: Int,
            unitPrice: Money,
        ): OrderItem {
            return OrderItem(
                orderId = orderId,
                productId = productId,
                productOptionId = productOptionId,
                productName = productName,
                optionName = optionName,
                categoryId = categoryId,
                quantity = quantity,
                unitPrice = unitPrice,
                totalPrice = unitPrice * quantity,
            )
        }
    }
}
