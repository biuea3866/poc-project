package com.closet.search.application.dto

import com.closet.search.domain.ProductDocument

data class ProductSearchFilter(
    val categoryId: Long? = null,
    val brandId: Long? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val gender: String? = null,
    val season: String? = null,
    val sort: String? = null,
)

data class ProductSearchResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val brandId: Long,
    val brandName: String?,
    val categoryId: Long,
    val categoryName: String?,
    val basePrice: Long,
    val salePrice: Long,
    val discountRate: Int,
    val status: String,
    val season: String,
    val fitType: String,
    val gender: String,
) {
    companion object {
        fun from(doc: ProductDocument): ProductSearchResponse = ProductSearchResponse(
            id = doc.id,
            name = doc.name,
            description = doc.description,
            brandId = doc.brandId,
            brandName = doc.brandName,
            categoryId = doc.categoryId,
            categoryName = doc.categoryName,
            basePrice = doc.basePrice,
            salePrice = doc.salePrice,
            discountRate = doc.discountRate,
            status = doc.status,
            season = doc.season,
            fitType = doc.fitType,
            gender = doc.gender,
        )
    }
}

data class AutocompleteResponse(
    val suggestions: List<String>,
)

data class IndexSyncResponse(
    val indexedCount: Int,
    val message: String,
)

/**
 * product-service 응답을 매핑하기 위한 DTO
 */
data class ProductServiceResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val brandId: Long,
    val categoryId: Long,
    val basePrice: Long,
    val salePrice: Long,
    val discountRate: Int,
    val status: String,
    val season: String?,
    val fitType: String?,
    val gender: String?,
)

data class ProductServicePageResponse(
    val content: List<ProductServiceResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
    val number: Int,
)
