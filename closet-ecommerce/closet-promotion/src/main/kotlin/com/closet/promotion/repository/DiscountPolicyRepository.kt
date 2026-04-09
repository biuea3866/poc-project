package com.closet.promotion.repository

import com.closet.promotion.domain.discount.DiscountPolicy
import org.springframework.data.jpa.repository.JpaRepository

interface DiscountPolicyRepository : JpaRepository<DiscountPolicy, Long>, DiscountPolicyCustomRepository
