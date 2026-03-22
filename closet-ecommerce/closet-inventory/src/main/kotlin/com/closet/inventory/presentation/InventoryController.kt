package com.closet.inventory.presentation

import com.closet.common.response.ApiResponse
import com.closet.inventory.application.InventoryService
import com.closet.inventory.presentation.dto.DeductStockRequest
import com.closet.inventory.presentation.dto.InventoryResponse
import com.closet.inventory.presentation.dto.ReleaseStockRequest
import com.closet.inventory.presentation.dto.ReserveStockRequest
import com.closet.inventory.presentation.dto.RestockRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val inventoryService: InventoryService,
) {

    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.OK)
    fun reserveStock(@RequestBody @Valid request: ReserveStockRequest): ApiResponse<InventoryResponse> {
        val response = inventoryService.reserveStock(
            productOptionId = request.productOptionId,
            quantity = request.quantity,
            orderId = request.orderId,
        )
        return ApiResponse.ok(response)
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.OK)
    fun releaseStock(@RequestBody @Valid request: ReleaseStockRequest): ApiResponse<InventoryResponse> {
        val response = inventoryService.releaseStock(
            productOptionId = request.productOptionId,
            quantity = request.quantity,
            orderId = request.orderId,
        )
        return ApiResponse.ok(response)
    }

    @PostMapping("/deduct")
    @ResponseStatus(HttpStatus.OK)
    fun deductStock(@RequestBody @Valid request: DeductStockRequest): ApiResponse<InventoryResponse> {
        val response = inventoryService.deductStock(
            productOptionId = request.productOptionId,
            quantity = request.quantity,
            orderId = request.orderId,
        )
        return ApiResponse.ok(response)
    }

    @PostMapping("/restock")
    @ResponseStatus(HttpStatus.OK)
    fun restockItem(@RequestBody @Valid request: RestockRequest): ApiResponse<InventoryResponse> {
        val response = inventoryService.restockItem(
            productOptionId = request.productOptionId,
            quantity = request.quantity,
        )
        return ApiResponse.ok(response)
    }

    @GetMapping("/{productOptionId}")
    fun getStock(@PathVariable productOptionId: Long): ApiResponse<InventoryResponse> {
        val response = inventoryService.getStock(productOptionId)
        return ApiResponse.ok(response)
    }

    @GetMapping("/bulk")
    fun bulkGetStock(@RequestParam ids: List<Long>): ApiResponse<List<InventoryResponse>> {
        val response = inventoryService.bulkGetStock(ids)
        return ApiResponse.ok(response)
    }
}
