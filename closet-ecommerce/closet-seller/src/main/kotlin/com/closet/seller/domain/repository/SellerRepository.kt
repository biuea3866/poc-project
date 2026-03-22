package com.closet.seller.domain.repository

import com.closet.seller.domain.Seller
import com.closet.seller.domain.SellerStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SellerRepository : JpaRepository<Seller, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Seller?
    fun findAllByDeletedAtIsNull(): List<Seller>
    fun findAllByStatusAndDeletedAtIsNull(status: SellerStatus): List<Seller>
    fun existsByEmail(email: String): Boolean
    fun existsByBusinessNumber(businessNumber: String): Boolean
}
