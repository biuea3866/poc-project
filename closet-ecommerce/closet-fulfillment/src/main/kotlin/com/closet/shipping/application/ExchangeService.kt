package com.closet.shipping.application
import com.closet.common.event.ClosetTopics
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.shipping.domain.ExchangeRequest
import com.closet.shipping.domain.ExchangeRequestRepository
import com.closet.shipping.domain.ExchangeStatus
import com.closet.shipping.domain.ShipmentRepository
import com.closet.shipping.domain.ShippingFeePolicyRepository
import com.closet.shipping.domain.ShippingStatus
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

/**
 * 교환 서비스 (CP-28).
 *
 * PD-14: 동일 상품 = 동일 가격 옵션만 교환 허용.
 * 교환 시 새 옵션 재고 예약 + 수거 완료 시 기존 옵션 재고 복구.
 */
@Service
@Transactional(readOnly = true)
class ExchangeService(
    private val exchangeRequestRepository: ExchangeRequestRepository,
    private val shipmentRepository: ShipmentRepository,
    private val shippingFeePolicyRepository: ShippingFeePolicyRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val EXCHANGE_ELIGIBLE_DAYS = 7L
    }

    /**
     * 교환 신청 (BUYER).
     */
    @Transactional
    fun createExchangeRequest(
        memberId: Long,
        sellerId: Long,
        request: CreateExchangeRequest,
    ): ExchangeRequestResponse {
        // 배송 완료 7일 이내인지 검증
        val shipment =
            shipmentRepository.findByOrderId(request.orderId)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "배송 정보를 찾을 수 없습니다: orderId=${request.orderId}") }

        if (shipment.status != ShippingStatus.DELIVERED) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "배송 완료 상태에서만 교환 신청이 가능합니다: status=${shipment.status}")
        }

        val deliveredAt =
            shipment.deliveredAt
                ?: throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "배송 완료 일시가 기록되지 않았습니다: orderId=${request.orderId}")

        val exchangeDeadline = deliveredAt.plusDays(EXCHANGE_ELIGIBLE_DAYS)
        if (ZonedDateTime.now().isAfter(exchangeDeadline)) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "교환 신청 기한이 경과했습니다 (배송 완료 후 ${EXCHANGE_ELIGIBLE_DAYS}일 이내)")
        }

        // 동일 상품 다른 옵션으로만 교환 (원본과 새 옵션이 같으면 거절)
        if (request.originalProductOptionId == request.newProductOptionId) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "동일 옵션으로 교환할 수 없습니다")
        }

        // 배송비 정책 조회
        val policy =
            shippingFeePolicyRepository.findByTypeAndReasonAndIsActiveTrue("EXCHANGE", request.reason.name)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "교환 배송비 정책을 찾을 수 없습니다: reason=${request.reason}") }

        val shippingFee = policy.fee
        val shippingFeePayer = policy.payer

        val exchangeRequest =
            ExchangeRequest.create(
                orderId = request.orderId,
                orderItemId = request.orderItemId,
                memberId = memberId,
                sellerId = sellerId,
                originalProductOptionId = request.originalProductOptionId,
                newProductOptionId = request.newProductOptionId,
                quantity = request.quantity,
                reason = request.reason,
                reasonDetail = request.reasonDetail,
                shippingFee = shippingFee,
                shippingFeePayer = shippingFeePayer,
            )

        val saved = exchangeRequestRepository.save(exchangeRequest)

        // exchange.requested 이벤트 발행 (inventory에서 새 옵션 재고 예약)
        publishExchangeRequestedEvent(saved)

        logger.info {
            "교환 신청 완료: id=${saved.id}, orderId=${request.orderId}"
        }
        return ExchangeRequestResponse.from(saved)
    }

    /**
     * 교환 상세 조회.
     */
    fun findById(id: Long): ExchangeRequestResponse {
        val exchangeRequest =
            exchangeRequestRepository.findById(id)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "교환 요청을 찾을 수 없습니다: id=$id") }
        return ExchangeRequestResponse.from(exchangeRequest)
    }

    /**
     * 주문별 교환 조회.
     */
    fun findByOrderId(orderId: Long): List<ExchangeRequestResponse> {
        return exchangeRequestRepository.findByOrderId(orderId)
            .map { ExchangeRequestResponse.from(it) }
    }

    /**
     * 수거 예약 (SELLER).
     */
    @Transactional
    fun schedulePickup(
        id: Long,
        pickupTrackingNumber: String?,
    ): ExchangeRequestResponse {
        val exchangeRequest = getExchangeRequestOrThrow(id)
        exchangeRequest.schedulePickup(pickupTrackingNumber)
        logger.info { "교환 수거 예약 완료: exchangeId=$id" }
        return ExchangeRequestResponse.from(exchangeRequest)
    }

    /**
     * 수거 완료 (SELLER).
     * 수거 완료 시 기존 옵션 재고 복구 이벤트 발행.
     */
    @Transactional
    fun completePickup(id: Long): ExchangeRequestResponse {
        val exchangeRequest = getExchangeRequestOrThrow(id)
        exchangeRequest.completePickup()

        // exchange.pickup.completed 이벤트 발행 (기존 옵션 재고 복구)
        publishExchangePickupCompletedEvent(exchangeRequest)

        logger.info { "교환 수거 완료: exchangeId=$id" }
        return ExchangeRequestResponse.from(exchangeRequest)
    }

    /**
     * 재배송 시작 (SELLER).
     */
    @Transactional
    fun startReshipping(
        id: Long,
        newTrackingNumber: String?,
    ): ExchangeRequestResponse {
        val exchangeRequest = getExchangeRequestOrThrow(id)
        exchangeRequest.startReshipping(newTrackingNumber)
        logger.info { "교환 재배송 시작: exchangeId=$id, trackingNumber=$newTrackingNumber" }
        return ExchangeRequestResponse.from(exchangeRequest)
    }

    /**
     * 교환 완료.
     */
    @Transactional
    fun complete(id: Long): ExchangeRequestResponse {
        val exchangeRequest = getExchangeRequestOrThrow(id)
        exchangeRequest.complete()
        logger.info { "교환 완료: exchangeId=$id" }
        return ExchangeRequestResponse.from(exchangeRequest)
    }

    /**
     * 교환 거절 (SELLER).
     * 거절 시 새 옵션 재고 해제 이벤트 발행.
     */
    @Transactional
    fun reject(id: Long): ExchangeRequestResponse {
        val exchangeRequest = getExchangeRequestOrThrow(id)
        exchangeRequest.reject()

        // exchange.rejected 이벤트 발행 (새 옵션 재고 해제)
        publishExchangeRejectedEvent(exchangeRequest)

        logger.info { "교환 거절: exchangeId=$id" }
        return ExchangeRequestResponse.from(exchangeRequest)
    }

    /**
     * 진행 중인 교환이 있는지 확인 (자동 구매확정 배제용).
     */
    fun hasActiveExchangeRequest(orderId: Long): Boolean {
        val terminalStatuses = listOf(ExchangeStatus.COMPLETED, ExchangeStatus.REJECTED)
        return exchangeRequestRepository.findByOrderIdAndStatusNotIn(orderId, terminalStatuses).isNotEmpty()
    }

    private fun getExchangeRequestOrThrow(id: Long): ExchangeRequest {
        return exchangeRequestRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "교환 요청을 찾을 수 없습니다: id=$id") }
    }

    private fun publishExchangeRequestedEvent(exchangeRequest: ExchangeRequest) {
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "exchangeRequestId" to exchangeRequest.id,
                    "orderId" to exchangeRequest.orderId,
                    "newProductOptionId" to exchangeRequest.newProductOptionId,
                    "quantity" to exchangeRequest.quantity,
                    "timestamp" to ZonedDateTime.now().toString(),
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "ExchangeRequest",
            aggregateId = exchangeRequest.id.toString(),
            eventType = "ExchangeRequested",
            topic = ClosetTopics.SHIPPING,
            partitionKey = exchangeRequest.orderId.toString(),
            payload = payload,
        )
    }

    private fun publishExchangePickupCompletedEvent(exchangeRequest: ExchangeRequest) {
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "exchangeRequestId" to exchangeRequest.id,
                    "orderId" to exchangeRequest.orderId,
                    "originalProductOptionId" to exchangeRequest.originalProductOptionId,
                    "quantity" to exchangeRequest.quantity,
                    "timestamp" to ZonedDateTime.now().toString(),
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "ExchangeRequest",
            aggregateId = exchangeRequest.id.toString(),
            eventType = "ExchangePickupCompleted",
            topic = ClosetTopics.SHIPPING,
            partitionKey = exchangeRequest.orderId.toString(),
            payload = payload,
        )
    }

    private fun publishExchangeRejectedEvent(exchangeRequest: ExchangeRequest) {
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "exchangeRequestId" to exchangeRequest.id,
                    "orderId" to exchangeRequest.orderId,
                    "newProductOptionId" to exchangeRequest.newProductOptionId,
                    "quantity" to exchangeRequest.quantity,
                    "timestamp" to ZonedDateTime.now().toString(),
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "ExchangeRequest",
            aggregateId = exchangeRequest.id.toString(),
            eventType = "ExchangeRejected",
            topic = ClosetTopics.SHIPPING,
            partitionKey = exchangeRequest.orderId.toString(),
            payload = payload,
        )
    }
}
