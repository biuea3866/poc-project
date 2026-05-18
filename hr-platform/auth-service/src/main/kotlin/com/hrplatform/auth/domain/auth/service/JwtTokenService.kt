package com.hrplatform.auth.domain.auth.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenHash: String,
    val refreshTokenExpiresAt: ZonedDateTime,
    val jti: String,
)

/**
 * JWT 발급/검증 내부 컴포넌트.
 * - Access token: 30분, claims: sub=userAccountId, jti=UUID
 * - Refresh token: 14일, SHA-256 hex hash로 DB 저장 (CHAR(64))
 */
@Component
class JwtTokenService(
    @Value("\${hrplatform.jwt.secret}")
    private val base64Secret: String,

    @Value("\${hrplatform.jwt.issuer:hr-platform}")
    private val issuer: String,

    @Value("\${hrplatform.jwt.audience:hr-platform-users}")
    private val audience: String,

    @Value("\${hrplatform.jwt.access-token-expiry-minutes:30}")
    private val accessTokenExpiryMinutes: Long,

    @Value("\${hrplatform.jwt.refresh-token-expiry-days:14}")
    private val refreshTokenExpiryDays: Long,
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret))
    }

    fun issueTokenPair(userAccountId: Long, now: ZonedDateTime): TokenPair {
        val jti = UUID.randomUUID().toString()
        val accessToken = buildAccessToken(userAccountId, jti, now)
        val rawRefreshToken = UUID.randomUUID().toString()
        val refreshTokenHash = sha256(rawRefreshToken)
        val refreshTokenExpiresAt = now.plusDays(refreshTokenExpiryDays)
        return TokenPair(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            refreshTokenHash = refreshTokenHash,
            refreshTokenExpiresAt = refreshTokenExpiresAt,
            jti = jti,
        )
    }

    fun validateAccessToken(token: String): Long {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(issuer)
            .requireAudience(audience)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.subject.toLong()
    }

    fun extractJti(token: String): String {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return requireNotNull(claims.id) { "JWT에 jti가 없습니다" }
    }

    fun hashRefreshToken(rawToken: String): String = sha256(rawToken)

    private fun buildAccessToken(userAccountId: Long, jti: String, now: ZonedDateTime): String {
        val issuedAt = Date.from(now.toInstant())
        val expiry = Date.from(now.plusMinutes(accessTokenExpiryMinutes).toInstant())
        return Jwts.builder()
            .subject(userAccountId.toString())
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(issuedAt)
            .expiration(expiry)
            .id(jti)
            .signWith(secretKey)
            .compact()
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun accessTokenExpirySeconds(): Long = accessTokenExpiryMinutes * 60

    fun refreshTokenExpiryAt(now: ZonedDateTime): ZonedDateTime = now.plusDays(refreshTokenExpiryDays)

    fun nowUtc(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)
}
