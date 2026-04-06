package com.closet.shipping.domain.cs.inquiry

import org.springframework.data.jpa.repository.JpaRepository

interface InquiryRepository : JpaRepository<Inquiry, Long> {
    fun findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId: Long): List<Inquiry>
}
