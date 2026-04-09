package com.closet.promotion.repository

import com.closet.promotion.domain.discount.DiscountHistory
import org.springframework.data.jpa.repository.JpaRepository

interface DiscountHistoryRepository : JpaRepository<DiscountHistory, Long>
