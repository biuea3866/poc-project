package com.biuea.wiki.infrastructure.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${security.jwt.secret}")
    secret: String,
    @Value("\${security.jwt.access-token-expiration-ms:3600000}")
    private val accessTokenExpirationMs: Long,
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun createAccessToken(authenticatedUser: AuthenticatedUser): String {
        val issuedAt = Date()
        val expiration = Date(issuedAt.time + accessTokenExpirationMs)

        return Jwts.builder()
            .subject(authenticatedUser.id.toString())
            .claim("email", authenticatedUser.email)
            .claim("name", authenticatedUser.name)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    fun resolveToken(authorizationHeader: String?): String? {
        if (authorizationHeader.isNullOrBlank()) return null
        if (!authorizationHeader.startsWith("Bearer ")) return null

        return authorizationHeader.substring(7).trim().ifBlank { null }
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
            true
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun getAuthentication(token: String): Authentication? {
        val claims = parseClaims(token) ?: return null
        val userId = claims.subject?.toLongOrNull() ?: return null
        val email = claims["email"]?.toString() ?: return null
        val name = claims["name"]?.toString() ?: return null

        val principal = AuthenticatedUser(
            id = userId,
            email = email,
            name = name,
        )

        return UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER")),
        )
    }

    fun getExpirationTimeMillis(token: String): Long? {
        return parseClaims(token)?.expiration?.time
    }

    private fun parseClaims(token: String): io.jsonwebtoken.Claims? {
        return try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).payload
        } catch (expiredJwtException: ExpiredJwtException) {
            expiredJwtException.claims
        } catch (_: JwtException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
