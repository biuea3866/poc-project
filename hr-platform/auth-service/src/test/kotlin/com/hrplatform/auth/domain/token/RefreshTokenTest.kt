package com.hrplatform.auth.domain.token

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZonedDateTime

class RefreshTokenTest : BehaviorSpec({

    val now = ZonedDateTime.now()

    fun createToken(expiresAt: ZonedDateTime = now.plusDays(14)) = RefreshToken(
        userAccountId = 1L,
        tokenHash = "original-hash",
        accessJti = "test-jti",
        expiresAt = expiresAt,
        deviceInfo = "Chrome/Mac",
        ipAddress = "127.0.0.1",
        revokedAt = null,
        revokedReason = null,
    )

    given("유효한 RefreshToken") {
        `when`("rotate 호출 시") {
            val token = createToken()
            token.rotate("new-hash", "new-jti")
            then("tokenHash가 새 값으로 교체된다") {
                token.tokenHash shouldBe "new-hash"
            }
        }

        `when`("revoke 호출 시") {
            val token = createToken()
            token.revoke("로그아웃", now)
            then("revokedAt이 설정된다") {
                token.revokedAt shouldNotBe null
                token.revokedReason shouldBe "로그아웃"
            }
        }

        `when`("isValid 호출 시") {
            val token = createToken()
            then("true 반환") {
                token.isValid(now) shouldBe true
            }
        }
    }

    given("만료된 RefreshToken") {
        `when`("isExpired 호출 시") {
            val token = createToken(expiresAt = now.minusDays(1))
            then("true 반환") {
                token.isExpired(now) shouldBe true
            }
        }
    }

    given("이미 폐기된 RefreshToken") {
        `when`("revoke 재호출 시") {
            val token = createToken()
            token.revoke("첫 번째 폐기", now)
            then("IllegalStateException 발생") {
                shouldThrow<IllegalStateException> {
                    token.revoke("두 번째 폐기", now)
                }
            }
        }

        `when`("rotate 호출 시") {
            val token = createToken()
            token.revoke("폐기", now)
            then("IllegalStateException 발생") {
                shouldThrow<IllegalStateException> {
                    token.rotate("new-hash", "new-jti")
                }
            }
        }
    }
})
