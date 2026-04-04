package com.closet.shipping.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.common.vo.Money
import com.closet.shipping.domain.ReturnRequest
import com.closet.shipping.domain.ReturnRequestRepository
import com.closet.shipping.domain.ReturnStatus
import com.closet.shipping.domain.ShippingFeePolicyRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class ReturnService(
    private val returnRequestRepository: ReturnRequestRepository,
    private val shippingFeePolicyRepository: ShippingFeePolicyRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
) {

    /**
     * 반품 신청 (BUYER).
     * PD-17: 반품 배송비 공제 후 환불
     * PD-11: 사유별 배송비 부담 매핑
     */
    @Transactional
    fun createReturnRequest(memberId: Long, sellerId: Long, request: CreateReturnRequest): ReturnRequestResponse {
        // 배송비 정책 조회
        val policy = shippingFeePolicyRepository.findByTypeAndReasonAndIsActiveTrue("RETURN", request.reason.name)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "반품 배송비 정책을 찾을 수 없습니다: reason=${request.reason}") }

        val shippingFee = policy.fee
        val shippingFeePayer = policy.payer

        // 환불금액 = 결제금액 - 반품배송비 (BUYER 부담시)
        val paymentAmount = Money.of(request.paymentAmount)
        val refundAmount = if (shippingFeePayer == "BUYER") {
            paymentAmount - shippingFee
        } else {
            paymentAmount
        }

        val returnRequest = ReturnRequest.create(
            orderId = request.orderId,
            orderItemId = request.orderItemId,
            memberId = memberId,
            sellerId = sellerId,
            productOptionId = request.productOptionId,
            quantity = request.quantity,
            reason = request.reason,
            reasonDetail = request.reasonDetail,
            shippingFee = shippingFee,
            shippingFeePayer = shippingFeePayer,
            refundAmount = refundAmount,
        )

        val saved = returnRequestRepository.save(returnRequest)
        logger.info { "반품 신청 완료: id=${saved.id}, orderId=${request.orderId}, reason=${request.reason}, refundAmount=$refundAmount" }
        return ReturnRequestResponse.from(saved)
    }

    /**
     * 반품 상세 조회.
     */
    fun findById(id: Long): ReturnRequestResponse {
        val returnRequest = returnRequestRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "반품 요청을 찾을 수 없습니다: id=$id") }
        return ReturnRequestResponse.from(returnRequest)
    }

    /**
     * 주문별 반품 조회.
     */
    fun findByOrderId(orderId: Long): List<ReturnRequestResponse> {
        return returnRequestRepository.findByOrderId(orderId)
            .map { ReturnRequestResponse.from(it) }
    }

    /**
     * 수거 예약 (SELLER).
     */
    @Transactional
    fun schedulePickup(id: Long, request: SchedulePickupRequest): ReturnRequestResponse {
        val returnRequest = getReturnRequestOrThrow(id)
        returnRequest.schedulePickup(request.pickupTrackingNumber)
        logger.info { "수거 예약 완료: returnId=$id" }
        return ReturnRequestResponse.from(returnRequest)
    }

    /**
     * 수거 완료 (SELLER).
     */
    @Transactional
    fun completePickup(id: Long): ReturnRequestResponse {
        val returnRequest = getReturnRequestOrThrow(id)
        returnRequest.completePickup()
        logger.info { "수거 완료: returnId=$id" }
        return ReturnRequestResponse.from(returnRequest)
    }

    /**
     * 검수 시작 (SELLER).
     */
    @Transactional
    fun startInspection(id: Long): ReturnRequestResponse {
        val returnRequest = getReturnRequestOrThrow(id)
        returnRequest.startInspection()
        logger.info { "검수 시작: returnId=$id" }
        return ReturnRequestResponse.from(returnRequest)
    }

    /**
     * 반품 승인 (SELLER).
     * APPROVED 시: return.approved 이벤트 발행 -> payment 부분환불 + inventory 재고 복구
     */
    @Transactional
    fun approve(id: Long): ReturnRequestResponse {
        val returnRequest = getReturnRequestOrThrow(id)
        returnRequest.approve()

        // return.approved 이벤트 발행 (inventory 재고 복구 + payment 부분 환불)
        publishReturnApprovedEvent(returnRequest)

        logger.info { "반품 승인 완료: returnId=$id, refundAmount=${returnRequest.refundAmount}" }
        return ReturnRequestResponse.from(returnRequest)
    }

    /**
     * 반품 거절 (SELLER).
     */
    @Transactional
    fun reject(id: Long, request: RejectReturnRequest): ReturnRequestResponse {
        val returnRequest = getReturnRequestOrThrow(id)
        returnRequest.reject(request.reason)
        logger.info { "반품 거절: returnId=$id, reason=${request.reason}" }
        return ReturnRequestResponse.from(returnRequest)
    }

    /**
     * 반품 완료 처리 (환불 완료 후).
     */
    @Transactional
    fun complete(id: Long): ReturnRequestResponse {
        val returnRequest = getReturnRequestOrThrow(id)
        returnRequest.complete()
        logger.info { "반품 완료: returnId=$id" }
        return ReturnRequestResponse.from(returnRequest)
    }

    /**
     * 진행 중인 반품/교환이 있는지 확인 (자동 구매확정 배제용).
     */
    fun hasActiveReturnRequest(orderId: Long): Boolean {
        val terminalStatuses = listOf(ReturnStatus.COMPLETED, ReturnStatus.REJECTED)
        return returnRequestRepository.findByOrderIdAndStatusNotIn(orderId, terminalStatuses).isNotEmpty()
    }

    private fun getReturnRequestOrThrow(id: Long): ReturnRequest {
        return returnRequestRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "반품 요청을 찾을 수 없습니다: id=$id") }
    }

    private fun publishReturnApprovedEvent(returnRequest: ReturnRequest) {
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "orderId" to returnRequest.orderId,
                "returnRequestId" to returnRequest.id,
                "orderItemId" to returnRequest.orderItemId,
                "productOptionId" to returnRequest.productOptionId,
                "quantity" to returnRequest.quantity,
                "refundAmount" to returnRequest.refundAmount.amount.toLong(),
                "shippingFee" to returnRequest.shippingFee.amount.toLong(),
                "timestamp" to LocalDateTime.now().toString(),
                "items" to listOf(
                    mapOf(
                        "productOptionId" to returnRequest.productOptionId,
                        "quantity" to returnRequest.quantity,
                    )
                ),
            )
        )

        outboxEventPublisher.publish(
            aggregateType = "ReturnRequest",
            aggregateId = returnRequest.id.toString(),
            eventType = "ReturnApproved",
            topic = "return.approved",
            partitionKey = returnRequest.orderId.toString(),
            payload = payload,
        )

        logger.info { "return.approved 이벤트 발행: returnId=${returnRequest.id}, orderId=${returnRequest.orderId}" }
    }
}
