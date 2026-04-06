package com.closet.shipping.domain.cs.inquiry

import org.springframework.data.jpa.repository.JpaRepository

interface InquiryAttachmentRepository : JpaRepository<InquiryAttachment, Long> {
    fun findByInquiryId(inquiryId: Long): List<InquiryAttachment>
}
