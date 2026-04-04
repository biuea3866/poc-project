package com.closet.product.application.event

import com.closet.product.domain.entity.Product
import java.math.BigDecimal

/**
 * 상품 생성 이벤트.
 * ApplicationEventPublisher를 통해 발행되며,
 * ProductOutboxListener가 outbox_event 테이블에 INSERT한다.
 */
data class ProductCreatedEvent(
    val productId: Long,
    val name: String,
    val description: String,
    val brandId: Long,
    val categoryId: Long,
    val basePrice: BigDecimal,
    val salePrice: BigDecimal,
    val discountRate: Int,
    val status: String,
    val season: String?,
    val fitType: String?,
    val gender: String?,
    val sizes: List<String>,
    val colors: List<String>,
    val imageUrl: String?,
) {
    companion object {
        fun from(product: Product): ProductCreatedEvent {
            return ProductCreatedEvent(
                productId = product.id,
                name = product.name,
                description = product.description,
                brandId = product.brandId,
                categoryId = product.categoryId,
                basePrice = product.basePrice.amount,
                salePrice = product.salePrice.amount,
                discountRate = product.discountRate,
                status = product.status.name,
                season = product.season?.name,
                fitType = product.fitType?.name,
                gender = product.gender?.name,
                sizes = product.options.map { it.size.name }.distinct(),
                colors = product.options.map { it.colorName }.distinct(),
                imageUrl = product.images.firstOrNull()?.imageUrl,
            )
        }
    }
}
