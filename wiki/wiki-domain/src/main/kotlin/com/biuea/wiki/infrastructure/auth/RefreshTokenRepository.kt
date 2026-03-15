package com.biuea.wiki.infrastructure.auth

import com.biuea.wiki.domain.auth.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByTokenHash(tokenHash: String): RefreshToken?

    fun findAllByFamilyId(familyId: String): List<RefreshToken>

    fun findAllByUserId(userId: Long): List<RefreshToken>

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.familyId = :familyId")
    fun revokeAllByFamilyId(familyId: String): Int

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.userId = :userId")
    fun revokeAllByUserId(userId: Long): Int
}
