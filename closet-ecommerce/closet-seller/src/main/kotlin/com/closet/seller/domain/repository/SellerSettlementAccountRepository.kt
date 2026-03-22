package com.closet.seller.domain.repository

import com.closet.seller.domain.SellerSettlementAccount
import org.springframework.data.jpa.repository.JpaRepository

interface SellerSettlementAccountRepository : JpaRepository<SellerSettlementAccount, Long> {
    fun findBySellerId(sellerId: Long): SellerSettlementAccount?
}
