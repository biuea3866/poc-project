package com.closet.seller.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 셀러 정산 계좌
 */
@Entity
@Table(name = "seller_settlement_account")
class SellerSettlementAccount(
    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,

    @Column(name = "bank_name", nullable = false, length = 50)
    val bankName: String,

    @Column(name = "account_number", nullable = false, length = 50)
    val accountNumber: String,

    @Column(name = "account_holder", nullable = false, length = 50)
    val accountHolder: String,

    @Column(name = "is_verified", nullable = false, columnDefinition = "TINYINT(1)")
    var isVerified: Boolean = false,

    @Column(name = "verified_at", columnDefinition = "DATETIME(6)")
    var verifiedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    /** 계좌 인증 */
    fun verify() {
        this.isVerified = true
        this.verifiedAt = LocalDateTime.now()
    }
}
