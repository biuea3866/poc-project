package com.closet.display.domain.entity

import com.closet.common.entity.BaseEntity
import com.closet.display.domain.enums.BannerPosition
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "banner")
class Banner(
    @Column(name = "title", nullable = false, length = 100)
    var title: String,
    @Column(name = "image_url", nullable = false, length = 500)
    var imageUrl: String,
    @Column(name = "link_url", nullable = false, length = 500)
    var linkUrl: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var position: BannerPosition,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
    @Column(name = "is_visible", nullable = false, columnDefinition = "TINYINT(1)")
    var isVisible: Boolean = true,
    @Column(name = "start_at", nullable = false, columnDefinition = "DATETIME(6)")
    var startAt: ZonedDateTime,
    @Column(name = "end_at", nullable = false, columnDefinition = "DATETIME(6)")
    var endAt: ZonedDateTime,
) : BaseEntity() {
    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }

    fun isActive(now: ZonedDateTime): Boolean {
        return isVisible && now.isAfter(startAt) && now.isBefore(endAt)
    }

    fun update(
        title: String,
        imageUrl: String,
        linkUrl: String,
        position: BannerPosition,
        sortOrder: Int,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ) {
        require(endAt.isAfter(startAt)) { "종료일시는 시작일시 이후여야 합니다" }
        this.title = title
        this.imageUrl = imageUrl
        this.linkUrl = linkUrl
        this.position = position
        this.sortOrder = sortOrder
        this.startAt = startAt
        this.endAt = endAt
    }
}
