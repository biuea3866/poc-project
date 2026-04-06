package com.closet.bff.facade

import com.closet.bff.client.MemberServiceClient
import com.closet.bff.dto.AddAddressRequest
import com.closet.bff.dto.ShippingAddressResponse
import com.closet.bff.dto.UpdateAddressRequest
import org.springframework.stereotype.Service

@Service
class AddressBffFacade(
    private val memberClient: MemberServiceClient,
) {
    fun addAddress(
        memberId: Long,
        request: AddAddressRequest,
    ): ShippingAddressResponse {
        return memberClient.addAddress(memberId, request).data!!
    }

    fun updateAddress(
        memberId: Long,
        addressId: Long,
        request: UpdateAddressRequest,
    ): ShippingAddressResponse {
        return memberClient.updateAddress(memberId, addressId, request).data!!
    }

    fun deleteAddress(
        memberId: Long,
        addressId: Long,
    ) {
        memberClient.deleteAddress(memberId, addressId)
    }

    fun setDefault(
        memberId: Long,
        addressId: Long,
    ): ShippingAddressResponse {
        return memberClient.setDefaultAddress(memberId, addressId).data!!
    }
}
