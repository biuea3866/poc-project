package com.hrplatform.auth.domain.token

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZonedDateTime

class ApiTokenTest : BehaviorSpec({

    val now = ZonedDateTime.now()

    fun createToken(expiresAt: ZonedDateTime? = now.plusDays(365)) = ApiToken(
        userAccountId = 1L,
        name = "CI Token",
        tokenHash = "sha256-hash",
        scopes = listOf("read", "write"),
        expiresAt = expiresAt,
        lastUsedAt = null,
        revokedAt = null,
    )

    given("활성 ApiToken") {
        `when`("isActive 확인") {
            val token = createToken()
            then("true 반환") {
                token.isActive(now) shouldBe true
            }
        }

        `when`("recordUsage 호출") {
            val token = createToken()
            token.recordUsage(now)
            then("lastUsedAt이 설정된다") {
                token.lastUsedAt shouldBe now
            }
        }

        `when`("revoke 호출") {
            val token = createToken()
            token.revoke(now)
            then("revokedAt이 설정되고 isActive=false") {
                token.revokedAt shouldNotBe null
                token.isActive(now) shouldBe false
            }
        }
    }

    given("만료된 ApiToken") {
        `when`("isActive 확인") {
            val token = createToken(expiresAt = now.minusDays(1))
            then("false 반환") {
                token.isActive(now) shouldBe false
            }
        }
    }

    given("만료 없는 ApiToken") {
        `when`("isExpired 확인") {
            val token = createToken(expiresAt = null)
            then("false 반환") {
                token.isExpired(now) shouldBe false
            }
        }
    }

    given("이미 폐기된 ApiToken") {
        `when`("revoke 재호출") {
            val token = createToken()
            token.revoke(now)
            then("IllegalStateException 발생") {
                shouldThrow<IllegalStateException> {
                    token.revoke(now)
                }
            }
        }
    }
})
