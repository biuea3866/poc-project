package com.biuea.springai.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * JWT 발급/검증. HS256 대칭키 기반.
 *
 * Claims 구성:
 *   - sub: 클라이언트 식별자 (username 또는 API key id)
 *   - iss: clothing-ecommerce-mcp
 *   - scopes: ["catalog:read", "order:read"] (커스텀 클레임, 공백 구분 string 또는 list)
 *   - exp / iat: 만료/발급 시각
 */
@Service
class JwtService(private val properties: JwtProperties) {

    private val key = Keys.hmacShaKeyFor(properties.secret.toByteArray())
    private val parser = Jwts.parser().verifyWith(key).requireIssuer(properties.issuer).build()

    fun issue(subject: String, scopes: List<String>): IssuedToken {
        val now = Instant.now()
        val expiry = now.plus(properties.ttlMinutes, ChronoUnit.MINUTES)
        val token = Jwts.builder()
            .subject(subject)
            .issuer(properties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("scopes", scopes.joinToString(" "))
            .signWith(key, Jwts.SIG.HS256)
            .compact()
        return IssuedToken(token = token, issuedAt = now, expiresAt = expiry, scopes = scopes)
    }

    fun parse(token: String): ParsedToken {
        val claims: Claims = try {
            parser.parseSignedClaims(token).payload
        } catch (e: JwtException) {
            throw InvalidJwtException(e.message ?: "invalid token")
        }
        val subject = claims.subject ?: throw InvalidJwtException("missing subject")
        val scopesClaim = claims["scopes"] as? String ?: ""
        val scopes = scopesClaim.split(" ").filter { it.isNotBlank() }
        return ParsedToken(subject = subject, scopes = scopes)
    }
}

data class IssuedToken(
    val token: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val scopes: List<String>,
)

data class ParsedToken(
    val subject: String,
    val scopes: List<String>,
)

class InvalidJwtException(message: String) : RuntimeException(message)
