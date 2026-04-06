package com.closet.member.presentation.dto

import com.closet.member.domain.ShippingAddress
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime

/** 배송지 등록/수정 요청 */
data class ShippingAddressRequest(
    @field:NotBlank(message = "수령인 이름은 필수입니다")
    @field:Size(max = 50, message = "수령인 이름은 50자 이하여야 합니다")
    val name: String,
    @field:NotBlank(message = "수령인 전화번호는 필수입니다")
    @field:Size(max = 20, message = "전화번호는 20자 이하여야 합니다")
    val phone: String,
    @field:NotBlank(message = "우편번호는 필수입니다")
    @field:Size(max = 10, message = "우편번호는 10자 이하여야 합니다")
    val zipCode: String,
    @field:NotBlank(message = "주소는 필수입니다")
    @field:Size(max = 200, message = "주소는 200자 이하여야 합니다")
    val address: String,
    @field:Size(max = 200, message = "상세주소는 200자 이하여야 합니다")
    val detailAddress: String? = null,
)

/** 배송지 응답 */
data class ShippingAddressResponse(
    val id: Long,
    val name: String,
    val phone: String,
    val zipCode: String,
    val address: String,
    val detailAddress: String?,
    val isDefault: Boolean,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(entity: ShippingAddress): ShippingAddressResponse =
            ShippingAddressResponse(
                id = entity.id,
                name = entity.name,
                phone = entity.phone,
                zipCode = entity.zipCode,
                address = entity.address,
                detailAddress = entity.detailAddress,
                isDefault = entity.isDefault,
                createdAt = entity.createdAt,
            )
    }
}
