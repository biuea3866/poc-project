package com.closet.bff.presentation

import com.closet.bff.client.CsServiceClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/cs")
class CsProxyController(
    private val csClient: CsServiceClient,
) {

    // Inquiries
    @PostMapping("/inquiries")
    fun createInquiry(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: Any,
    ) = csClient.createInquiry(memberId, request)

    @GetMapping("/inquiries/my")
    fun getMyInquiries(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ) = csClient.getMyInquiries(memberId, page, size)

    @GetMapping("/inquiries/{id}")
    fun getInquiry(@PathVariable id: Long) =
        csClient.getInquiry(id)

    @PostMapping("/inquiries/{id}/reply")
    fun addReply(@PathVariable id: Long, @RequestBody request: Any) =
        csClient.addReply(id, request)

    @PatchMapping("/inquiries/{id}/close")
    fun closeInquiry(@PathVariable id: Long) =
        csClient.closeInquiry(id)

    @GetMapping("/inquiries")
    fun getInquiriesByStatus(
        @RequestParam status: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = csClient.getInquiriesByStatus(status, page, size)

    // FAQs
    @GetMapping("/faqs")
    fun getFaqs(@RequestParam(required = false) category: String?) =
        csClient.getFaqs(category)

    @PostMapping("/faqs")
    fun createFaq(@RequestBody request: Any) =
        csClient.createFaq(request)

    @PutMapping("/faqs/{id}")
    fun updateFaq(@PathVariable id: Long, @RequestBody request: Any) =
        csClient.updateFaq(id, request)
}
