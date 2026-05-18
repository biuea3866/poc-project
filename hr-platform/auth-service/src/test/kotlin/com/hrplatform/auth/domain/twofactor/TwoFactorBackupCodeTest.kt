package com.hrplatform.auth.domain.twofactor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

class TwoFactorBackupCodeTest : BehaviorSpec({

    val now = ZonedDateTime.now()

    fun createCode() = TwoFactorBackupCode(
        userAccountId = 1L,
        codeHash = "bcrypt-hash",
        usedAt = null,
    )

    given("미사용 백업 코드") {
        `when`("use 호출 시") {
            val code = createCode()
            code.use(now)
            then("usedAt이 설정된다") {
                code.usedAt shouldBe now
                code.isUsed() shouldBe true
            }
        }
    }

    given("이미 사용된 백업 코드") {
        `when`("use 재호출 시") {
            val code = createCode()
            code.use(now)

            then("BackupCodeAlreadyUsedException 발생한다") {
                shouldThrow<BackupCodeAlreadyUsedException> {
                    code.use(now)
                }
            }
        }
    }
})
