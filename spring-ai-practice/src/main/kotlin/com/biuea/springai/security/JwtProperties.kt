package com.biuea.springai.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT 설정. application.yml `security.jwt.*` 에 바인딩된다.
 *
 * - secret: HS256 서명용 공유 비밀키 (운영에서는 32바이트 이상)
 * - issuer: 토큰 발급자 식별
 * - ttlMinutes: 발급 후 만료까지의 분 단위
 */
@ConfigurationProperties(prefix = "security.jwt")
data class JwtProperties(
    val secret: String,
    val issuer: String = "clothing-ecommerce-mcp",
    val ttlMinutes: Long = 60,
)
