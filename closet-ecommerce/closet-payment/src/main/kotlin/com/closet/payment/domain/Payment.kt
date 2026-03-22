package com.closet.payment.domain

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
import java.time.LocalDateTime

@Entity
@Table(name = "payment")
@EntityListeners(AuditingEntityListener::class)
class Payment(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "payment_key", length = 200)
    var paymentKey: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 30, columnDefinition = "VARCHAR(30)")
    var method: PaymentMethod? = null,

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "final_amount", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    var finalAmount: Money,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: PaymentStatus = PaymentStatus.PENDING,
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

    fun confirm(paymentKey: String, method: PaymentMethod) {
        this.paymentKey = paymentKey
        this.method = method
        this.status = PaymentStatus.PAID
    }

    fun cancel() {
        this.status = PaymentStatus.CANCELLED
    }

    companion object {
        fun create(orderId: Long, finalAmount: Money): Payment {
            return Payment(
                orderId = orderId,
                finalAmount = finalAmount,
            )
        }
    }
}

enum class PaymentStatus {
    PENDING, PAID, CANCELLED, REFUNDED
}

enum class PaymentMethod {
    CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE
}
