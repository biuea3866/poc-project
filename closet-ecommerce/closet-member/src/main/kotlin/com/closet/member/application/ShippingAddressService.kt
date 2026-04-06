package com.closet.member.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.member.domain.ShippingAddress
import com.closet.member.domain.repository.ShippingAddressRepository
import com.closet.member.presentation.dto.ShippingAddressRequest
import com.closet.member.presentation.dto.ShippingAddressResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ShippingAddressService(
    private val shippingAddressRepository: ShippingAddressRepository,
) {
    /** 배송지 등록 */
    @Transactional
    fun create(
        memberId: Long,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse {
        // 첫 배송지이면 기본 배송지로 설정
        val existingAddresses = shippingAddressRepository.findByMemberIdAndDeletedAtIsNull(memberId)
        val isDefault = existingAddresses.isEmpty()

        val address =
            ShippingAddress(
                memberId = memberId,
                name = request.name,
                phone = request.phone,
                zipCode = request.zipCode,
                address = request.address,
                detailAddress = request.detailAddress,
                isDefault = isDefault,
            )

        val saved = shippingAddressRepository.save(address)
        return ShippingAddressResponse.from(saved)
    }

    /** 배송지 목록 조회 */
    fun findAll(memberId: Long): List<ShippingAddressResponse> {
        return shippingAddressRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .map { ShippingAddressResponse.from(it) }
    }

    /** 배송지 수정 */
    @Transactional
    fun update(
        memberId: Long,
        addressId: Long,
        request: ShippingAddressRequest,
    ): ShippingAddressResponse {
        val address = findByIdAndMemberId(addressId, memberId)

        address.update(
            name = request.name,
            phone = request.phone,
            zipCode = request.zipCode,
            address = request.address,
            detailAddress = request.detailAddress,
        )

        return ShippingAddressResponse.from(address)
    }

    /** 배송지 삭제 */
    @Transactional
    fun delete(
        memberId: Long,
        addressId: Long,
    ) {
        val address = findByIdAndMemberId(addressId, memberId)
        address.softDelete()
    }

    /** 기본 배송지 설정 */
    @Transactional
    fun setDefault(
        memberId: Long,
        addressId: Long,
    ): ShippingAddressResponse {
        // 기존 기본 배송지 해제
        val currentDefault = shippingAddressRepository.findByMemberIdAndIsDefaultTrueAndDeletedAtIsNull(memberId)
        currentDefault?.unmarkDefault()

        // 새 기본 배송지 설정
        val address = findByIdAndMemberId(addressId, memberId)
        address.markAsDefault()

        return ShippingAddressResponse.from(address)
    }

    private fun findByIdAndMemberId(
        addressId: Long,
        memberId: Long,
    ): ShippingAddress {
        return shippingAddressRepository.findByIdAndMemberIdAndDeletedAtIsNull(addressId, memberId)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "배송지를 찾을 수 없습니다")
    }
}
