package com.closet.order.domain.cart

import com.closet.common.vo.Money
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "cart_item")
@EntityListeners(AuditingEntityListener::class)
class CartItem(
    @Column(name = "cart_id", nullable = false)
    val cartId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "product_option_id", nullable = false)
    val productOptionId: Long,

    @Column(name = "quantity", nullable = false)
    var quantity: Int,

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "unit_price", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    val unitPrice: Money,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    fun updateQuantity(quantity: Int) {
        require(quantity > 0) { "수량은 1 이상이어야 합니다" }
        this.quantity = quantity
    }

    companion object {
        fun create(
            cartId: Long,
            productId: Long,
            productOptionId: Long,
            quantity: Int,
            unitPrice: Money,
        ): CartItem {
            require(quantity > 0) { "수량은 1 이상이어야 합니다" }
            return CartItem(
                cartId = cartId,
                productId = productId,
                productOptionId = productOptionId,
                quantity = quantity,
                unitPrice = unitPrice,
            )
        }
    }
}
