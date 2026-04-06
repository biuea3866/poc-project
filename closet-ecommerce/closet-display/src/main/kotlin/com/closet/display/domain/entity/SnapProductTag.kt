package com.closet.display.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import java.time.ZonedDateTime

@Entity
@Table(name = "snap_product_tag")
class SnapProductTag(
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "position_x", nullable = false)
    val positionX: Double,
    @Column(name = "position_y", nullable = false)
    val positionY: Double,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snap_id", nullable = false)
    lateinit var snap: Snap

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    var createdAt: ZonedDateTime = ZonedDateTime.now()
}
