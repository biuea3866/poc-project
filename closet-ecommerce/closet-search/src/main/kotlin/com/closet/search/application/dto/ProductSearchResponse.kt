package com.closet.search.application.dto

import com.closet.search.domain.ProductDocument
import java.math.BigDecimal

/**
 * 상품 검색 응답 DTO.
 */
data class ProductSearchResponse(
    val productId: Long,
    val name: String,
    val brandName: String?,
    val categoryName: String?,
    val basePrice: BigDecimal,
    val salePrice: BigDecimal,
    val discountRate: Int,
    val sizes: List<String>,
    val colors: List<String>,
    val fitType: String?,
    val gender: String?,
    val season: String?,
    val imageUrl: String?,
    val reviewCount: Int,
    val avgRating: Double,
    val popularityScore: Double,
    val highlights: Map<String, List<String>> = emptyMap(),
) {
    companion object {
        fun from(doc: ProductDocument): ProductSearchResponse {
            return ProductSearchResponse(
                productId = doc.productId,
                name = doc.name,
                brandName = doc.brandName,
                categoryName = doc.categoryName,
                basePrice = doc.basePrice,
                salePrice = doc.salePrice,
                discountRate = doc.discountRate,
                sizes = doc.sizes,
                colors = doc.colors,
                fitType = doc.fitType,
                gender = doc.gender,
                season = doc.season,
                imageUrl = doc.imageUrl,
                reviewCount = doc.reviewCount,
                avgRating = doc.avgRating,
                popularityScore = doc.popularityScore,
            )
        }

        fun from(
            doc: ProductDocument,
            highlightFields: Map<String, List<String>>,
        ): ProductSearchResponse {
            return ProductSearchResponse(
                productId = doc.productId,
                name = doc.name,
                brandName = doc.brandName,
                categoryName = doc.categoryName,
                basePrice = doc.basePrice,
                salePrice = doc.salePrice,
                discountRate = doc.discountRate,
                sizes = doc.sizes,
                colors = doc.colors,
                fitType = doc.fitType,
                gender = doc.gender,
                season = doc.season,
                imageUrl = doc.imageUrl,
                reviewCount = doc.reviewCount,
                avgRating = doc.avgRating,
                popularityScore = doc.popularityScore,
                highlights = highlightFields,
            )
        }
    }
}
