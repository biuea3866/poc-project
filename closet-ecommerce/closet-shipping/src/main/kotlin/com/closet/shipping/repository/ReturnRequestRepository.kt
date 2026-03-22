package com.closet.shipping.repository

import com.closet.shipping.domain.ReturnRequest
import org.springframework.data.jpa.repository.JpaRepository

interface ReturnRequestRepository : JpaRepository<ReturnRequest, Long> {
    fun findByOrderId(orderId: Long): List<ReturnRequest>
}
