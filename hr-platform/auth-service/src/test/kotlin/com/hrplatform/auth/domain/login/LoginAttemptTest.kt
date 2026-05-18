package com.hrplatform.auth.domain.login

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

class LoginAttemptTest : BehaviorSpec({

    val now = ZonedDateTime.now()
    val emailHash = "a".repeat(64)

    given("성공 로그인 시도 생성") {
        `when`("success factory 호출") {
            val attempt = LoginAttempt.success(
                userAccountId = 1L,
                emailHash = emailHash,
                ipAddress = "127.0.0.1",
                userAgent = "Chrome",
                now = now,
            )
            then("success=true, failureReason=null") {
                attempt.success shouldBe true
                attempt.failureReason shouldBe null
                attempt.emailHash shouldBe emailHash
                attempt.userAccountId shouldBe 1L
            }
        }
    }

    given("실패 로그인 시도 생성") {
        `when`("failure factory 호출") {
            val attempt = LoginAttempt.failure(
                userAccountId = null,
                emailHash = emailHash,
                failureReason = LoginFailureReason.EMAIL_NOT_FOUND,
                ipAddress = "10.0.0.1",
                userAgent = null,
                now = now,
            )
            then("success=false, failureReason 설정됨") {
                attempt.success shouldBe false
                attempt.failureReason shouldBe LoginFailureReason.EMAIL_NOT_FOUND
                attempt.userAccountId shouldBe null
            }
        }
    }
})
