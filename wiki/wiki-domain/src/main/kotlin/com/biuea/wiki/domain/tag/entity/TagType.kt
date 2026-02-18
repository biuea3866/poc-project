package com.biuea.wiki.domain.tag.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "tag_type")
class TagType(
    @Column(name = "tag_type")
    @Enumerated(EnumType.STRING)
    val tagConstant: TagConstant,

    @Column(name = "created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "deleted_at")
    val deletedAt: ZonedDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) {
    @OneToMany(mappedBy = "tagType", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var tags: MutableList<Tag> = mutableListOf()
        protected set

    fun addTag(tag: Tag) {
        this.tags.add(tag)
        tag.mappedBy(this)
    }

    companion object {
        fun create(tagConstant: TagConstant): TagType {
            return TagType(tagConstant = tagConstant)
        }
    }
}

enum class TagConstant {
    TECH,
    BACKEND,
    FRONTEND,
    DEVOPS,
    AI,
    DAILY,
    TRAVEL
}
