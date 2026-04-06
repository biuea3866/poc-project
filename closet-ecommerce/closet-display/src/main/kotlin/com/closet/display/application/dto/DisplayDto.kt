package com.closet.display.application.dto

import com.closet.display.domain.entity.Banner
import com.closet.display.domain.entity.Exhibition
import com.closet.display.domain.entity.ExhibitionProduct
import com.closet.display.domain.entity.RankingSnapshot
import com.closet.display.domain.enums.BannerPosition
import com.closet.display.domain.enums.ExhibitionStatus
import com.closet.display.domain.enums.PeriodType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime

// ===== Banner =====

data class BannerCreateRequest(
    @field:NotBlank(message = "배너 제목은 필수입니다")
    val title: String,
    @field:NotBlank(message = "이미지 URL은 필수입니다")
    val imageUrl: String,
    @field:NotBlank(message = "링크 URL은 필수입니다")
    val linkUrl: String,
    @field:NotNull(message = "배너 위치는 필수입니다")
    val position: BannerPosition,
    val sortOrder: Int = 0,
    @field:NotNull(message = "시작일시는 필수입니다")
    val startAt: ZonedDateTime,
    @field:NotNull(message = "종료일시는 필수입니다")
    val endAt: ZonedDateTime,
)

data class BannerUpdateRequest(
    @field:NotBlank(message = "배너 제목은 필수입니다")
    val title: String,
    @field:NotBlank(message = "이미지 URL은 필수입니다")
    val imageUrl: String,
    @field:NotBlank(message = "링크 URL은 필수입니다")
    val linkUrl: String,
    @field:NotNull(message = "배너 위치는 필수입니다")
    val position: BannerPosition,
    val sortOrder: Int = 0,
    @field:NotNull(message = "시작일시는 필수입니다")
    val startAt: ZonedDateTime,
    @field:NotNull(message = "종료일시는 필수입니다")
    val endAt: ZonedDateTime,
)

data class BannerResponse(
    val id: Long,
    val title: String,
    val imageUrl: String,
    val linkUrl: String,
    val position: BannerPosition,
    val sortOrder: Int,
    val isVisible: Boolean,
    val startAt: ZonedDateTime,
    val endAt: ZonedDateTime,
) {
    companion object {
        fun from(banner: Banner): BannerResponse =
            BannerResponse(
                id = banner.id,
                title = banner.title,
                imageUrl = banner.imageUrl,
                linkUrl = banner.linkUrl,
                position = banner.position,
                sortOrder = banner.sortOrder,
                isVisible = banner.isVisible,
                startAt = banner.startAt,
                endAt = banner.endAt,
            )
    }
}

// ===== Exhibition =====

data class ExhibitionCreateRequest(
    @field:NotBlank(message = "기획전 제목은 필수입니다")
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    @field:NotNull(message = "시작일시는 필수입니다")
    val startAt: ZonedDateTime,
    @field:NotNull(message = "종료일시는 필수입니다")
    val endAt: ZonedDateTime,
)

data class ExhibitionResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val status: ExhibitionStatus,
    val startAt: ZonedDateTime,
    val endAt: ZonedDateTime,
) {
    companion object {
        fun from(exhibition: Exhibition): ExhibitionResponse =
            ExhibitionResponse(
                id = exhibition.id,
                title = exhibition.title,
                description = exhibition.description,
                thumbnailUrl = exhibition.thumbnailUrl,
                status = exhibition.status,
                startAt = exhibition.startAt,
                endAt = exhibition.endAt,
            )
    }
}

// ===== ExhibitionProduct =====

data class ExhibitionProductCreateRequest(
    @field:NotNull(message = "상품 ID는 필수입니다")
    val productId: Long,
    val sortOrder: Int = 0,
    @field:Min(0, message = "할인율은 0 이상이어야 합니다")
    val discountRate: Int = 0,
)

data class ExhibitionProductResponse(
    val id: Long,
    val productId: Long,
    val sortOrder: Int,
    val discountRate: Int,
) {
    companion object {
        fun from(ep: ExhibitionProduct): ExhibitionProductResponse =
            ExhibitionProductResponse(
                id = ep.id,
                productId = ep.productId,
                sortOrder = ep.sortOrder,
                discountRate = ep.discountRate,
            )
    }
}

// ===== Ranking =====

data class RankingResponse(
    val id: Long,
    val categoryId: Long,
    val productId: Long,
    val rankPosition: Int,
    val score: Double,
    val periodType: PeriodType,
    val snapshotDate: ZonedDateTime,
) {
    companion object {
        fun from(snapshot: RankingSnapshot): RankingResponse =
            RankingResponse(
                id = snapshot.id,
                categoryId = snapshot.categoryId,
                productId = snapshot.productId,
                rankPosition = snapshot.rankPosition,
                score = snapshot.score,
                periodType = snapshot.periodType,
                snapshotDate = snapshot.snapshotDate,
            )
    }
}
