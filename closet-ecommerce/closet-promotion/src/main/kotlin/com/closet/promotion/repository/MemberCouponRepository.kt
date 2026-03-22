package com.closet.promotion.repository

import com.closet.promotion.domain.coupon.MemberCoupon
import com.closet.promotion.domain.coupon.MemberCouponStatus
import org.springframework.data.jpa.repository.JpaRepository

interface MemberCouponRepository : JpaRepository<MemberCoupon, Long> {
    fun findByMemberIdAndStatus(memberId: Long, status: MemberCouponStatus): List<MemberCoupon>
    fun findByMemberId(memberId: Long): List<MemberCoupon>
    fun existsByCouponIdAndMemberId(couponId: Long, memberId: Long): Boolean
}
