package com.closet.display.domain.entity

import com.closet.common.entity.BaseEntity
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.display.domain.enums.SnapStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "snap")
class Snap(
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "image_url", nullable = false, length = 500)
    var imageUrl: String,
    @Column(name = "description", length = 1000)
    var description: String? = null,
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0,
    @Column(name = "report_count", nullable = false)
    var reportCount: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: SnapStatus = SnapStatus.ACTIVE,
    @OneToMany(mappedBy = "snap", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val productTags: MutableList<SnapProductTag> = mutableListOf(),
) : BaseEntity() {
    fun like() {
        if (status != SnapStatus.ACTIVE) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "활성 상태의 스냅만 좋아요할 수 있습니다")
        }
        likeCount++
    }

    fun unlike() {
        if (likeCount > 0) {
            likeCount--
        }
    }

    fun report() {
        reportCount++
        if (reportCount >= REPORT_THRESHOLD) {
            status = SnapStatus.REPORTED
        }
    }

    fun hide() {
        status.validateTransitionTo(SnapStatus.HIDDEN)
        status = SnapStatus.HIDDEN
    }

    fun restore() {
        status.validateTransitionTo(SnapStatus.ACTIVE)
        status = SnapStatus.ACTIVE
        reportCount = 0
    }

    fun addProductTag(tag: SnapProductTag) {
        tag.snap = this
        productTags.add(tag)
    }

    fun removeProductTag(productId: Long) {
        val tag =
            productTags.find { it.productId == productId }
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "스냅 상품 태그를 찾을 수 없습니다: productId=$productId")
        productTags.remove(tag)
    }

    companion object {
        private const val REPORT_THRESHOLD = 5

        fun create(
            memberId: Long,
            imageUrl: String,
            description: String? = null,
        ): Snap {
            require(imageUrl.isNotBlank()) { "이미지 URL은 비어있을 수 없습니다" }
            return Snap(
                memberId = memberId,
                imageUrl = imageUrl,
                description = description,
            )
        }
    }
}
