package com.closet.content.domain.entity

import com.closet.common.entity.BaseEntity
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.content.domain.enums.CoordinationGender
import com.closet.content.domain.enums.CoordinationSeason
import com.closet.content.domain.enums.CoordinationStatus
import com.closet.content.domain.enums.CoordinationStyle
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "coordination")
class Coordination(

    @Column(name = "title", nullable = false, length = 200)
    var title: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "thumbnail_url", length = 500)
    var thumbnailUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "style", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var style: CoordinationStyle,

    @Enumerated(EnumType.STRING)
    @Column(name = "season", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var season: CoordinationSeason,

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var gender: CoordinationGender,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: CoordinationStatus = CoordinationStatus.ACTIVE,

    @OneToMany(mappedBy = "coordination", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val products: MutableList<CoordinationProduct> = mutableListOf()

) : BaseEntity() {

    fun activate() {
        status.validateTransitionTo(CoordinationStatus.ACTIVE)
        status = CoordinationStatus.ACTIVE
    }

    fun deactivate() {
        status.validateTransitionTo(CoordinationStatus.INACTIVE)
        status = CoordinationStatus.INACTIVE
    }

    fun addProduct(productId: Long, sortOrder: Int, description: String? = null) {
        val exists = products.any { it.productId == productId }
        if (exists) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 추가된 상품입니다: $productId")
        }
        val product = CoordinationProduct(
            productId = productId,
            sortOrder = sortOrder,
            description = description
        )
        product.coordination = this
        products.add(product)
    }

    fun removeProduct(productId: Long) {
        val removed = products.removeIf { it.productId == productId }
        if (!removed) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "코디 상품을 찾을 수 없습니다: $productId")
        }
    }
}
