package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.math.BigDecimal

@FeignClient(name = "promotion-service", url = "\${service.promotion.url}")
interface PromotionServiceClient {

    // Coupons
    @PostMapping("/promotions/coupons")
    fun createCoupon(@RequestBody request: Any): Any

    @PostMapping("/promotions/coupons/{id}/issue")
    fun issueCoupon(@PathVariable id: Long, @RequestBody request: Any): Any

    @PostMapping("/promotions/coupons/{id}/use")
    fun useCoupon(@PathVariable id: Long, @RequestBody request: Any): Any

    @GetMapping("/promotions/coupons/my")
    fun getMyCoupons(@RequestParam memberId: Long): Any

    @GetMapping("/promotions/coupons/{id}/validate")
    fun validateCoupon(@PathVariable id: Long, @RequestParam orderAmount: BigDecimal): Any

    // Time Sales
    @PostMapping("/promotions/time-sales")
    fun createTimeSale(@RequestBody request: Any): Any

    @GetMapping("/promotions/time-sales/active")
    fun getActiveTimeSales(): Any

    @PostMapping("/promotions/time-sales/{id}/purchase")
    fun purchaseTimeSale(@PathVariable id: Long): Any

    // Point Policies
    @GetMapping("/promotions/point-policies")
    fun getActivePolicies(): Any
}
