package com.hrplatform.core.domain

import java.time.ZonedDateTime

abstract class BaseEntity(
    open val id: Long?,
    open val createdAt: ZonedDateTime,
    open val updatedAt: ZonedDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseEntity) return false
        val thisId = id ?: return false
        return thisId == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)
}
