package com.closet.shipping.application
import com.closet.common.event.ClosetTopics
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.shipping.application.carrier.CarrierAdapterFactory
import com.closet.shipping.application.carrier.ShipmentRegistrationRequest
import com.closet.shipping.domain.Shipment
import com.closet.shipping.domain.ShipmentRepository
import com.closet.shipping.domain.ShippingStatus
import com.closet.shipping.domain.ShippingTrackingLog
import com.closet.shipping.domain.ShippingTrackingLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class ShippingService(
    private val shipmentRepository: ShipmentRepository,
    private val trackingLogRepository: ShippingTrackingLogRepository,
    private val carrierAdapterFactory: CarrierAdapterFactory,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val TRACKING_CACHE_PREFIX = "shipping:tracking:"
        private val TRACKING_CACHE_TTL = Duration.ofMinutes(5) // PD-41
    }

    /**
     * 배송 준비 정보 사전 저장 (order.created 이벤트 수신 시).
     */
    @Transactional
    fun prepareShipment(request: PrepareShipmentRequest): ShipmentResponse {
        val existing = shipmentRepository.findByOrderId(request.orderId)
        if (existing != null) {
            logger.info { "이미 배송 정보가 존재합니다: orderId=${request.orderId}" }
            return ShipmentResponse.from(existing)
        }

        val shipment =
            Shipment.create(
                orderId = request.orderId,
                sellerId = request.sellerId,
                memberId = request.memberId,
                receiverName = request.receiverName,
                receiverPhone = request.receiverPhone,
                zipCode = request.zipCode,
                address = request.address,
                detailAddress = request.detailAddress,
            )
        val saved = shipmentRepository.save(shipment)
        logger.info { "배송 준비 정보 저장 완료: orderId=${request.orderId}, shipmentId=${saved.id}" }
        return ShipmentResponse.from(saved)
    }

    /**
     * 송장 등록 (배송 시작).
     * PD-07: 시스템 자동 채번 + 수동 입력 병행
     */
    @Transactional
    fun registerShipment(request: RegisterShipmentRequest): ShipmentResponse {
        val shipment =
            shipmentRepository.findByOrderId(request.orderId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "배송 정보를 찾을 수 없습니다: orderId=${request.orderId}")

        if (shipment.trackingNumber != null) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "이미 송장이 등록된 배송입니다: orderId=${request.orderId}")
        }

        val adapter = carrierAdapterFactory.getAdapter(request.carrier)

        if (request.trackingNumber != null) {
            // 수동 입력 (PD-08 형식 검증)
            require(adapter.validateTrackingNumber(request.trackingNumber)) {
                "송장번호 형식이 올바르지 않습니다: ${request.trackingNumber}"
            }
            shipment.registerTracking(request.carrier, request.trackingNumber)
        } else {
            // 시스템 자동 채번 (택배사 API 호출)
            val result =
                adapter.registerShipment(
                    ShipmentRegistrationRequest(
                        orderId = shipment.orderId,
                        senderName = "Closet 물류센터",
                        receiverName = shipment.receiverName,
                        receiverPhone = shipment.receiverPhone,
                        receiverAddress = "${shipment.address} ${shipment.detailAddress}",
                    ),
                )
            shipment.registerTracking(result.carrier, result.trackingNumber)
        }

        // 초기 추적 이력 저장
        trackingLogRepository.save(
            ShippingTrackingLog.create(
                shippingId = shipment.id,
                carrierStatus = "ACCEPTED",
                mappedStatus = ShippingStatus.READY,
                location = "택배사 접수",
                description = "송장이 등록되었습니다",
            ),
        )

        // shipping.status.changed 이벤트 발행
        publishStatusChangedEvent(shipment, null, ShippingStatus.READY)

        // 주문 상태 PAID->SHIPPING 변경 이벤트 (ORDER 토픽)
        publishOrderShippingEvent(shipment)

        logger.info { "송장 등록 완료: orderId=${shipment.orderId}, carrier=${shipment.carrier}, trackingNumber=${shipment.trackingNumber}" }
        return ShipmentResponse.from(shipment)
    }

    /**
     * 배송 상세 조회.
     */
    fun findById(id: Long): ShipmentResponse {
        val shipment =
            shipmentRepository.findByIdOrNull(id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "배송 정보를 찾을 수 없습니다: id=$id")
        return ShipmentResponse.from(shipment)
    }

    /**
     * orderId 기반 배송 조회 (PD-44).
     */
    fun findByOrderId(orderId: Long): ShipmentResponse {
        val shipment =
            shipmentRepository.findByOrderId(orderId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "배송 정보를 찾을 수 없습니다: orderId=$orderId")
        return ShipmentResponse.from(shipment)
    }

    /**
     * 배송 추적 조회 (Redis 캐시 5분 TTL, PD-41).
     * 캐시 히트 시 캐시 반환. 미스 시 택배사 API 호출.
     * API 장애 시 기존 캐시 또는 DB 데이터를 반환한다.
     */
    @Transactional
    fun getTrackingLogs(shipmentId: Long): List<TrackingLogResponse> {
        val cacheKey = "$TRACKING_CACHE_PREFIX$shipmentId"
        val cached = redisTemplate.opsForValue().get(cacheKey)

        if (cached != null) {
            logger.debug { "배송 추적 캐시 히트: shipmentId=$shipmentId" }
            val type = objectMapper.typeFactory.constructCollectionType(List::class.java, TrackingLogResponse::class.java)
            return objectMapper.readValue(cached, type)
        }

        val shipment =
            shipmentRepository.findByIdOrNull(shipmentId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "배송 정보를 찾을 수 없습니다: id=$shipmentId")

        // 택배사 API로 최신 상태 조회
        if (shipment.carrier != null && shipment.trackingNumber != null) {
            try {
                val adapter = carrierAdapterFactory.getAdapter(shipment.carrier!!)
                val trackingResponse = adapter.trackShipment(shipment.trackingNumber!!)

                // 새로운 추적 이벤트 저장
                val existingLogs = trackingLogRepository.findByShippingIdOrderByTrackedAtAsc(shipmentId)
                for (event in trackingResponse.events) {
                    val mappedStatus =
                        try {
                            ShippingStatus.fromCarrierStatus(event.status)
                        } catch (_: IllegalArgumentException) {
                            continue
                        }

                    val alreadyExists = existingLogs.any { it.carrierStatus == event.status && it.location == event.location }
                    if (!alreadyExists) {
                        trackingLogRepository.save(
                            ShippingTrackingLog.create(
                                shippingId = shipmentId,
                                carrierStatus = event.status,
                                mappedStatus = mappedStatus,
                                location = event.location,
                                description = event.description,
                                trackedAt = ZonedDateTime.now(),
                            ),
                        )
                    }
                }

                // 배송 상태 업데이트
                val latestStatus =
                    try {
                        ShippingStatus.fromCarrierStatus(trackingResponse.status)
                    } catch (_: IllegalArgumentException) {
                        null
                    }

                if (latestStatus != null && latestStatus != shipment.status && shipment.status.canTransitionTo(latestStatus)) {
                    val previousStatus = shipment.status
                    shipment.updateStatus(latestStatus)
                    shipmentRepository.save(shipment)
                    publishStatusChangedEvent(shipment, previousStatus, latestStatus)
                }
            } catch (e: Exception) {
                logger.warn(e) { "택배사 API 조회 실패, DB 데이터를 반환합니다: shipmentId=$shipmentId" }
                // API 장애 시 DB에 있는 기존 데이터를 반환
                return trackingLogRepository.findByShippingIdOrderByTrackedAtAsc(shipmentId)
                    .map { TrackingLogResponse.from(it) }
            }
        }

        val logs =
            trackingLogRepository.findByShippingIdOrderByTrackedAtAsc(shipmentId)
                .map { TrackingLogResponse.from(it) }

        // 캐시 저장
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(logs), TRACKING_CACHE_TTL)

        return logs
    }

    /**
     * 배송 추적 폴링 스케줄러에서 호출.
     * READY, IN_TRANSIT 상태 배송 건의 택배사 API를 폴링하여 상태를 갱신한다.
     */
    @Transactional
    fun pollTrackingStatus() {
        val shipments =
            shipmentRepository.findByStatusIn(
                listOf(ShippingStatus.READY, ShippingStatus.IN_TRANSIT),
            )

        for (shipment in shipments) {
            if (shipment.carrier == null || shipment.trackingNumber == null) continue

            try {
                val adapter = carrierAdapterFactory.getAdapter(shipment.carrier!!)
                val trackingResponse = adapter.trackShipment(shipment.trackingNumber!!)

                val latestStatus =
                    try {
                        ShippingStatus.fromCarrierStatus(trackingResponse.status)
                    } catch (_: IllegalArgumentException) {
                        continue
                    }

                if (latestStatus != shipment.status && shipment.status.canTransitionTo(latestStatus)) {
                    val previousStatus = shipment.status
                    shipment.updateStatus(latestStatus)
                    shipmentRepository.save(shipment)

                    // 추적 이력 저장
                    for (event in trackingResponse.events) {
                        val mappedStatus =
                            try {
                                ShippingStatus.fromCarrierStatus(event.status)
                            } catch (_: IllegalArgumentException) {
                                continue
                            }

                        val existingLogs = trackingLogRepository.findByShippingIdOrderByTrackedAtAsc(shipment.id)
                        val alreadyExists = existingLogs.any { it.carrierStatus == event.status && it.location == event.location }
                        if (!alreadyExists) {
                            trackingLogRepository.save(
                                ShippingTrackingLog.create(
                                    shippingId = shipment.id,
                                    carrierStatus = event.status,
                                    mappedStatus = mappedStatus,
                                    location = event.location,
                                    description = event.description,
                                    trackedAt = ZonedDateTime.now(),
                                ),
                            )
                        }
                    }

                    // Redis 캐시 갱신
                    val cacheKey = "$TRACKING_CACHE_PREFIX${shipment.id}"
                    redisTemplate.delete(cacheKey)

                    publishStatusChangedEvent(shipment, previousStatus, latestStatus)
                    logger.info { "배송 상태 폴링 업데이트: shipmentId=${shipment.id}, $previousStatus -> $latestStatus" }
                }
            } catch (e: Exception) {
                logger.warn(e) { "배송 추적 폴링 실패: shipmentId=${shipment.id}" }
            }
        }
    }

    /**
     * 수동 구매확정 (BUYER).
     * 배송 완료 후 BUYER가 직접 구매확정을 한다.
     */
    @Transactional
    fun confirmOrder(orderId: Long): ShipmentResponse {
        val shipment =
            shipmentRepository.findByOrderId(orderId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "배송 정보를 찾을 수 없습니다: orderId=$orderId")

        if (shipment.status != ShippingStatus.DELIVERED) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "배송 완료 상태에서만 구매확정할 수 있습니다: status=${shipment.status}")
        }

        // order.confirmed Kafka 이벤트 발행
        publishOrderConfirmedEvent(shipment)

        logger.info { "수동 구매확정 완료: orderId=$orderId" }
        return ShipmentResponse.from(shipment)
    }

    /**
     * 자동 구매확정 처리 (Scheduler 호출).
     * 배송완료 7일(168시간) 경과 건을 자동 구매확정 처리한다.
     * 반품/교환 진행 중인 건은 제외한다.
     */
    @Transactional
    fun autoConfirmOrders(
        returnService: ReturnService,
        exchangeService: ExchangeService,
    ) {
        val cutoff = ZonedDateTime.now().minusHours(168)
        val candidates = shipmentRepository.findByStatusAndDeliveredAtLessThanEqual(ShippingStatus.DELIVERED, cutoff)

        var confirmedCount = 0
        var skippedCount = 0

        for (shipment in candidates) {
            try {
                // 반품/교환 진행 중인 건 제외
                if (returnService.hasActiveReturnRequest(shipment.orderId) ||
                    exchangeService.hasActiveExchangeRequest(shipment.orderId)
                ) {
                    skippedCount++
                    logger.info { "자동 구매확정 스킵 (반품/교환 진행 중): orderId=${shipment.orderId}" }
                    continue
                }

                // order.confirmed Kafka 이벤트 발행
                publishOrderConfirmedEvent(shipment)

                confirmedCount++
                logger.info { "자동 구매확정 처리: orderId=${shipment.orderId}" }
            } catch (e: Exception) {
                logger.error(e) { "자동 구매확정 실패: orderId=${shipment.orderId}" }
            }
        }

        logger.info { "자동 구매확정 배치 완료: 대상=${candidates.size}, 확정=$confirmedCount, 스킵=$skippedCount" }
    }

    private fun publishOrderShippingEvent(shipment: Shipment) {
        val eventId = "order-shipping-${shipment.orderId}-${System.currentTimeMillis()}"
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "eventId" to eventId,
                    "eventType" to "OrderShippingStarted",
                    "orderId" to shipment.orderId,
                    "shippingId" to shipment.id,
                    "carrier" to shipment.carrier,
                    "trackingNumber" to shipment.trackingNumber,
                    "timestamp" to ZonedDateTime.now().toString(),
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "Shipment",
            aggregateId = shipment.id.toString(),
            eventType = "OrderShippingStarted",
            topic = ClosetTopics.ORDER,
            partitionKey = shipment.orderId.toString(),
            payload = payload,
        )
    }

    private fun publishOrderConfirmedEvent(shipment: Shipment) {
        val eventId = "order-confirmed-${shipment.orderId}-${System.currentTimeMillis()}"
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "eventId" to eventId,
                    "eventType" to "OrderConfirmed",
                    "orderId" to shipment.orderId,
                    "memberId" to shipment.memberId,
                    "shippingId" to shipment.id,
                    "timestamp" to ZonedDateTime.now().toString(),
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "Shipment",
            aggregateId = shipment.id.toString(),
            eventType = "OrderConfirmed",
            topic = ClosetTopics.ORDER,
            partitionKey = shipment.orderId.toString(),
            payload = payload,
        )
    }

    private fun publishStatusChangedEvent(
        shipment: Shipment,
        fromStatus: ShippingStatus?,
        toStatus: ShippingStatus,
    ) {
        val eventId = "shipping-status-${shipment.orderId}-${toStatus.name}-${System.currentTimeMillis()}"
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "eventId" to eventId,
                    "orderId" to shipment.orderId,
                    "shippingId" to shipment.id,
                    "fromStatus" to fromStatus?.name,
                    "toStatus" to toStatus.name,
                    "shippingStatus" to toStatus.name,
                    "carrier" to shipment.carrier,
                    "trackingNumber" to shipment.trackingNumber,
                    "timestamp" to ZonedDateTime.now().toString(),
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "Shipment",
            aggregateId = shipment.id.toString(),
            eventType = "ShippingStatusChanged",
            topic = ClosetTopics.SHIPPING,
            partitionKey = shipment.orderId.toString(),
            payload = payload,
        )
    }
}
