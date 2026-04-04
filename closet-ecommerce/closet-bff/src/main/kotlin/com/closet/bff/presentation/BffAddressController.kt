package com.closet.bff.presentation

import com.closet.bff.dto.AddAddressRequest
import com.closet.bff.dto.UpdateAddressRequest
import com.closet.bff.facade.AddressBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bff/addresses")
class BffAddressController(
    private val addressFacade: AddressBffFacade,
) {
    @PostMapping
    fun addAddress(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: AddAddressRequest,
    ) = ApiResponse.created(addressFacade.addAddress(memberId, request))

    @PutMapping("/{id}")
    fun updateAddress(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
        @RequestBody request: UpdateAddressRequest,
    ) = ApiResponse.ok(addressFacade.updateAddress(memberId, id, request))

    @DeleteMapping("/{id}")
    fun deleteAddress(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        addressFacade.deleteAddress(memberId, id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/default")
    fun setDefault(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
    ) = ApiResponse.ok(addressFacade.setDefault(memberId, id))
}
