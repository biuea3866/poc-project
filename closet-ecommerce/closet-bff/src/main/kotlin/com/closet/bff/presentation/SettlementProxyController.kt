package com.closet.bff.presentation

import com.closet.bff.client.SettlementServiceClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class SettlementProxyController(
    private val settlementClient: SettlementServiceClient,
) {

    // Settlements
    @PostMapping("/settlements/calculate")
    fun calculate(@RequestBody request: Any) =
        settlementClient.calculate(request)

    @GetMapping("/settlements")
    fun getSettlements(
        @RequestParam sellerId: Long,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = settlementClient.getSettlements(sellerId, status, page, size)

    @GetMapping("/settlements/{id}")
    fun getSettlement(@PathVariable id: Long) =
        settlementClient.getSettlement(id)

    @PatchMapping("/settlements/{id}/confirm")
    fun confirm(@PathVariable id: Long) =
        settlementClient.confirm(id)

    @PatchMapping("/settlements/{id}/pay")
    fun pay(@PathVariable id: Long) =
        settlementClient.pay(id)

    // Commission Rates
    @GetMapping("/commission-rates")
    fun getCommissionRates() =
        settlementClient.getCommissionRates()

    @PutMapping("/commission-rates/{categoryId}")
    fun setCommissionRate(@PathVariable categoryId: Long, @RequestBody request: Any) =
        settlementClient.setCommissionRate(categoryId, request)
}
