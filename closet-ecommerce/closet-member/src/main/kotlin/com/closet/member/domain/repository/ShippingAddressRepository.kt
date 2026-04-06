package com.closet.member.domain.repository

import com.closet.member.domain.ShippingAddress
import org.springframework.data.jpa.repository.JpaRepository

interface ShippingAddressRepository : JpaRepository<ShippingAddress, Long> {
    fun findByMemberIdAndDeletedAtIsNull(memberId: Long): List<ShippingAddress>

    fun findByIdAndMemberIdAndDeletedAtIsNull(
        id: Long,
        memberId: Long,
    ): ShippingAddress?

    fun findByMemberIdAndIsDefaultTrueAndDeletedAtIsNull(memberId: Long): ShippingAddress?
}
