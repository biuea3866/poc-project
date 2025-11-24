package com.example.cachepractice.controller

import com.example.cachepractice.domain.Order
import com.example.cachepractice.service.EagerLoadingOrderService
import com.example.cachepractice.service.LazyLoadingOrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val lazyLoadingOrderService: LazyLoadingOrderService,
    private val eagerLoadingOrderService: EagerLoadingOrderService
) {

    @GetMapping("/lazy/{id}")
    fun getOrderWithLazyCache(@PathVariable id: Long): ResponseEntity<Order> {
        val order = lazyLoadingOrderService.getOrder(id)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/lazy/no-cache/{id}")
    fun getOrderLazyNoCache(@PathVariable id: Long): ResponseEntity<Order> {
        val order = lazyLoadingOrderService.getOrderWithoutCache(id)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/eager/{id}")
    fun getOrderWithEagerCache(@PathVariable id: Long): ResponseEntity<Order> {
        val order = eagerLoadingOrderService.getOrder(id)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/eager/no-cache/{id}")
    fun getOrderEagerNoCache(@PathVariable id: Long): ResponseEntity<Order> {
        val order = eagerLoadingOrderService.getOrderWithoutCache(id)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
