package com.hrplatform.auth.infrastructure.persistence

import com.hrplatform.auth.domain.login.LoginAttempt
import com.hrplatform.auth.domain.login.LoginAttemptRepository
import com.hrplatform.auth.domain.login.LoginFailureReason
import com.hrplatform.auth.support.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime

class LoginAttemptRepositoryImplTest(
    @Autowired private val loginAttemptRepository: LoginAttemptRepository,
) : BaseIntegrationTest() {

    init {
        given("동일 이메일에 5건 실패 저장 후") {
            val email = "lockme@example.com"
            val now = ZonedDateTime.now()
            val since = now.minusMinutes(15)

            repeat(5) {
                loginAttemptRepository.save(
                    LoginAttempt.failure(
                        userAccountId = null,
                        email = email,
                        failureReason = LoginFailureReason.BAD_PASSWORD,
                        ipAddress = "127.0.0.1",
                        userAgent = null,
                        now = now,
                    ),
                )
            }

            `when`("countRecentFailures 조회 시") {
                val count = loginAttemptRepository.countRecentFailures(email, since)
                then("5 반환") {
                    count shouldBe 5
                }
            }

            `when`("findRecentByEmail 조회 시") {
                val attempts = loginAttemptRepository.findRecentByEmail(email, 10)
                then("5건 반환") {
                    attempts.size shouldBe 5
                }
            }
        }
    }
}
