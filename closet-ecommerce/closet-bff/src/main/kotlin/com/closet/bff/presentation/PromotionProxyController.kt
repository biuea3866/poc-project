package com.closet.bff.presentation

import com.closet.bff.client.PromotionServiceClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/promotions")
class PromotionProxyController(
    private val promotionClient: PromotionServiceClient,
) {

    // Coupons
    @PostMapping("/coupons")
    fun createCoupon(@RequestBody request: Any) =
        promotionClient.createCoupon(request)

    @PostMapping("/coupons/{id}/issue")
    fun issueCoupon(@PathVariable id: Long, @RequestBody request: Any) =
        promotionClient.issueCoupon(id, request)

    @PostMapping("/coupons/{id}/use")
    fun useCoupon(@PathVariable id: Long, @RequestBody request: Any) =
        promotionClient.useCoupon(id, request)

    @GetMapping("/coupons/my")
    fun getMyCoupons(@RequestParam memberId: Long) =
        promotionClient.getMyCoupons(memberId)

    @GetMapping("/coupons/{id}/validate")
    fun validateCoupon(@PathVariable id: Long, @RequestParam orderAmount: BigDecimal) =
        promotionClient.validateCoupon(id, orderAmount)

    // Time Sales
    @PostMapping("/time-sales")
    fun createTimeSale(@RequestBody request: Any) =
        promotionClient.createTimeSale(request)

    @GetMapping("/time-sales/active")
    fun getActiveTimeSales() =
        promotionClient.getActiveTimeSales()

    @PostMapping("/time-sales/{id}/purchase")
    fun purchaseTimeSale(@PathVariable id: Long) =
        promotionClient.purchaseTimeSale(id)

    // Point Policies
    @GetMapping("/point-policies")
    fun getActivePolicies() =
        promotionClient.getActivePolicies()
}
