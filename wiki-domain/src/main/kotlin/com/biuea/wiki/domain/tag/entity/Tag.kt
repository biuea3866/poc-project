package com.biuea.wiki.domain.tag.entity

import com.biuea.wiki.domain.tag.rule.DefaultTagRule
import com.biuea.wiki.domain.tag.rule.TagRule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "tag")
@EntityListeners(AuditingEntityListener::class)
class Tag(
    @Column(name = "name")
    val name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_type_id")
    var tagType: TagType,

    @CreatedDate
    @Column(name = "created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) {
    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY, cascade = [jakarta.persistence.CascadeType.ALL], orphanRemoval = true)
    var mappings: MutableList<TagDocumentMapping> = mutableListOf()
        protected set

    @Transient
    private var tagRule: TagRule = DefaultTagRule()

    fun applyTagRule(tagRule: TagRule) {
        this.tagRule = tagRule
    }

    fun validate() {
        this.tagRule.validate(this)
    }

    fun mappedBy(tagType: TagType) {
        this.tagType = tagType
    }

    fun addMapping(mapping: TagDocumentMapping) {
        mappings.add(mapping)
    }

    companion object {
        fun create(name: String, tagType: TagType): Tag {
            return Tag(name = name, tagType = tagType)
        }
    }
}
