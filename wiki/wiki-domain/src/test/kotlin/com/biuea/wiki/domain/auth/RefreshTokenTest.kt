package com.biuea.wiki.domain.auth

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshTokenTest {

    private fun buildToken(
        isRevoked: Boolean = false,
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7),
    ) = RefreshToken(
        tokenHash = "abc123",
        userId = 1L,
        familyId = "family-uuid",
        isRevoked = isRevoked,
        expiresAt = expiresAt,
    )

    @Test
    fun `isValid returns true for active non-expired token`() {
        val token = buildToken()
        assertTrue(token.isValid())
    }

    @Test
    fun `isValid returns false for revoked token`() {
        val token = buildToken(isRevoked = true)
        assertFalse(token.isValid())
    }

    @Test
    fun `isValid returns false for expired token`() {
        val token = buildToken(expiresAt = LocalDateTime.now().minusSeconds(1))
        assertFalse(token.isValid())
    }

    @Test
    fun `revoke sets isRevoked to true`() {
        val token = buildToken()
        assertFalse(token.isRevoked)
        token.revoke()
        assertTrue(token.isRevoked)
    }

    @Test
    fun `isExpired returns false for future expiry`() {
        val token = buildToken(expiresAt = LocalDateTime.now().plusMinutes(10))
        assertFalse(token.isExpired())
    }

    @Test
    fun `isExpired returns true for past expiry`() {
        val token = buildToken(expiresAt = LocalDateTime.now().minusMinutes(1))
        assertTrue(token.isExpired())
    }

    @Test
    fun `hashToken produces deterministic SHA-256 hex`() {
        val rawToken = "test-refresh-token-value"
        val hash1 = RefreshToken.hashToken(rawToken)
        val hash2 = RefreshToken.hashToken(rawToken)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length, "SHA-256 hex should be 64 characters")
    }

    @Test
    fun `hashToken produces different hashes for different inputs`() {
        val hash1 = RefreshToken.hashToken("token-a")
        val hash2 = RefreshToken.hashToken("token-b")
        assertTrue(hash1 != hash2)
    }
}
