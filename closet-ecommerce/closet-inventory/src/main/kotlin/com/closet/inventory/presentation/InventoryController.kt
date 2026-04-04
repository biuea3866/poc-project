package com.closet.inventory.presentation

import com.closet.common.auth.MemberRole
import com.closet.common.auth.RoleRequired
import com.closet.common.response.ApiResponse
import com.closet.inventory.application.CreateInventoryRequest
import com.closet.inventory.application.DeductItemRequest
import com.closet.inventory.application.DeductRequest
import com.closet.inventory.application.InboundRequest
import com.closet.inventory.application.InventoryResponse
import com.closet.inventory.application.InventoryResult
import com.closet.inventory.application.InventoryService
import com.closet.inventory.application.ReleaseItemRequest
import com.closet.inventory.application.ReleaseRequest
import com.closet.inventory.application.ReserveItemRequest
import com.closet.inventory.application.ReserveRequest
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
@RequestMapping("/api/v1/inventories")
class InventoryController(
    private val inventoryService: InventoryService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun createInventory(@RequestBody @Valid request: CreateInventoryRequest): ApiResponse<InventoryResponse> {
        val response = inventoryService.createInventory(request)
        return ApiResponse.created(response)
    }

    @GetMapping("/{id}")
    fun getInventory(@PathVariable id: Long): ApiResponse<InventoryResponse> {
        val response = inventoryService.findById(id)
        return ApiResponse.ok(response)
    }

    @GetMapping("/option/{productOptionId}")
    fun getInventoryByProductOptionId(@PathVariable productOptionId: Long): ApiResponse<InventoryResponse> {
        val response = inventoryService.findByProductOptionId(productOptionId)
        return ApiResponse.ok(response)
    }

    @GetMapping
    fun getInventoriesByProductId(@RequestParam productId: Long): ApiResponse<List<InventoryResponse>> {
        val response = inventoryService.findByProductId(productId)
        return ApiResponse.ok(response)
    }

    @PostMapping("/{id}/inbound")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun inbound(
        @PathVariable id: Long,
        @RequestBody @Valid request: InboundRequest,
    ): ApiResponse<InventoryResponse> {
        val response = inventoryService.inbound(id, request)
        return ApiResponse.ok(response)
    }

    /**
     * 내부 API: 재고 예약 (order-service에서 호출).
     */
    @PostMapping("/reserve")
    fun reserve(@RequestBody @Valid request: ReserveRequest): ApiResponse<InventoryResult> {
        val items = request.items.map {
            ReserveItemRequest(
                productOptionId = it.productOptionId,
                quantity = it.quantity,
            )
        }
        val result = inventoryService.reserveAll(request.orderId, items)
        return ApiResponse.ok(result)
    }

    /**
     * 내부 API: 재고 차감 (payment 완료 시 호출).
     */
    @PostMapping("/deduct")
    fun deduct(@RequestBody @Valid request: DeductRequest): ApiResponse<Unit> {
        val items = request.items.map {
            DeductItemRequest(
                productOptionId = it.productOptionId,
                quantity = it.quantity,
            )
        }
        inventoryService.deductAll(request.orderId, items)
        return ApiResponse.ok(Unit)
    }

    /**
     * 내부 API: 재고 해제 (주문 취소 시 호출).
     */
    @PostMapping("/release")
    fun release(@RequestBody @Valid request: ReleaseRequest): ApiResponse<Unit> {
        val items = request.items.map {
            ReleaseItemRequest(
                productOptionId = it.productOptionId,
                quantity = it.quantity,
            )
        }
        inventoryService.releaseAll(request.orderId, items, request.reason)
        return ApiResponse.ok(Unit)
    }
}
