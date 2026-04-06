package com.closet.external.carrier

import com.closet.external.domain.MockShipment
import com.closet.external.domain.MockShipmentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.atomic.AtomicLong

@Service
@Transactional(readOnly = true)
class CarrierService(
    private val shipmentRepository: MockShipmentRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val trackingSeq = AtomicLong(1000000000L)

    private val trackingSteps =
        listOf(
            "ACCEPTED" to ("접수" to "택배 접수가 완료되었습니다"),
            "IN_TRANSIT" to ("간선상차" to "발송지 터미널에서 상차되었습니다"),
            "IN_TRANSIT" to ("간선하차" to "도착지 터미널에 하차되었습니다"),
            "OUT_FOR_DELIVERY" to ("배달출발" to "배달원이 배달을 시작했습니다"),
            "DELIVERED" to ("배달완료" to "배달이 완료되었습니다"),
        )

    @Transactional
    fun registerShipment(
        carrier: String,
        prefix: String,
        request: Map<String, Any>,
    ): MockShipment {
        val orderId = request["orderId"]?.toString() ?: throw IllegalArgumentException("orderId는 필수입니다")
        val trackingNumber = "$prefix${trackingSeq.getAndIncrement()}"

        val shipment =
            MockShipment(
                carrier = carrier,
                trackingNumber = trackingNumber,
                orderId = orderId,
                senderName = request["senderName"]?.toString() ?: "Closet 물류센터",
                receiverName = request["receiverName"]?.toString() ?: "고객",
                receiverAddress = request["receiverAddress"]?.toString() ?: "서울시 강남구",
                receiverPhone = request["receiverPhone"]?.toString() ?: "010-0000-0000",
            )
        shipment.addTrackingEvent("ACCEPTED", "택배 접수가 완료되었습니다", "온라인")

        val saved = shipmentRepository.save(shipment)
        log.info("[{}] 택배 접수: trackingNumber={}, orderId={}", carrier, trackingNumber, orderId)
        return saved
    }

    fun getTracking(
        carrier: String,
        trackingNumber: String,
    ): MockShipment? {
        val shipment = shipmentRepository.findByTrackingNumber(trackingNumber).orElse(null) ?: return null
        if (shipment.carrier != carrier) return null
        return shipment
    }

    @Transactional
    fun advanceStatus(trackingNumber: String): MockShipment? {
        val shipment = shipmentRepository.findByTrackingNumber(trackingNumber).orElse(null) ?: return null
        val currentStep = shipment.trackingHistory.size - 1

        if (currentStep < trackingSteps.size - 1) {
            val (status, info) = trackingSteps[currentStep + 1]
            shipment.addTrackingEvent(status, info.second, info.first)
            log.info("[{}] 배송 상태 진행: {} → {}", shipment.carrier, trackingNumber, status)
        }
        return shipment
    }
}
