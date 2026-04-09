package com.closet.member.domain.repository

import com.closet.member.domain.point.PointPolicy
import org.springframework.data.jpa.repository.JpaRepository

interface PointPolicyRepository : JpaRepository<PointPolicy, Long>, PointPolicyCustomRepository {
    fun findByIsActiveTrue(): List<PointPolicy>
}
