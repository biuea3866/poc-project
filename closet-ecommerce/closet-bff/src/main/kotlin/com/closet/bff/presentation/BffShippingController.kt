package com.closet.bff.presentation

import com.closet.bff.facade.ShippingBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 배송 BFF 컨트롤러 (CP-30).
 */
@RestController
@RequestMapping("/api/v1/bff")
class BffShippingController(
    private val shippingFacade: ShippingBffFacade,
) {

    /**
     * 주문별 배송 상세 (배송 + 추적 + 반품 + 교환 집계).
     */
    @GetMapping("/orders/shipping")
    fun getShippingDetail(@RequestParam orderId: Long) = ApiResponse.ok(shippingFacade.getShippingDetail(orderId))
}
