package com.hrplatform.auth.domain.account

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class UserAccountStatusTest : BehaviorSpec({

    given("UserAccountStatus 전이 매트릭스") {

        `when`("ACTIVE 상태에서") {
            then("LOCKED으로 전이 가능") {
                UserAccountStatus.ACTIVE.canTransitTo(UserAccountStatus.LOCKED) shouldBe true
            }
            then("SUSPENDED으로 전이 가능") {
                UserAccountStatus.ACTIVE.canTransitTo(UserAccountStatus.SUSPENDED) shouldBe true
            }
            then("DEACTIVATED으로 전이 가능") {
                UserAccountStatus.ACTIVE.canTransitTo(UserAccountStatus.DEACTIVATED) shouldBe true
            }
            then("ACTIVE 자기 자신으로 전이 불가") {
                UserAccountStatus.ACTIVE.canTransitTo(UserAccountStatus.ACTIVE) shouldBe false
            }
        }

        `when`("LOCKED 상태에서") {
            then("ACTIVE으로 전이 가능") {
                UserAccountStatus.LOCKED.canTransitTo(UserAccountStatus.ACTIVE) shouldBe true
            }
            then("SUSPENDED으로 전이 불가") {
                UserAccountStatus.LOCKED.canTransitTo(UserAccountStatus.SUSPENDED) shouldBe false
            }
            then("DEACTIVATED으로 전이 불가") {
                UserAccountStatus.LOCKED.canTransitTo(UserAccountStatus.DEACTIVATED) shouldBe false
            }
        }

        `when`("SUSPENDED 상태에서") {
            then("ACTIVE으로 전이 가능") {
                UserAccountStatus.SUSPENDED.canTransitTo(UserAccountStatus.ACTIVE) shouldBe true
            }
            then("DEACTIVATED으로 전이 가능") {
                UserAccountStatus.SUSPENDED.canTransitTo(UserAccountStatus.DEACTIVATED) shouldBe true
            }
            then("LOCKED으로 전이 불가") {
                UserAccountStatus.SUSPENDED.canTransitTo(UserAccountStatus.LOCKED) shouldBe false
            }
        }

        `when`("DEACTIVATED 상태에서") {
            then("모든 상태로 전이 불가") {
                UserAccountStatus.entries.forEach { target ->
                    UserAccountStatus.DEACTIVATED.canTransitTo(target) shouldBe false
                }
            }
        }
    }
})
