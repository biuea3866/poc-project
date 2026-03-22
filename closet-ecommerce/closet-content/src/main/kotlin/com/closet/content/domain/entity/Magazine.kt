package com.closet.content.domain.entity

import com.closet.common.entity.BaseEntity
import com.closet.content.domain.enums.MagazineStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "magazine")
class Magazine(

    @Column(name = "title", nullable = false, length = 200)
    var title: String,

    @Column(name = "subtitle", length = 300)
    var subtitle: String? = null,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "thumbnail_url", length = 500)
    var thumbnailUrl: String? = null,

    @Column(name = "author", nullable = false, length = 100)
    var author: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: MagazineStatus = MagazineStatus.DRAFT,

    @Column(name = "published_at", columnDefinition = "DATETIME(6)")
    var publishedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "magazine", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tags: MutableList<MagazineTag> = mutableListOf()

) : BaseEntity() {

    fun publish() {
        status.validateTransitionTo(MagazineStatus.PUBLISHED)
        status = MagazineStatus.PUBLISHED
        publishedAt = LocalDateTime.now()
    }

    fun archive() {
        status.validateTransitionTo(MagazineStatus.ARCHIVED)
        status = MagazineStatus.ARCHIVED
    }

    fun addTag(tagName: String) {
        val exists = tags.any { it.tagName == tagName }
        if (!exists) {
            val tag = MagazineTag(tagName = tagName)
            tag.magazine = this
            tags.add(tag)
        }
    }

    fun removeTag(tagName: String) {
        tags.removeIf { it.tagName == tagName }
    }
}
