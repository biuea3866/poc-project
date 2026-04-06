package com.closet.product.domain.entity

import com.closet.common.entity.BaseEntity
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.vo.Money
import com.closet.product.domain.enums.FitType
import com.closet.product.domain.enums.Gender
import com.closet.product.domain.enums.ProductStatus
import com.closet.product.domain.enums.Season
import jakarta.persistence.AttributeOverride
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "product")
class Product(
    @Column(name = "name", nullable = false, length = 100)
    var name: String,
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String,
    @Column(name = "brand_id", nullable = false)
    var brandId: Long,
    @Column(name = "category_id", nullable = false)
    var categoryId: Long,
    @AttributeOverride(name = "amount", column = Column(name = "base_price", nullable = false, columnDefinition = "BIGINT"))
    var basePrice: Money,
    @AttributeOverride(name = "amount", column = Column(name = "sale_price", nullable = false, columnDefinition = "BIGINT"))
    var salePrice: Money,
    @Column(name = "discount_rate", nullable = false)
    var discountRate: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: ProductStatus = ProductStatus.DRAFT,
    @Enumerated(EnumType.STRING)
    @Column(name = "season", length = 30, columnDefinition = "VARCHAR(30)")
    var season: Season? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "fit_type", length = 30, columnDefinition = "VARCHAR(30)")
    var fitType: FitType? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 30, columnDefinition = "VARCHAR(30)")
    var gender: Gender? = null,
    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val options: MutableList<ProductOption> = mutableListOf(),
    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val images: MutableList<ProductImage> = mutableListOf(),
    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val sizeGuides: MutableList<SizeGuide> = mutableListOf(),
) : BaseEntity() {
    fun activate() {
        status.validateTransitionTo(ProductStatus.ACTIVE)
        status = ProductStatus.ACTIVE
    }

    fun deactivate() {
        status.validateTransitionTo(ProductStatus.INACTIVE)
        status = ProductStatus.INACTIVE
    }

    fun markSoldOut() {
        status.validateTransitionTo(ProductStatus.SOLD_OUT)
        status = ProductStatus.SOLD_OUT
    }

    fun changeStatus(target: ProductStatus) {
        status.validateTransitionTo(target)
        status = target
    }

    fun updatePrice(
        newBasePrice: Money,
        newSalePrice: Money,
        newDiscountRate: Int,
    ) {
        require(newSalePrice <= newBasePrice) { "판매가는 정가 이하여야 합니다" }
        require(newDiscountRate in 0..100) { "할인율은 0~100 사이여야 합니다" }
        this.basePrice = newBasePrice
        this.salePrice = newSalePrice
        this.discountRate = newDiscountRate
    }

    fun addOption(option: ProductOption) {
        option.product = this
        options.add(option)
    }

    fun removeOption(optionId: Long) {
        val option =
            options.find { it.id == optionId }
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "옵션을 찾을 수 없습니다: $optionId")
        options.remove(option)
    }

    fun addImage(image: ProductImage) {
        image.product = this
        images.add(image)
    }

    fun update(
        name: String,
        description: String,
        brandId: Long,
        categoryId: Long,
        basePrice: Money,
        salePrice: Money,
        discountRate: Int,
        season: Season?,
        fitType: FitType?,
        gender: Gender?,
    ) {
        require(salePrice <= basePrice) { "판매가는 정가 이하여야 합니다" }
        this.name = name
        this.description = description
        this.brandId = brandId
        this.categoryId = categoryId
        this.basePrice = basePrice
        this.salePrice = salePrice
        this.discountRate = discountRate
        this.season = season
        this.fitType = fitType
        this.gender = gender
    }
}
