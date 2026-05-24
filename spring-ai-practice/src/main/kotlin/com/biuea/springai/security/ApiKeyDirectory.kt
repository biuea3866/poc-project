package com.biuea.springai.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * PoC 용 정적 클라이언트 디렉토리. 운영에서는 DB / IAM 으로 대체.
 *
 * application.yml `security.clients` 에 바인딩된다.
 *   security:
 *     clients:
 *       - id: shopper-llm
 *         secret: dev-secret-1
 *         scopes: [catalog:read, order:read]
 */
@ConfigurationProperties(prefix = "security")
data class ClientCatalogProperties(
    val clients: List<ClientCredential> = emptyList(),
) {
    fun find(id: String, secret: String): ClientCredential? =
        clients.firstOrNull { it.id == id && it.secret == secret }
}

data class ClientCredential(
    val id: String,
    val secret: String,
    val scopes: List<String>,
)
