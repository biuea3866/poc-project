package com.closet.shipping.application

import com.closet.shipping.domain.ReturnReason
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 송장 등록 요청.
 * 판매자가 택배사를 선택하여 송장을 등록한다.
 * PD-07: 시스템 자동 채번 + 수동 입력 병행
 */
data class RegisterShipmentRequest(
    @field:NotNull val orderId: Long,
    @field:NotBlank val carrier: String,
    val trackingNumber: String? = null,
)

/**
 * 배송 준비 정보 사전 저장 (order.created 이벤트 수신 시).
 */
data class PrepareShipmentRequest(
    val orderId: Long,
    val sellerId: Long,
    val memberId: Long,
    val receiverName: String,
    val receiverPhone: String,
    val zipCode: String,
    val address: String,
    val detailAddress: String,
)

/**
 * 반품 신청 요청.
 */
data class CreateReturnRequest(
    @field:NotNull val orderId: Long,
    @field:NotNull val orderItemId: Long,
    @field:NotNull val productOptionId: Long,
    @field:Positive val quantity: Int,
    @field:NotNull val reason: ReturnReason,
    val reasonDetail: String? = null,
    @field:NotNull val paymentAmount: Long,
)

/**
 * 수거 예약 요청.
 */
data class SchedulePickupRequest(
    val pickupTrackingNumber: String? = null,
)

/**
 * 반품 거절 요청.
 */
data class RejectReturnRequest(
    val reason: String? = null,
)

// === Exchange Requests (CP-28) ===

/**
 * 교환 신청 요청.
 * PD-14: 동일 가격 옵션만 교환.
 */
data class CreateExchangeRequest(
    @field:NotNull val orderId: Long,
    @field:NotNull val orderItemId: Long,
    @field:NotNull val originalProductOptionId: Long,
    @field:NotNull val newProductOptionId: Long,
    @field:Positive val quantity: Int,
    @field:NotNull val reason: com.closet.shipping.domain.ReturnReason,
    val reasonDetail: String? = null,
)
