package com.biuea.wiki.domain.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): User?

    fun findByIdAndDeletedAtIsNull(id: Long): User?

    fun findByEmailAndDeletedAtIsNull(email: String): User?
}
