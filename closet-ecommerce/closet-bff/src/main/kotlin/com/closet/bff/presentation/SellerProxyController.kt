package com.closet.bff.presentation

import com.closet.bff.client.SellerServiceClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sellers")
class SellerProxyController(
    private val sellerClient: SellerServiceClient,
) {

    @PostMapping("/apply")
    fun apply(@RequestBody request: Any) =
        sellerClient.apply(request)

    @GetMapping("/applications")
    fun getApplications(@RequestParam(defaultValue = "SUBMITTED") status: String) =
        sellerClient.getApplications(status)

    @PatchMapping("/applications/{id}/review")
    fun startReview(@PathVariable id: Long) =
        sellerClient.startReview(id)

    @PatchMapping("/applications/{id}/approve")
    fun approve(@PathVariable id: Long, @RequestBody request: Any) =
        sellerClient.approve(id, request)

    @PatchMapping("/applications/{id}/reject")
    fun reject(@PathVariable id: Long, @RequestBody request: Any) =
        sellerClient.reject(id, request)

    @GetMapping("/{id}")
    fun getSeller(@PathVariable id: Long) =
        sellerClient.getSeller(id)

    @GetMapping
    fun getSellers() =
        sellerClient.getSellers()

    @PostMapping("/{id}/settlement-account")
    fun registerSettlementAccount(@PathVariable id: Long, @RequestBody request: Any) =
        sellerClient.registerSettlementAccount(id, request)
}
