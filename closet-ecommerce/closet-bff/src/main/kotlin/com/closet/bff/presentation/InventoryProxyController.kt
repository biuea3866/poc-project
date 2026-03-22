package com.closet.bff.presentation

import com.closet.bff.client.InventoryServiceClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryProxyController(
    private val inventoryClient: InventoryServiceClient,
) {

    @PostMapping("/reserve")
    fun reserveStock(@RequestBody request: Any) =
        inventoryClient.reserveStock(request)

    @PostMapping("/release")
    fun releaseStock(@RequestBody request: Any) =
        inventoryClient.releaseStock(request)

    @PostMapping("/deduct")
    fun deductStock(@RequestBody request: Any) =
        inventoryClient.deductStock(request)

    @PostMapping("/restock")
    fun restockItem(@RequestBody request: Any) =
        inventoryClient.restockItem(request)

    @GetMapping("/{productOptionId}")
    fun getStock(@PathVariable productOptionId: Long) =
        inventoryClient.getStock(productOptionId)

    @GetMapping("/bulk")
    fun bulkGetStock(@RequestParam ids: List<Long>) =
        inventoryClient.bulkGetStock(ids)
}
