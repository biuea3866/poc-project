package com.biuea.wiki.domain.auth

import io.jsonwebtoken.Claims
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${security.jwt.secret}")
    secret: String,
    @Value("\${security.jwt.access-token-expiration-ms:3600000}")
    private val accessTokenExpirationMs: Long,
    @Value("\${security.jwt.refresh-token-expiration-ms:604800000}")
    private val refreshTokenExpirationMs: Long,
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun createAccessToken(authenticatedUser: AuthenticatedUser): String {
        val issuedAt = Date()
        val expiration = Date(issuedAt.time + accessTokenExpirationMs)

        return Jwts.builder()
            .subject(authenticatedUser.id.toString())
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
            .claim("email", authenticatedUser.email)
            .claim("name", authenticatedUser.name)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    fun createRefreshToken(userId: Long): String {
        val issuedAt = Date()
        val expiration = Date(issuedAt.time + refreshTokenExpirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
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

    fun isRefreshToken(token: String): Boolean {
        return parseClaims(token)?.get(CLAIM_TOKEN_TYPE)?.toString() == TOKEN_TYPE_REFRESH
    }

    fun getAuthentication(token: String): Authentication? {
        val claims = parseClaims(token) ?: return null
        if (claims[CLAIM_TOKEN_TYPE]?.toString() != TOKEN_TYPE_ACCESS) return null

        val userId = claims.subject?.toLongOrNull() ?: return null
        val email = claims["email"]?.toString() ?: return null
        val name = claims["name"]?.toString() ?: return null

        val principal = AuthenticatedUser(id = userId, email = email, name = name)
        return UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER")),
        )
    }

    fun getExpirationTime(token: String): LocalDateTime? {
        val expiration = parseClaims(token)?.expiration ?: return null
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault())
    }

    fun getUserId(token: String): Long? {
        return parseClaims(token)?.subject?.toLongOrNull()
    }

    private fun parseClaims(token: String): Claims? {
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

    companion object {
        private const val CLAIM_TOKEN_TYPE = "token_type"
        private const val TOKEN_TYPE_ACCESS = "ACCESS"
        private const val TOKEN_TYPE_REFRESH = "REFRESH"
    }
}
