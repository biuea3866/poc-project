package com.closet.content.application.dto

import com.closet.content.domain.entity.OotdSnap
import com.closet.content.domain.enums.OotdSnapStatus
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class OotdSnapCreateRequest(
    @field:NotBlank(message = "이미지 URL은 필수입니다")
    val imageUrl: String,
    val content: String? = null
)

data class OotdSnapResponse(
    val id: Long,
    val memberId: Long,
    val imageUrl: String,
    val content: String?,
    val likeCount: Int,
    val status: OotdSnapStatus,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(snap: OotdSnap): OotdSnapResponse = OotdSnapResponse(
            id = snap.id,
            memberId = snap.memberId,
            imageUrl = snap.imageUrl,
            content = snap.content,
            likeCount = snap.likeCount,
            status = snap.status,
            createdAt = if (snap.id != 0L) snap.createdAt else null,
            updatedAt = if (snap.id != 0L) snap.updatedAt else null
        )
    }
}

data class OotdSnapListResponse(
    val id: Long,
    val memberId: Long,
    val imageUrl: String,
    val content: String?,
    val likeCount: Int,
    val status: OotdSnapStatus
) {
    companion object {
        fun from(snap: OotdSnap): OotdSnapListResponse = OotdSnapListResponse(
            id = snap.id,
            memberId = snap.memberId,
            imageUrl = snap.imageUrl,
            content = snap.content,
            likeCount = snap.likeCount,
            status = snap.status
        )
    }
}
