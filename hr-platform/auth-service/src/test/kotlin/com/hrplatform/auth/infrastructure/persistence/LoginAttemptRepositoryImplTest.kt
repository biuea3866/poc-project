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
        given("동일 emailHash로 5건 실패 저장 후") {
            val emailHash = "a".repeat(64)
            val now = ZonedDateTime.now()
            val since = now.minusMinutes(15)

            repeat(5) {
                loginAttemptRepository.save(
                    LoginAttempt.failure(
                        userAccountId = null,
                        emailHash = emailHash,
                        failureReason = LoginFailureReason.BAD_PASSWORD,
                        ipAddress = "127.0.0.1",
                        userAgent = null,
                        now = now,
                    ),
                )
            }

            `when`("countRecentFailures 조회 시") {
                val count = loginAttemptRepository.countRecentFailures(emailHash, since)
                then("5 반환") {
                    count shouldBe 5
                }
            }

            `when`("findRecentByEmailHash 조회 시") {
                val attempts = loginAttemptRepository.findRecentByEmailHash(emailHash, 10)
                then("5건 반환") {
                    attempts.size shouldBe 5
                }
            }
        }
    }
}
