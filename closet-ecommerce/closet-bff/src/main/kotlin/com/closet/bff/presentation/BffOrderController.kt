package com.closet.bff.presentation

import com.closet.bff.facade.OrderBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bff")
class BffOrderController(
    private val orderFacade: OrderBffFacade,
) {
    @GetMapping("/orders/{id}")
    fun getOrderDetail(@PathVariable id: Long) = ApiResponse.ok(orderFacade.getOrderDetail(id))

    @GetMapping("/checkout")
    fun getCheckout(@RequestHeader("X-Member-Id") memberId: Long) = ApiResponse.ok(orderFacade.getCheckout(memberId))
}
