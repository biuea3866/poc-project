package com.closet.shipping.domain.cs.inquiry

import org.springframework.data.jpa.repository.JpaRepository

interface InquiryAnswerRepository : JpaRepository<InquiryAnswer, Long> {
    fun findByInquiryIdOrderByCreatedAtAsc(inquiryId: Long): List<InquiryAnswer>
}
