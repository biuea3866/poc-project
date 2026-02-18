package com.biuea.wiki.domain.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JwtTokenProviderTest {

    @Test
    fun createAndParseAccessToken() {
        val tokenProvider = JwtTokenProvider(
            secret = "this-is-a-test-jwt-secret-key-at-least-32-bytes-long",
            accessTokenExpirationMs = 60_000,
            refreshTokenExpirationMs = 120_000,
        )

        val token = tokenProvider.createAccessToken(
            AuthenticatedUser(
                id = 1L,
                email = "user@example.com",
                name = "user",
            )
        )

        assertTrue(tokenProvider.validateToken(token))

        val authentication = tokenProvider.getAuthentication(token)
        assertNotNull(authentication)

        val principal = authentication.principal as AuthenticatedUser
        assertEquals(1L, principal.id)
        assertEquals("user@example.com", principal.email)
        assertEquals("user", principal.name)
    }
}
