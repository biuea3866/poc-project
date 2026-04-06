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
import java.time.ZonedDateTime

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
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "refund_amount", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    var refundAmount: Money = Money.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: PaymentStatus = PaymentStatus.PENDING,
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

    fun confirm(
        paymentKey: String,
        method: PaymentMethod,
    ) {
        this.paymentKey = paymentKey
        this.method = method
        this.status = PaymentStatus.PAID
    }

    fun cancel() {
        this.status = PaymentStatus.CANCELLED
    }

    /**
     * 부분 환불 (PD-12).
     * 환불금액 = 결제금액 - 반품배송비(BUYER 부담시).
     * 이미 환불된 금액이 있으면 누적한다.
     */
    fun refund(amount: Money) {
        require(this.status == PaymentStatus.PAID || this.status == PaymentStatus.REFUNDED) {
            "환불은 PAID 또는 REFUNDED 상태에서만 가능합니다. 현재 상태: ${this.status}"
        }
        require(amount.amount > java.math.BigDecimal.ZERO) {
            "환불 금액은 0보다 커야 합니다"
        }
        val totalRefunded = this.refundAmount + amount
        require(totalRefunded <= this.finalAmount) {
            "환불 금액이 결제 금액을 초과합니다. 결제금액=${this.finalAmount.amount}, 기환불=${this.refundAmount.amount}, 요청=${amount.amount}"
        }
        this.refundAmount = totalRefunded
        this.status = PaymentStatus.REFUNDED
    }

    companion object {
        fun create(
            orderId: Long,
            finalAmount: Money,
        ): Payment {
            return Payment(
                orderId = orderId,
                finalAmount = finalAmount,
            )
        }
    }
}

enum class PaymentStatus {
    PENDING,
    PAID,
    CANCELLED,
    REFUNDED,
}

enum class PaymentMethod {
    CARD,
    BANK_TRANSFER,
    VIRTUAL_ACCOUNT,
    MOBILE,
}
