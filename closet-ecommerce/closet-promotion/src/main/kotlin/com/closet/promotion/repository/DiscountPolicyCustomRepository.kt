package com.closet.promotion.repository

import com.closet.promotion.domain.discount.DiscountPolicy
import java.math.BigDecimal

interface DiscountPolicyCustomRepository {
    fun findActiveByConditions(
        categoryId: Long?,
        brandId: Long?,
        orderAmount: BigDecimal?,
    ): List<DiscountPolicy>
}
