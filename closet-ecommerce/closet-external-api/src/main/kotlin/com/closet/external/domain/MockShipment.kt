package com.closet.external.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "mock_shipment")
@EntityListeners(AuditingEntityListener::class)
class MockShipment(
    @Column(name = "carrier", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val carrier: String,
    @Column(name = "tracking_number", nullable = false, length = 50)
    val trackingNumber: String,
    @Column(name = "order_id", nullable = false, length = 100)
    val orderId: String,
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: String = "ACCEPTED",
    @Column(name = "sender_name", nullable = false, length = 50)
    val senderName: String,
    @Column(name = "receiver_name", nullable = false, length = 50)
    val receiverName: String,
    @Column(name = "receiver_address", nullable = false, length = 500)
    val receiverAddress: String,
    @Column(name = "receiver_phone", nullable = false, length = 20)
    val receiverPhone: String,
    @Column(name = "delivered_at", columnDefinition = "DATETIME(6)")
    var deliveredAt: ZonedDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @OneToMany(mappedBy = "shipment", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @OrderBy("trackedAt ASC")
    val trackingHistory: MutableList<MockTrackingHistory> = mutableListOf()

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    fun addTrackingEvent(
        status: String,
        description: String,
        location: String,
    ) {
        this.status = status
        val event =
            MockTrackingHistory(
                shipment = this,
                status = status,
                description = description,
                location = location,
                trackedAt = ZonedDateTime.now(),
            )
        this.trackingHistory.add(event)
        if (status == "DELIVERED") {
            this.deliveredAt = ZonedDateTime.now()
        }
    }
}

@Entity
@Table(name = "mock_tracking_history")
@EntityListeners(AuditingEntityListener::class)
class MockTrackingHistory(
    @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "shipment_id", nullable = false)
    val shipment: MockShipment,
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val status: String,
    @Column(name = "description", nullable = false, length = 200)
    val description: String,
    @Column(name = "location", nullable = false, length = 100)
    val location: String,
    @Column(name = "tracked_at", nullable = false, columnDefinition = "DATETIME(6)")
    val trackedAt: ZonedDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime
}
