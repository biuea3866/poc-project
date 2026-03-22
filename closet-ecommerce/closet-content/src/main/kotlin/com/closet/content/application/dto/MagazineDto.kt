package com.closet.content.application.dto

import com.closet.content.domain.entity.Magazine
import com.closet.content.domain.enums.MagazineStatus
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class MagazineCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    val subtitle: String? = null,
    @field:NotBlank(message = "본문 내용은 필수입니다")
    val content: String,
    val thumbnailUrl: String? = null,
    @field:NotBlank(message = "작성자는 필수입니다")
    val author: String,
    val tags: List<String> = emptyList()
)

data class MagazineResponse(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val content: String,
    val thumbnailUrl: String?,
    val author: String,
    val status: MagazineStatus,
    val publishedAt: LocalDateTime?,
    val tags: List<String>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(magazine: Magazine): MagazineResponse = MagazineResponse(
            id = magazine.id,
            title = magazine.title,
            subtitle = magazine.subtitle,
            content = magazine.content,
            thumbnailUrl = magazine.thumbnailUrl,
            author = magazine.author,
            status = magazine.status,
            publishedAt = magazine.publishedAt,
            tags = magazine.tags.map { it.tagName },
            createdAt = if (magazine.id != 0L) magazine.createdAt else null,
            updatedAt = if (magazine.id != 0L) magazine.updatedAt else null
        )
    }
}

data class MagazineListResponse(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val author: String,
    val status: MagazineStatus,
    val publishedAt: LocalDateTime?,
    val tags: List<String>
) {
    companion object {
        fun from(magazine: Magazine): MagazineListResponse = MagazineListResponse(
            id = magazine.id,
            title = magazine.title,
            subtitle = magazine.subtitle,
            thumbnailUrl = magazine.thumbnailUrl,
            author = magazine.author,
            status = magazine.status,
            publishedAt = magazine.publishedAt,
            tags = magazine.tags.map { it.tagName }
        )
    }
}
