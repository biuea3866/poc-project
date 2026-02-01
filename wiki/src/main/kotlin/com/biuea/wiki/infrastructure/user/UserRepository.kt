package com.biuea.wiki.infrastructure.user

import com.biuea.wiki.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): User?

    fun findByIdAndDeletedAtIsNull(id: Long): User?

    fun findByEmailAndDeletedAtIsNull(email: String): User?
}