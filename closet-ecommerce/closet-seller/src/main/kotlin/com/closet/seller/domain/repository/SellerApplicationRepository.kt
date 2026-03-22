package com.closet.seller.domain.repository

import com.closet.seller.domain.ApplicationStatus
import com.closet.seller.domain.SellerApplication
import org.springframework.data.jpa.repository.JpaRepository

interface SellerApplicationRepository : JpaRepository<SellerApplication, Long> {
    fun findAllByStatus(status: ApplicationStatus): List<SellerApplication>
    fun findAllBySellerId(sellerId: Long): List<SellerApplication>
}
