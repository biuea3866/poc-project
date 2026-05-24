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
 * 토큰 발급 엔드포인트.
 *
 *   curl -s localhost:8080/auth/login -H 'Content-Type: application/json' \
 *     -d '{"clientId":"shopper-llm","clientSecret":"dev-secret-1"}'
 *
 * 응답의 access_token 을 Bearer 헤더로 `/sse`, `/mcp/...`, `/api/...` 호출에 사용.
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val clientCatalog: ClientCatalogProperties,
    private val jwtService: JwtService,
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val client = clientCatalog.find(request.clientId, request.clientSecret)
            ?: return ResponseEntity.status(401)
                .body(LoginResponse.error("invalid client credentials"))
        val token = jwtService.issue(subject = client.id, scopes = client.scopes)
        return ResponseEntity.ok(LoginResponse.success(token))
    }
}

data class LoginRequest(
    @field:NotBlank val clientId: String,
    @field:NotBlank val clientSecret: String,
)

data class LoginResponse(
    val accessToken: String?,
    val tokenType: String?,
    val issuedAt: Instant?,
    val expiresAt: Instant?,
    val scopes: List<String>?,
    val error: String?,
) {
    companion object {
        fun success(token: IssuedToken): LoginResponse = LoginResponse(
            accessToken = token.token,
            tokenType = "Bearer",
            issuedAt = token.issuedAt,
            expiresAt = token.expiresAt,
            scopes = token.scopes,
            error = null,
        )

        fun error(message: String): LoginResponse = LoginResponse(
            accessToken = null,
            tokenType = null,
            issuedAt = null,
            expiresAt = null,
            scopes = null,
            error = message,
        )
    }
}
