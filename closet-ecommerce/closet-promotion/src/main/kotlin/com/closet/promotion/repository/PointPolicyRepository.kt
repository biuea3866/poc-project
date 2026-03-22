package com.closet.promotion.repository

import com.closet.promotion.domain.point.PointPolicy
import org.springframework.data.jpa.repository.JpaRepository

interface PointPolicyRepository : JpaRepository<PointPolicy, Long> {
    fun findByIsActiveTrue(): List<PointPolicy>
}
