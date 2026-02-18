package com.biuea.wiki.domain.user

import com.biuea.wiki.domain.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user")
class User(
    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {

    fun softDelete() {
        this.deletedAt = LocalDateTime.now()
    }

    fun isDeleted(): Boolean = deletedAt != null
}
