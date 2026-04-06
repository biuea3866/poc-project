package com.closet.member.domain

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * 배송지 엔티티
 */
@Entity
@Table(name = "shipping_address")
class ShippingAddress(
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(nullable = false, length = 50)
    var name: String,
    @Column(nullable = false, length = 20)
    var phone: String,
    @Column(name = "zip_code", nullable = false, length = 10)
    var zipCode: String,
    @Column(nullable = false, length = 200)
    var address: String,
    @Column(name = "detail_address", length = 200)
    var detailAddress: String? = null,
    @Column(name = "is_default", nullable = false, columnDefinition = "TINYINT(1)")
    var isDefault: Boolean = false,
) : BaseEntity() {
    fun update(
        name: String,
        phone: String,
        zipCode: String,
        address: String,
        detailAddress: String?,
    ) {
        this.name = name
        this.phone = phone
        this.zipCode = zipCode
        this.address = address
        this.detailAddress = detailAddress
    }

    fun markAsDefault() {
        this.isDefault = true
    }

    fun unmarkDefault() {
        this.isDefault = false
    }
}
