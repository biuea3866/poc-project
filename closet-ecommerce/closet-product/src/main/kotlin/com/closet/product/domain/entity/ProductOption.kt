package com.closet.product.domain.entity

import com.closet.common.vo.Money
import com.closet.product.domain.enums.Size
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.EntityListeners
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "product_option")
@EntityListeners(AuditingEntityListener::class)
class ProductOption(

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val size: Size,

    @Column(name = "color_name", nullable = false, length = 50)
    val colorName: String,

    @Column(name = "color_hex", nullable = false, length = 7)
    val colorHex: String,

    @Column(name = "sku_code", nullable = false, unique = true, length = 50)
    val skuCode: String,

    @AttributeOverride(name = "amount", column = Column(name = "additional_price", nullable = false, columnDefinition = "BIGINT"))
    val additionalPrice: Money = Money.ZERO,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: LocalDateTime
}
