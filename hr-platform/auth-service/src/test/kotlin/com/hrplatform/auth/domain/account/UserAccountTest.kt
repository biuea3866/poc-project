package com.hrplatform.auth.domain.account

import com.hrplatform.auth.domain.account.event.UserLockedEvent
import com.hrplatform.auth.domain.account.event.UserPasswordChangedEvent
import com.hrplatform.auth.domain.account.event.UserTwoFactorDisabledEvent
import com.hrplatform.auth.domain.account.event.UserTwoFactorEnrolledEvent
import com.hrplatform.auth.domain.account.event.UserUnlockedEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import java.time.ZonedDateTime

class UserAccountTest : BehaviorSpec({

    val now = ZonedDateTime.now()

    fun createActiveAccount() = UserAccount.create(
        employmentId = 1L,
        companyId = 10L,
        email = "test@example.com",
        passwordHash = "hashed",
    )

    given("ACTIVE 계정에 로그인 실패를 반복할 때") {
        `when`("5회 실패 시") {
            val account = createActiveAccount()
            repeat(5) { account.recordFailedAttempt(now) }

            then("status=LOCKED로 전이된다") {
                account.status shouldBe UserAccountStatus.LOCKED
            }
            then("lockedUntil이 15분 후로 설정된다") {
                account.lockedUntil shouldNotBe null
                account.lockedUntil!!.isAfter(now) shouldBe true
            }
            then("UserLockedEvent가 1건 적재되고 failedAttempts=5이다") {
                val events = account.pullDomainEvents()
                events shouldHaveSize 1
                events[0] should beInstanceOf<UserLockedEvent>()
                (events[0] as UserLockedEvent).failedAttempts shouldBe 5
            }
        }

        `when`("4회 실패 시") {
            val account = createActiveAccount()
            repeat(4) { account.recordFailedAttempt(now) }

            then("status=ACTIVE 유지") {
                account.status shouldBe UserAccountStatus.ACTIVE
                account.failedLoginAttempts shouldBe 4
            }
            then("이벤트 없음") {
                account.pullDomainEvents() shouldHaveSize 0
            }
        }
    }

    given("LOCKED 계정에 tryAutoUnlock 호출 시") {
        `when`("lockedUntil이 이미 지났으면") {
            val account = createActiveAccount()
            repeat(5) { account.recordFailedAttempt(now) }
            account.pullDomainEvents()

            val futureNow = now.plusMinutes(16)
            val unlocked = account.tryAutoUnlock(futureNow)

            then("true 반환 + ACTIVE 전이") {
                unlocked shouldBe true
                account.status shouldBe UserAccountStatus.ACTIVE
            }
            then("UserUnlocked(trigger=AUTO) 이벤트 적재") {
                val events = account.pullDomainEvents()
                events shouldHaveSize 1
                (events[0] as UserUnlockedEvent).trigger shouldBe "AUTO"
            }
        }

        `when`("lockedUntil이 아직 지나지 않았으면") {
            val account = createActiveAccount()
            repeat(5) { account.recordFailedAttempt(now) }
            account.pullDomainEvents()

            val earlyNow = now.plusMinutes(1)
            val unlocked = account.tryAutoUnlock(earlyNow)

            then("false 반환 + LOCKED 유지") {
                unlocked shouldBe false
                account.status shouldBe UserAccountStatus.LOCKED
            }
        }
    }

    given("LOCKED 계정에 수동 unlock 호출") {
        `when`("관리자가 수동 해제하면") {
            val account = createActiveAccount()
            repeat(5) { account.recordFailedAttempt(now) }
            account.pullDomainEvents()
            account.unlock(actorEmploymentId = 99L, now = now)

            then("ACTIVE 전이 + UserUnlocked(MANUAL) 이벤트") {
                account.status shouldBe UserAccountStatus.ACTIVE
                val events = account.pullDomainEvents()
                events shouldHaveSize 1
                (events[0] as UserUnlockedEvent).trigger shouldBe "MANUAL"
            }
        }
    }

    given("DEACTIVATED 계정에 상태 전이 시도") {
        `when`("suspend 호출 시") {
            val account = createActiveAccount()
            account.deactivate("퇴사", null, now)
            account.pullDomainEvents()

            then("IllegalStateTransitionException 발생") {
                shouldThrow<IllegalStateTransitionException> {
                    account.suspend("테스트", null, now)
                }
            }
        }

        `when`("reactivate 호출 시") {
            val account = createActiveAccount()
            account.deactivate("퇴사", null, now)
            account.pullDomainEvents()

            then("IllegalStateTransitionException 발생") {
                shouldThrow<IllegalStateTransitionException> {
                    account.reactivate(null, now)
                }
            }
        }
    }

    given("비밀번호 변경") {
        `when`("ACTIVE 계정에서 changePassword 호출") {
            val account = createActiveAccount()
            account.changePassword("newHash", "SELF_CHANGE", null, now)

            then("passwordHash 변경 + UserPasswordChangedEvent 적재") {
                account.passwordHash shouldBe "newHash"
                val events = account.pullDomainEvents()
                events shouldHaveSize 1
                (events[0] as UserPasswordChangedEvent).trigger shouldBe "SELF_CHANGE"
            }
        }
    }

    given("2FA 등록/해제") {
        `when`("enrollTwoFactor 호출") {
            val account = createActiveAccount()
            account.enrollTwoFactor("encrypted-secret", null, now)

            then("twoFactorEnabled=true + 이벤트 적재") {
                account.twoFactorEnabled shouldBe true
                account.twoFactorSecret shouldBe "encrypted-secret"
                val events = account.pullDomainEvents()
                events shouldHaveSize 1
                events[0] should beInstanceOf<UserTwoFactorEnrolledEvent>()
            }
        }

        `when`("disableTwoFactor 호출") {
            val account = createActiveAccount()
            account.enrollTwoFactor("secret", null, now)
            account.pullDomainEvents()
            account.disableTwoFactor(null, now)

            then("twoFactorEnabled=false + 이벤트 적재") {
                account.twoFactorEnabled shouldBe false
                account.twoFactorSecret shouldBe null
                val events = account.pullDomainEvents()
                events shouldHaveSize 1
                events[0] should beInstanceOf<UserTwoFactorDisabledEvent>()
            }
        }
    }

    given("성공 로그인") {
        `when`("recordSuccessfulLogin 호출") {
            val account = createActiveAccount()
            account.recordFailedAttempt(now)
            account.recordFailedAttempt(now)
            account.recordSuccessfulLogin(now)

            then("failedLoginAttempts 초기화 + lastLoginAt 설정") {
                account.failedLoginAttempts shouldBe 0
                account.lastLoginAt shouldBe now
            }
        }
    }
})
