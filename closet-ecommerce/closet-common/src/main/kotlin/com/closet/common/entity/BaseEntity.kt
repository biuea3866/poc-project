package com.closet.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    @Column(columnDefinition = "DATETIME(6)")
    var deletedAt: ZonedDateTime? = null

    fun softDelete() {
        this.deletedAt = ZonedDateTime.now()
    }

    fun isDeleted(): Boolean = deletedAt != null
}
