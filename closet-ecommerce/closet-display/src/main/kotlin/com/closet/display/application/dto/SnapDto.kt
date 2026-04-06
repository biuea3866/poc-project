package com.closet.display.application.dto

import com.closet.display.domain.entity.Snap
import com.closet.display.domain.entity.SnapProductTag
import com.closet.display.domain.enums.SnapStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// ===== Snap =====

data class SnapCreateRequest(
    @field:NotNull(message = "회원 ID는 필수입니다")
    val memberId: Long,
    @field:NotBlank(message = "이미지 URL은 필수입니다")
    val imageUrl: String,
    val description: String? = null,
    val productTags: List<SnapProductTagRequest> = emptyList(),
)

data class SnapResponse(
    val id: Long,
    val memberId: Long,
    val imageUrl: String,
    val description: String?,
    val likeCount: Long,
    val reportCount: Int,
    val status: SnapStatus,
    val productTags: List<SnapProductTagResponse>,
) {
    companion object {
        fun from(snap: Snap): SnapResponse =
            SnapResponse(
                id = snap.id,
                memberId = snap.memberId,
                imageUrl = snap.imageUrl,
                description = snap.description,
                likeCount = snap.likeCount,
                reportCount = snap.reportCount,
                status = snap.status,
                productTags = snap.productTags.map { SnapProductTagResponse.from(it) },
            )
    }
}

// ===== SnapProductTag =====

data class SnapProductTagRequest(
    @field:NotNull(message = "상품 ID는 필수입니다")
    val productId: Long,
    val positionX: Double = 0.0,
    val positionY: Double = 0.0,
)

data class SnapProductTagResponse(
    val id: Long,
    val productId: Long,
    val positionX: Double,
    val positionY: Double,
) {
    companion object {
        fun from(tag: SnapProductTag): SnapProductTagResponse =
            SnapProductTagResponse(
                id = tag.id,
                productId = tag.productId,
                positionX = tag.positionX,
                positionY = tag.positionY,
            )
    }
}

// ===== SnapLike =====

data class SnapLikeRequest(
    @field:NotNull(message = "회원 ID는 필수입니다")
    val memberId: Long,
)

data class SnapReportRequest(
    @field:NotNull(message = "회원 ID는 필수입니다")
    val memberId: Long,
)
