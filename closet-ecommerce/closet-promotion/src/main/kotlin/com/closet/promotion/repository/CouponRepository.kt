package com.closet.promotion.repository

import com.closet.promotion.domain.coupon.Coupon
import com.closet.promotion.domain.coupon.CouponStatus
import org.springframework.data.jpa.repository.JpaRepository

interface CouponRepository : JpaRepository<Coupon, Long> {
    fun findByStatus(status: CouponStatus): List<Coupon>
}
