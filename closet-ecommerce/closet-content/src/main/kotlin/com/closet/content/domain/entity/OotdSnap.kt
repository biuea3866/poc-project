package com.closet.content.domain.entity

import com.closet.common.entity.BaseEntity
import com.closet.content.domain.enums.OotdSnapStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "ootd_snap")
class OotdSnap(

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "image_url", nullable = false, length = 500)
    var imageUrl: String,

    @Column(name = "content", length = 500)
    var content: String? = null,

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: OotdSnapStatus = OotdSnapStatus.ACTIVE

) : BaseEntity() {

    fun like() {
        require(status == OotdSnapStatus.ACTIVE) { "활성 상태의 스냅만 좋아요 할 수 있습니다" }
        likeCount++
    }

    fun hide() {
        status.validateTransitionTo(OotdSnapStatus.HIDDEN)
        status = OotdSnapStatus.HIDDEN
    }

    fun delete() {
        status.validateTransitionTo(OotdSnapStatus.DELETED)
        status = OotdSnapStatus.DELETED
        softDelete()
    }
}
