package com.hrplatform.auth.domain.account

/**
 * 이메일 HMAC-SHA-256 해시 서비스 interface.
 * domain layer에 정의하고, infrastructure layer에서 구현한다.
 * deterministic hash 이므로 동일 이메일은 항상 동일한 hash 값을 반환한다.
 */
interface EmailHashService {
    fun hash(email: String): String
}
