package com.closet.cs.domain.repository

import com.closet.cs.domain.InquiryReply
import org.springframework.data.jpa.repository.JpaRepository

interface InquiryReplyRepository : JpaRepository<InquiryReply, Long> {
    fun findByInquiryIdOrderByCreatedAtAsc(inquiryId: Long): List<InquiryReply>
}
