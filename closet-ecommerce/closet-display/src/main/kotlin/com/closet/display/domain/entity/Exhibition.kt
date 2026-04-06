package com.closet.display.domain.entity

import com.closet.common.entity.BaseEntity
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.display.domain.enums.ExhibitionStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "exhibition")
class Exhibition(
    @Column(name = "title", nullable = false, length = 100)
    var title: String,
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "thumbnail_url", length = 500)
    var thumbnailUrl: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: ExhibitionStatus = ExhibitionStatus.DRAFT,
    @Column(name = "start_at", nullable = false, columnDefinition = "DATETIME(6)")
    var startAt: ZonedDateTime,
    @Column(name = "end_at", nullable = false, columnDefinition = "DATETIME(6)")
    var endAt: ZonedDateTime,
    @OneToMany(mappedBy = "exhibition", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val products: MutableList<ExhibitionProduct> = mutableListOf(),
) : BaseEntity() {
    fun activate() {
        status.validateTransitionTo(ExhibitionStatus.ACTIVE)
        status = ExhibitionStatus.ACTIVE
    }

    fun end() {
        status.validateTransitionTo(ExhibitionStatus.ENDED)
        status = ExhibitionStatus.ENDED
    }

    fun addProduct(product: ExhibitionProduct) {
        require(status != ExhibitionStatus.ENDED) { "종료된 기획전에는 상품을 추가할 수 없습니다" }
        product.exhibition = this
        products.add(product)
    }

    fun removeProduct(productId: Long) {
        val product =
            products.find { it.productId == productId }
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "기획전 상품을 찾을 수 없습니다: productId=$productId")
        products.remove(product)
    }
}
