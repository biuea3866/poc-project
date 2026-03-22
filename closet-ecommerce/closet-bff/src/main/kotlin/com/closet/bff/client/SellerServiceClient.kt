package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "seller-service", url = "\${service.seller.url}")
interface SellerServiceClient {

    @PostMapping("/sellers/apply")
    fun apply(@RequestBody request: Any): Any

    @GetMapping("/sellers/applications")
    fun getApplications(@RequestParam(defaultValue = "SUBMITTED") status: String): Any

    @PatchMapping("/sellers/applications/{id}/review")
    fun startReview(@PathVariable id: Long): Any

    @PatchMapping("/sellers/applications/{id}/approve")
    fun approve(@PathVariable id: Long, @RequestBody request: Any): Any

    @PatchMapping("/sellers/applications/{id}/reject")
    fun reject(@PathVariable id: Long, @RequestBody request: Any): Any

    @GetMapping("/sellers/{id}")
    fun getSeller(@PathVariable id: Long): Any

    @GetMapping("/sellers")
    fun getSellers(): Any

    @PostMapping("/sellers/{id}/settlement-account")
    fun registerSettlementAccount(@PathVariable id: Long, @RequestBody request: Any): Any
}
