package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "settlement-service", url = "\${service.settlement.url}")
interface SettlementServiceClient {

    // Settlements
    @PostMapping("/settlements/calculate")
    fun calculate(@RequestBody request: Any): Any

    @GetMapping("/settlements")
    fun getSettlements(
        @RequestParam sellerId: Long,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Any

    @GetMapping("/settlements/{id}")
    fun getSettlement(@PathVariable id: Long): Any

    @PatchMapping("/settlements/{id}/confirm")
    fun confirm(@PathVariable id: Long): Any

    @PatchMapping("/settlements/{id}/pay")
    fun pay(@PathVariable id: Long): Any

    // Commission Rates
    @GetMapping("/commission-rates")
    fun getCommissionRates(): Any

    @PutMapping("/commission-rates/{categoryId}")
    fun setCommissionRate(@PathVariable categoryId: Long, @RequestBody request: Any): Any
}
