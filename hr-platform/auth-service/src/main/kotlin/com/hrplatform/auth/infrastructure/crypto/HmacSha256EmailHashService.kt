package com.hrplatform.auth.infrastructure.crypto

import com.hrplatform.auth.domain.account.EmailHashService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA-256 기반 이메일 해시 구현체.
 * deterministic — 동일 이메일 + 동일 secret => 동일 hex 문자열(64자).
 * DB lookup용 email_hash 컬럼 값 생성에 사용한다.
 */
@Component
class HmacSha256EmailHashService(
    @Value("\${hrplatform.auth.email-hash-secret}")
    private val secret: String,
) : EmailHashService {

    init {
        require(secret.isNotBlank()) { "HRPLATFORM_AUTH_EMAIL_HASH_SECRET 환경변수 필수" }
    }

    override fun hash(email: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(email.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
