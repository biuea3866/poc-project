package com.closet.external.domain

import jakarta.persistence.Column
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
@Table(name = "mock_payment")
@EntityListeners(AuditingEntityListener::class)
class MockPayment(
    @Column(name = "provider", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val provider: String,
    @Column(name = "payment_key", nullable = false, length = 200)
    val paymentKey: String,
    @Column(name = "order_id", nullable = false, length = 100)
    val orderId: String,
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: String = "READY",
    @Column(name = "method", length = 30, columnDefinition = "VARCHAR(30)")
    var method: String? = null,
    @Column(name = "total_amount", nullable = false)
    val totalAmount: Long,
    @Column(name = "balance_amount", nullable = false)
    var balanceAmount: Long,
    @Column(name = "cancel_amount", nullable = false)
    var cancelAmount: Long = 0,
    @Column(name = "cancel_reason", length = 200)
    var cancelReason: String? = null,
    @Column(name = "order_name", length = 200)
    var orderName: String? = null,
    @Column(name = "buyer_name", length = 50)
    var buyerName: String? = null,
    @Column(name = "buyer_tel", length = 20)
    var buyerTel: String? = null,
    @Column(name = "card_number", length = 30)
    var cardNumber: String? = null,
    @Column(name = "card_type", length = 20)
    var cardType: String? = null,
    @Column(name = "approve_no", length = 30)
    var approveNo: String? = null,
    @Column(name = "approved_at", columnDefinition = "DATETIME(6)")
    var approvedAt: ZonedDateTime? = null,
    @Column(name = "canceled_at", columnDefinition = "DATETIME(6)")
    var canceledAt: ZonedDateTime? = null,
    @Column(name = "extra_data", columnDefinition = "TEXT")
    var extraData: String? = null,
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

    fun approve(
        method: String,
        approveNo: String,
    ) {
        this.status = "DONE"
        this.method = method
        this.approveNo = approveNo
        this.cardNumber = "4330****1234"
        this.cardType = "신용"
        this.approvedAt = ZonedDateTime.now()
    }

    fun cancel(
        reason: String,
        amount: Long? = null,
    ) {
        val actualCancelAmount = amount ?: this.totalAmount
        this.status = "CANCELED"
        this.cancelAmount = actualCancelAmount
        this.balanceAmount = this.totalAmount - actualCancelAmount
        this.cancelReason = reason
        this.canceledAt = ZonedDateTime.now()
    }
}
