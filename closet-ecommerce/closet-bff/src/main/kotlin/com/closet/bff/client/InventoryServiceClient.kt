package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "inventory-service", url = "\${service.inventory.url}")
interface InventoryServiceClient {

    @PostMapping("/inventory/reserve")
    fun reserveStock(@RequestBody request: Any): Any

    @PostMapping("/inventory/release")
    fun releaseStock(@RequestBody request: Any): Any

    @PostMapping("/inventory/deduct")
    fun deductStock(@RequestBody request: Any): Any

    @PostMapping("/inventory/restock")
    fun restockItem(@RequestBody request: Any): Any

    @GetMapping("/inventory/{productOptionId}")
    fun getStock(@PathVariable productOptionId: Long): Any

    @GetMapping("/inventory/bulk")
    fun bulkGetStock(@RequestParam ids: List<Long>): Any
}
