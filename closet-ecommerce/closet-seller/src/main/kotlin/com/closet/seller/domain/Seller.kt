package com.closet.seller.domain

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * 셀러 Aggregate Root
 */
@Entity
@Table(name = "seller")
class Seller(
    @Column(nullable = false, unique = true, length = 200)
    val email: String,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(name = "business_name", nullable = false, length = 100)
    val businessName: String,

    @Column(name = "business_number", nullable = false, unique = true, length = 20)
    val businessNumber: String,

    @Column(name = "representative_name", nullable = false, length = 50)
    val representativeName: String,

    @Column(nullable = false, length = 20)
    var phone: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: SellerStatus = SellerStatus.PENDING,

    @Column(name = "commission_rate", precision = 5, scale = 2)
    var commissionRate: BigDecimal? = null,
) : BaseEntity() {

    companion object {
        /** 셀러 등록 팩토리 메서드 */
        fun register(
            email: String,
            name: String,
            businessName: String,
            businessNumber: String,
            representativeName: String,
            phone: String,
        ): Seller {
            return Seller(
                email = email,
                name = name,
                businessName = businessName,
                businessNumber = businessNumber,
                representativeName = representativeName,
                phone = phone,
                status = SellerStatus.PENDING,
            )
        }
    }

    /** 셀러 활성화 (입점 승인 시) */
    fun activate(commissionRate: BigDecimal) {
        status.validateTransitionTo(SellerStatus.ACTIVE)
        require(commissionRate > BigDecimal.ZERO) { "수수료율은 0보다 커야 합니다" }
        this.status = SellerStatus.ACTIVE
        this.commissionRate = commissionRate
    }

    /** 셀러 정지 */
    fun suspend(reason: String) {
        status.validateTransitionTo(SellerStatus.SUSPENDED)
        require(reason.isNotBlank()) { "정지 사유는 필수입니다" }
        this.status = SellerStatus.SUSPENDED
    }

    /** 셀러 탈퇴 */
    fun withdraw() {
        status.validateTransitionTo(SellerStatus.WITHDRAWN)
        this.status = SellerStatus.WITHDRAWN
        softDelete()
    }
}
