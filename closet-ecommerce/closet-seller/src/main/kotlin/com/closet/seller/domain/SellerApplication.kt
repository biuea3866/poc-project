package com.closet.seller.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 입점 신청
 */
@Entity
@Table(name = "seller_application")
class SellerApplication(
    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,

    @Column(name = "brand_name", nullable = false, length = 100)
    val brandName: String,

    @Column(name = "category_ids", columnDefinition = "TEXT")
    val categoryIds: String? = null,

    @Column(name = "business_license_url", nullable = false, length = 500)
    val businessLicenseUrl: String,

    @Column(name = "bank_name", nullable = false, length = 50)
    val bankName: String,

    @Column(name = "account_number", nullable = false, length = 50)
    val accountNumber: String,

    @Column(name = "account_holder", nullable = false, length = 50)
    val accountHolder: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: ApplicationStatus = ApplicationStatus.SUBMITTED,

    @Column(name = "reject_reason", length = 500)
    var rejectReason: String? = null,

    @Column(name = "submitted_at", nullable = false, columnDefinition = "DATETIME(6)")
    val submittedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "reviewed_at", columnDefinition = "DATETIME(6)")
    var reviewedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    /** 심사 시작 */
    fun startReview() {
        status.validateTransitionTo(ApplicationStatus.REVIEWING)
        this.status = ApplicationStatus.REVIEWING
    }

    /** 승인 */
    fun approve() {
        status.validateTransitionTo(ApplicationStatus.APPROVED)
        this.status = ApplicationStatus.APPROVED
        this.reviewedAt = LocalDateTime.now()
    }

    /** 반려 */
    fun reject(reason: String) {
        status.validateTransitionTo(ApplicationStatus.REJECTED)
        require(reason.isNotBlank()) { "반려 사유는 필수입니다" }
        this.status = ApplicationStatus.REJECTED
        this.rejectReason = reason
        this.reviewedAt = LocalDateTime.now()
    }
}
