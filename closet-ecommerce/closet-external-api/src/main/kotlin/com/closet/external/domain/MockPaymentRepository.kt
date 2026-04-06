package com.closet.external.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MockPaymentRepository : JpaRepository<MockPayment, Long> {
    fun findByPaymentKey(paymentKey: String): Optional<MockPayment>

    fun findByOrderId(orderId: String): Optional<MockPayment>

    fun findByProviderAndStatus(
        provider: String,
        status: String,
    ): List<MockPayment>
}

interface MockShipmentRepository : JpaRepository<MockShipment, Long> {
    fun findByTrackingNumber(trackingNumber: String): Optional<MockShipment>

    fun findByOrderId(orderId: String): Optional<MockShipment>

    fun findByCarrier(carrier: String): List<MockShipment>

    fun findByCarrierAndStatus(
        carrier: String,
        status: String,
    ): List<MockShipment>
}
