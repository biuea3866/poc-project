package com.closet.promotion.repository

import com.closet.promotion.domain.timesale.TimeSale
import com.closet.promotion.domain.timesale.TimeSaleStatus
import org.springframework.data.jpa.repository.JpaRepository

interface TimeSaleRepository : JpaRepository<TimeSale, Long> {
    fun findByStatus(status: TimeSaleStatus): List<TimeSale>
}
