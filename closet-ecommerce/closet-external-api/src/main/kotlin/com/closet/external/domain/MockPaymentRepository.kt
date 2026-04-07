package com.closet.external.domain

import org.springframework.data.jpa.repository.JpaRepository

interface MockPaymentRepository : JpaRepository<MockPayment, Long> {
    fun findByPaymentKey(paymentKey: String): MockPayment?

    fun findByOrderId(orderId: String): MockPayment?

    fun findByProviderAndStatus(
        provider: String,
        status: String,
    ): List<MockPayment>
}

interface MockShipmentRepository : JpaRepository<MockShipment, Long> {
    fun findByTrackingNumber(trackingNumber: String): MockShipment?

    fun findByOrderId(orderId: String): MockShipment?

    fun findByCarrier(carrier: String): List<MockShipment>

    fun findByCarrierAndStatus(
        carrier: String,
        status: String,
    ): List<MockShipment>
}
