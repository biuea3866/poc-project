package com.closet.display.application.dto

import com.closet.display.domain.entity.Magazine
import com.closet.display.domain.entity.MagazineProduct
import com.closet.display.domain.entity.MagazineTag
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime

// ===== Magazine =====

data class MagazineCreateRequest(
    @field:NotBlank(message = "매거진 제목은 필수입니다")
    val title: String,
    val subtitle: String? = null,
    @field:NotBlank(message = "매거진 본문은 필수입니다")
    val contentBody: String,
    val thumbnailUrl: String? = null,
    @field:NotBlank(message = "카테고리는 필수입니다")
    val category: String,
    @field:NotBlank(message = "작성자는 필수입니다")
    val authorName: String,
)

data class MagazineUpdateRequest(
    @field:NotBlank(message = "매거진 제목은 필수입니다")
    val title: String,
    val subtitle: String? = null,
    @field:NotBlank(message = "매거진 본문은 필수입니다")
    val contentBody: String,
    val thumbnailUrl: String? = null,
    @field:NotBlank(message = "카테고리는 필수입니다")
    val category: String,
    @field:NotBlank(message = "작성자는 필수입니다")
    val authorName: String,
)

data class MagazineResponse(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val contentBody: String,
    val thumbnailUrl: String?,
    val category: String,
    val authorName: String,
    val viewCount: Long,
    val isPublished: Boolean,
    val publishedAt: ZonedDateTime?,
    val products: List<MagazineProductResponse>,
    val tags: List<String>,
) {
    companion object {
        fun from(magazine: Magazine): MagazineResponse =
            MagazineResponse(
                id = magazine.id,
                title = magazine.title,
                subtitle = magazine.subtitle,
                contentBody = magazine.contentBody,
                thumbnailUrl = magazine.thumbnailUrl,
                category = magazine.category,
                authorName = magazine.authorName,
                viewCount = magazine.viewCount,
                isPublished = magazine.isPublished,
                publishedAt = magazine.publishedAt,
                products = magazine.products.sortedBy { it.sortOrder }.map { MagazineProductResponse.from(it) },
                tags = magazine.tags.map { it.tagName },
            )
    }
}

// ===== MagazineProduct =====

data class MagazineProductCreateRequest(
    @field:NotNull(message = "상품 ID는 필수입니다")
    val productId: Long,
    val sortOrder: Int = 0,
)

data class MagazineProductResponse(
    val id: Long,
    val productId: Long,
    val sortOrder: Int,
) {
    companion object {
        fun from(mp: MagazineProduct): MagazineProductResponse =
            MagazineProductResponse(
                id = mp.id,
                productId = mp.productId,
                sortOrder = mp.sortOrder,
            )
    }
}

// ===== MagazineTag =====

data class MagazineTagCreateRequest(
    @field:NotBlank(message = "태그명은 필수입니다")
    val tagName: String,
)

data class MagazineTagResponse(
    val id: Long,
    val tagName: String,
) {
    companion object {
        fun from(tag: MagazineTag): MagazineTagResponse =
            MagazineTagResponse(
                id = tag.id,
                tagName = tag.tagName,
            )
    }
}
