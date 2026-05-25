package com.biuea.springai.security

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * MCP 클라이언트 토큰 발급 엔드포인트 (OAuth2 client_credentials grant 형태).
 *
 * 사용자 로그인이 아니다. MCP 클라이언트(외부 LLM 시스템 — Claude Desktop, MCP Inspector,
 * Python MCP SDK 등) 가 자기 clientId/clientSecret 으로 JWT 를 발급받는다.
 *
 *   curl -s localhost:8080/auth/token -H 'Content-Type: application/json' \
 *     -d '{"clientId":"shopper-llm","clientSecret":"dev-secret-1"}'
 *
 * 응답의 accessToken 을 Bearer 헤더로 `/sse`, `/mcp/...` 호출에 사용.
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val clientCatalog: ClientCatalogProperties,
    private val jwtService: JwtService,
) {

    @PostMapping("/token")
    fun issueToken(@Valid @RequestBody request: TokenRequest): ResponseEntity<TokenResponse> {
        val client = clientCatalog.find(request.clientId, request.clientSecret)
            ?: return ResponseEntity.status(401)
                .body(TokenResponse.error("invalid client credentials"))
        val token = jwtService.issue(subject = client.id, scopes = client.scopes)
        return ResponseEntity.ok(TokenResponse.success(token))
    }
}

data class TokenRequest(
    @field:NotBlank val clientId: String,
    @field:NotBlank val clientSecret: String,
)

data class TokenResponse(
    val accessToken: String?,
    val tokenType: String?,
    val issuedAt: Instant?,
    val expiresAt: Instant?,
    val scopes: List<String>?,
    val error: String?,
) {
    companion object {
        fun success(token: IssuedToken): TokenResponse = TokenResponse(
            accessToken = token.token,
            tokenType = "Bearer",
            issuedAt = token.issuedAt,
            expiresAt = token.expiresAt,
            scopes = token.scopes,
            error = null,
        )

        fun error(message: String): TokenResponse = TokenResponse(
            accessToken = null,
            tokenType = null,
            issuedAt = null,
            expiresAt = null,
            scopes = null,
            error = message,
        )
    }
}
