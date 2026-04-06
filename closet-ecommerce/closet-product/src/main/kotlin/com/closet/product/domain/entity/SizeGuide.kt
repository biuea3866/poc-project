package com.closet.product.domain.entity

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
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "size_guide")
class SizeGuide(
    @Column(name = "size", nullable = false, length = 30)
    val size: String,
    @Column(name = "shoulder_width", precision = 6, scale = 1)
    val shoulderWidth: BigDecimal? = null,
    @Column(name = "chest_width", precision = 6, scale = 1)
    val chestWidth: BigDecimal? = null,
    @Column(name = "total_length", precision = 6, scale = 1)
    val totalLength: BigDecimal? = null,
    @Column(name = "sleeve_length", precision = 6, scale = 1)
    val sleeveLength: BigDecimal? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime
}
