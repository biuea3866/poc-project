package com.closet.shipping.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.shipping.domain.Carrier
import com.closet.shipping.domain.Shipment
import com.closet.shipping.domain.ShipmentStatus
import com.closet.shipping.domain.ShipmentStatusHistory
import com.closet.shipping.presentation.dto.CreateShipmentRequest
import com.closet.shipping.presentation.dto.ShipmentResponse
import com.closet.shipping.repository.ShipmentRepository
import com.closet.shipping.repository.ShipmentStatusHistoryRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class ShipmentService(
    private val shipmentRepository: ShipmentRepository,
    private val shipmentStatusHistoryRepository: ShipmentStatusHistoryRepository,
) {

    @Transactional
    fun createShipment(request: CreateShipmentRequest): ShipmentResponse {
        val shipment = Shipment.create(
            orderId = request.orderId,
            sellerId = request.sellerId,
            receiverName = request.receiverName,
            receiverPhone = request.receiverPhone,
            address = request.address,
        )

        val savedShipment = shipmentRepository.save(shipment)

        shipmentStatusHistoryRepository.save(
            ShipmentStatusHistory.create(
                shipmentId = savedShipment.id,
                fromStatus = null,
                toStatus = savedShipment.status,
            )
        )

        // 송장 정보가 함께 들어온 경우 바로 등록
        if (request.carrier != null && request.trackingNumber != null) {
            val previousStatus = savedShipment.status
            savedShipment.registerTracking(request.carrier, request.trackingNumber)

            shipmentStatusHistoryRepository.save(
                ShipmentStatusHistory.create(
                    shipmentId = savedShipment.id,
                    fromStatus = previousStatus,
                    toStatus = savedShipment.status,
                )
            )
        }

        logger.info { "배송 생성 완료: shipmentId=${savedShipment.id}, orderId=${savedShipment.orderId}" }
        return ShipmentResponse.from(savedShipment)
    }

    @Transactional
    fun registerTracking(id: Long, carrier: Carrier, trackingNumber: String): ShipmentResponse {
        val shipment = findShipmentById(id)

        val previousStatus = shipment.status
        shipment.registerTracking(carrier, trackingNumber)

        shipmentStatusHistoryRepository.save(
            ShipmentStatusHistory.create(
                shipmentId = shipment.id,
                fromStatus = previousStatus,
                toStatus = shipment.status,
            )
        )

        logger.info { "송장 등록 완료: shipmentId=${shipment.id}, carrier=${carrier.name}, trackingNumber=$trackingNumber" }
        return ShipmentResponse.from(shipment)
    }

    @Transactional
    fun updateStatus(id: Long, newStatus: ShipmentStatus): ShipmentResponse {
        val shipment = findShipmentById(id)

        val previousStatus = shipment.status
        if (newStatus == ShipmentStatus.DELIVERED) {
            shipment.completeDelivery()
        } else {
            shipment.updateStatus(newStatus)
        }

        shipmentStatusHistoryRepository.save(
            ShipmentStatusHistory.create(
                shipmentId = shipment.id,
                fromStatus = previousStatus,
                toStatus = shipment.status,
            )
        )

        logger.info { "배송 상태 변경: shipmentId=${shipment.id}, ${previousStatus.name} -> ${shipment.status.name}" }
        return ShipmentResponse.from(shipment)
    }

    fun findByOrderId(orderId: Long): List<ShipmentResponse> {
        return shipmentRepository.findByOrderId(orderId)
            .map { ShipmentResponse.from(it) }
    }

    private fun findShipmentById(id: Long): Shipment {
        return shipmentRepository.findById(id).orElseThrow {
            BusinessException(ErrorCode.ENTITY_NOT_FOUND, "배송 정보를 찾을 수 없습니다. id=$id")
        }
    }
}
