package com.closet.shipping.domain.cs.claim

import org.springframework.data.jpa.repository.JpaRepository

interface ClaimRequestRepository : JpaRepository<ClaimRequest, Long> {
    fun findByMemberId(memberId: Long): List<ClaimRequest>

    fun findByOrderId(orderId: Long): List<ClaimRequest>

    fun findByStatus(status: ClaimStatus): List<ClaimRequest>
}
