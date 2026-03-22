package com.closet.product.domain.entity

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brand")
class Brand(

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = null,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT(1)")
    var status: Boolean = true

) : BaseEntity()
