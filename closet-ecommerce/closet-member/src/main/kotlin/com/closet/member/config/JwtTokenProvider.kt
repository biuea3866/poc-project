package com.closet.member.config

import com.closet.common.auth.MemberRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

/**
 * JWT 토큰 생성/검증
 */
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.access-token-expiry-ms}")
    private val accessTokenExpiryMs: Long,
    @Value("\${jwt.refresh-token-expiry-ms}")
    private val refreshTokenExpiryMs: Long,
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    /** Access Token 생성 (30분) — role claim 포함 */
    fun generateAccessToken(
        memberId: Long,
        role: MemberRole = MemberRole.BUYER,
    ): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpiryMs)

        return Jwts.builder()
            .subject(memberId.toString())
            .claim("type", "access")
            .claim("role", role.name)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /** Refresh Token 생성 (7일) — role claim 포함 */
    fun generateRefreshToken(
        memberId: Long,
        role: MemberRole = MemberRole.BUYER,
    ): String {
        val now = Date()
        val expiry = Date(now.time + refreshTokenExpiryMs)

        return Jwts.builder()
            .subject(memberId.toString())
            .claim("type", "refresh")
            .claim("role", role.name)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /** 토큰에서 memberId 추출 */
    fun extractMemberId(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    /** 토큰에서 role 추출. claim이 없으면 BUYER 반환 (레거시 하위 호환) */
    fun extractRole(token: String): MemberRole {
        val claims = parseClaims(token)
        val roleStr = claims["role"] as? String
        return MemberRole.fromStringOrDefault(roleStr)
    }

    /** 토큰 유효성 검증 */
    fun validate(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: ExpiredJwtException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
