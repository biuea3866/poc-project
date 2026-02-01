package com.biuea.wiki.domain.document.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(
    name = "tag",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_tag_name_type", columnNames = ["name", "tag_type_id"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Tag(
    @Column(nullable = false, length = 100)
    val name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_type_id", nullable = false)
    val tagType: TagType,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) {
    companion object {
        fun create(name: String, tagType: TagType): Tag {
            return Tag(name = name, tagType = tagType)
        }
    }
}
