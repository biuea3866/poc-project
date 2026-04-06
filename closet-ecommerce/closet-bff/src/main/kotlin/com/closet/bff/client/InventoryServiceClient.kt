package com.closet.bff.client

import com.closet.bff.dto.InventoryBffResponse
import com.closet.common.response.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

/**
 * Inventory Service Feign Client (CP-30).
 * Port: 8089 (PD-05)
 */
@FeignClient(name = "inventory-service", url = "\${service.inventory.url}")
interface InventoryServiceClient {
    @GetMapping("/api/v1/inventories/option/{productOptionId}")
    fun getInventoryByProductOptionId(
        @PathVariable productOptionId: Long,
    ): ApiResponse<InventoryBffResponse>

    @GetMapping("/api/v1/inventories")
    fun getInventoriesByProductId(
        @RequestParam productId: Long,
    ): ApiResponse<List<InventoryBffResponse>>

    @GetMapping("/api/v1/inventories/restock-notification/count")
    fun getRestockNotificationCount(
        @RequestParam productOptionId: Long,
    ): ApiResponse<Long>
}
