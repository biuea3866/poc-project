package com.closet.member.domain.repository

import com.closet.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?
    fun existsByEmail(email: String): Boolean
    fun findByIdAndDeletedAtIsNull(id: Long): Member?
}
