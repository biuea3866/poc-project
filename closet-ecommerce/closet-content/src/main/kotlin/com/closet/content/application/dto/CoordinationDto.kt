package com.closet.content.application.dto

import com.closet.content.domain.entity.Coordination
import com.closet.content.domain.entity.CoordinationProduct
import com.closet.content.domain.enums.CoordinationGender
import com.closet.content.domain.enums.CoordinationSeason
import com.closet.content.domain.enums.CoordinationStatus
import com.closet.content.domain.enums.CoordinationStyle
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CoordinationCreateRequest(
    @field:NotBlank(message = "코디 제목은 필수입니다")
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    @field:NotNull(message = "스타일은 필수입니다")
    val style: CoordinationStyle,
    @field:NotNull(message = "시즌은 필수입니다")
    val season: CoordinationSeason,
    @field:NotNull(message = "성별은 필수입니다")
    val gender: CoordinationGender
)

data class CoordinationProductAddRequest(
    @field:NotNull(message = "상품 ID는 필수입니다")
    val productId: Long,
    val sortOrder: Int = 0,
    val description: String? = null
)

data class CoordinationResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val style: CoordinationStyle,
    val season: CoordinationSeason,
    val gender: CoordinationGender,
    val status: CoordinationStatus,
    val products: List<CoordinationProductResponse>
) {
    companion object {
        fun from(coordination: Coordination): CoordinationResponse = CoordinationResponse(
            id = coordination.id,
            title = coordination.title,
            description = coordination.description,
            thumbnailUrl = coordination.thumbnailUrl,
            style = coordination.style,
            season = coordination.season,
            gender = coordination.gender,
            status = coordination.status,
            products = coordination.products.map { CoordinationProductResponse.from(it) }
        )
    }
}

data class CoordinationListResponse(
    val id: Long,
    val title: String,
    val thumbnailUrl: String?,
    val style: CoordinationStyle,
    val season: CoordinationSeason,
    val gender: CoordinationGender,
    val status: CoordinationStatus,
    val productCount: Int
) {
    companion object {
        fun from(coordination: Coordination): CoordinationListResponse = CoordinationListResponse(
            id = coordination.id,
            title = coordination.title,
            thumbnailUrl = coordination.thumbnailUrl,
            style = coordination.style,
            season = coordination.season,
            gender = coordination.gender,
            status = coordination.status,
            productCount = coordination.products.size
        )
    }
}

data class CoordinationProductResponse(
    val id: Long,
    val productId: Long,
    val sortOrder: Int,
    val description: String?
) {
    companion object {
        fun from(product: CoordinationProduct): CoordinationProductResponse = CoordinationProductResponse(
            id = product.id,
            productId = product.productId,
            sortOrder = product.sortOrder,
            description = product.description
        )
    }
}
