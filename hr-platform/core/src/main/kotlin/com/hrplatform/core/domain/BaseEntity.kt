package com.hrplatform.core.domain

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @CreatedDate
    @Column(updatable = false, nullable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @CreatedBy
    @Column(updatable = false)
    var createdBy: Long? = null
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    lateinit var updatedAt: ZonedDateTime
        protected set

    @LastModifiedBy
    var updatedBy: Long? = null
        protected set

    @Column
    var deletedAt: ZonedDateTime? = null
        protected set

    @Column
    var deletedBy: Long? = null
        protected set

    val isDeleted: Boolean get() = deletedAt != null

    fun softDelete(now: ZonedDateTime, by: Long?) {
        require(!isDeleted) { "이미 삭제된 엔티티입니다" }
        deletedAt = now
        deletedBy = by
    }

    fun restore() {
        require(isDeleted) { "삭제되지 않은 엔티티는 복구할 수 없습니다" }
        deletedAt = null
        deletedBy = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false
        val thisId = id ?: return false
        return thisId == (other as BaseEntity).id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)
}
