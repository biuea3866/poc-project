package com.closet.cs.domain.repository

import com.closet.cs.domain.Inquiry
import com.closet.cs.domain.InquiryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface InquiryRepository : JpaRepository<Inquiry, Long> {
    fun findByMemberIdOrderByCreatedAtDesc(
        memberId: Long,
        pageable: Pageable,
    ): Page<Inquiry>

    fun findByStatusOrderByCreatedAtAsc(
        status: InquiryStatus,
        pageable: Pageable,
    ): Page<Inquiry>
}
