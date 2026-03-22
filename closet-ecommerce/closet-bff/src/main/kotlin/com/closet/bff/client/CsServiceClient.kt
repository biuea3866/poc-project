package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "cs-service", url = "\${service.cs.url}")
interface CsServiceClient {

    // Inquiries
    @PostMapping("/cs/inquiries")
    fun createInquiry(@RequestHeader("X-Member-Id") memberId: Long, @RequestBody request: Any): Any

    @GetMapping("/cs/inquiries/my")
    fun getMyInquiries(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): Any

    @GetMapping("/cs/inquiries/{id}")
    fun getInquiry(@PathVariable id: Long): Any

    @PostMapping("/cs/inquiries/{id}/reply")
    fun addReply(@PathVariable id: Long, @RequestBody request: Any): Any

    @PatchMapping("/cs/inquiries/{id}/close")
    fun closeInquiry(@PathVariable id: Long): Any

    @GetMapping("/cs/inquiries")
    fun getInquiriesByStatus(
        @RequestParam status: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Any

    // FAQs
    @GetMapping("/cs/faqs")
    fun getFaqs(@RequestParam(required = false) category: String?): Any

    @PostMapping("/cs/faqs")
    fun createFaq(@RequestBody request: Any): Any

    @PutMapping("/cs/faqs/{id}")
    fun updateFaq(@PathVariable id: Long, @RequestBody request: Any): Any
}
