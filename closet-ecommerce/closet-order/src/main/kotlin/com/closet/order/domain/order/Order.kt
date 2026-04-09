package com.closet.order.domain.order

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.vo.Money
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener::class)
class Order(
    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    val orderNumber: String,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "total_amount", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    var totalAmount: Money,
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "discount_amount", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    var discountAmount: Money = Money.ZERO,
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "shipping_fee", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    var shippingFee: Money = Money.ZERO,
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "payment_amount", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    var paymentAmount: Money,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: OrderStatus = OrderStatus.PENDING,
    @Column(name = "receiver_name", nullable = false, length = 50)
    val receiverName: String,
    @Column(name = "receiver_phone", nullable = false, length = 20)
    val receiverPhone: String,
    @Column(name = "zip_code", nullable = false, length = 10)
    val zipCode: String,
    @Column(name = "address", nullable = false, length = 200)
    val address: String,
    @Column(name = "detail_address", nullable = false, length = 200)
    val detailAddress: String,
    @Column(name = "reservation_expires_at", columnDefinition = "DATETIME(6)")
    var reservationExpiresAt: ZonedDateTime? = null,
    @Column(name = "ordered_at", columnDefinition = "DATETIME(6)")
    var orderedAt: ZonedDateTime? = null,
    @Column(name = "delivered_at", columnDefinition = "DATETIME(6)")
    var deliveredAt: ZonedDateTime? = null,
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

    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    var deletedAt: ZonedDateTime? = null

    private fun transitionTo(
        newStatus: OrderStatus,
        reason: String? = null,
    ) {
        status.validateTransitionTo(newStatus)
        status = newStatus
    }

    fun place() {
        transitionTo(OrderStatus.STOCK_RESERVED)
        this.reservationExpiresAt = ZonedDateTime.now().plusMinutes(15)
        this.orderedAt = ZonedDateTime.now()
    }

    fun pay() {
        transitionTo(OrderStatus.PAID)
    }

    fun prepare() {
        transitionTo(OrderStatus.PREPARING)
    }

    fun ship() {
        transitionTo(OrderStatus.SHIPPED)
    }

    fun deliver() {
        transitionTo(OrderStatus.DELIVERED)
        this.deliveredAt = ZonedDateTime.now()
    }

    fun confirm() {
        transitionTo(OrderStatus.CONFIRMED)
    }

    fun cancel(reason: String) {
        if (status != OrderStatus.PAID && status != OrderStatus.PENDING && status != OrderStatus.STOCK_RESERVED) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "취소는 PENDING, STOCK_RESERVED, PAID 상태에서만 가능합니다. 현재 상태: ${status.name}",
            )
        }
        transitionTo(OrderStatus.CANCELLED, reason)
    }

    fun fail() {
        transitionTo(OrderStatus.FAILED)
    }

    fun requestReturn(
        itemId: Long,
        reason: String,
    ) {
        if (status != OrderStatus.DELIVERED) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "반품 요청은 DELIVERED 상태에서만 가능합니다. 현재 상태: ${status.name}",
            )
        }
    }

    fun calculatePaymentAmount(): Money {
        return totalAmount - discountAmount + shippingFee
    }

    companion object {
        private val ORDER_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        fun generateOrderNumber(): String {
            val now = ZonedDateTime.now()
            val datePart = now.format(ORDER_NUMBER_DATE_FORMAT)
            val randomPart = (100000..999999).random().toString()
            return "$datePart$randomPart"
        }

        fun create(
            memberId: Long,
            sellerId: Long,
            items: List<OrderItem>,
            receiverName: String,
            receiverPhone: String,
            zipCode: String,
            address: String,
            detailAddress: String,
            shippingFee: Money = Money.ZERO,
            discountAmount: Money = Money.ZERO,
        ): Order {
            require(items.isNotEmpty()) { "주문 항목은 1개 이상이어야 합니다" }

            val totalAmount = items.fold(Money.ZERO) { acc, item -> acc + item.totalPrice }
            val paymentAmount = totalAmount - discountAmount + shippingFee

            return Order(
                orderNumber = generateOrderNumber(),
                memberId = memberId,
                sellerId = sellerId,
                totalAmount = totalAmount,
                discountAmount = discountAmount,
                shippingFee = shippingFee,
                paymentAmount = paymentAmount,
                receiverName = receiverName,
                receiverPhone = receiverPhone,
                zipCode = zipCode,
                address = address,
                detailAddress = detailAddress,
            )
        }
    }
}
