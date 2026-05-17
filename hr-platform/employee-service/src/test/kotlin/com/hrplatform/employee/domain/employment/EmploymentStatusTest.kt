package com.hrplatform.employee.domain.employment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class EmploymentStatusTest : BehaviorSpec({

    given("EmploymentStatus.canTransitTo 상태 전이 테이블") {

        `when`("PRE_HIRED 상태에서") {
            then("ACTIVE로 전이 가능하다") {
                EmploymentStatus.PRE_HIRED.canTransitTo(EmploymentStatus.ACTIVE) shouldBe true
            }
            then("ON_LEAVE로 전이 불가하다") {
                EmploymentStatus.PRE_HIRED.canTransitTo(EmploymentStatus.ON_LEAVE) shouldBe false
            }
            then("RESIGNED로 직접 전이 불가하다") {
                EmploymentStatus.PRE_HIRED.canTransitTo(EmploymentStatus.RESIGNED) shouldBe false
            }
            then("PRE_HIRED로 자기 자신 전이 불가하다") {
                EmploymentStatus.PRE_HIRED.canTransitTo(EmploymentStatus.PRE_HIRED) shouldBe false
            }
        }

        `when`("ACTIVE 상태에서") {
            then("ON_LEAVE로 전이 가능하다") {
                EmploymentStatus.ACTIVE.canTransitTo(EmploymentStatus.ON_LEAVE) shouldBe true
            }
            then("RESIGNED로 전이 가능하다") {
                EmploymentStatus.ACTIVE.canTransitTo(EmploymentStatus.RESIGNED) shouldBe true
            }
            then("PRE_HIRED로 전이 불가하다") {
                EmploymentStatus.ACTIVE.canTransitTo(EmploymentStatus.PRE_HIRED) shouldBe false
            }
            then("ACTIVE로 자기 자신 전이 불가하다") {
                EmploymentStatus.ACTIVE.canTransitTo(EmploymentStatus.ACTIVE) shouldBe false
            }
        }

        `when`("ON_LEAVE 상태에서") {
            then("ACTIVE로 전이 가능하다") {
                EmploymentStatus.ON_LEAVE.canTransitTo(EmploymentStatus.ACTIVE) shouldBe true
            }
            then("RESIGNED로 전이 가능하다") {
                EmploymentStatus.ON_LEAVE.canTransitTo(EmploymentStatus.RESIGNED) shouldBe true
            }
            then("PRE_HIRED로 전이 불가하다") {
                EmploymentStatus.ON_LEAVE.canTransitTo(EmploymentStatus.PRE_HIRED) shouldBe false
            }
            then("ON_LEAVE로 자기 자신 전이 불가하다") {
                EmploymentStatus.ON_LEAVE.canTransitTo(EmploymentStatus.ON_LEAVE) shouldBe false
            }
        }

        `when`("RESIGNED 상태에서") {
            then("어떤 상태로도 전이 불가하다 — PRE_HIRED") {
                EmploymentStatus.RESIGNED.canTransitTo(EmploymentStatus.PRE_HIRED) shouldBe false
            }
            then("어떤 상태로도 전이 불가하다 — ACTIVE") {
                EmploymentStatus.RESIGNED.canTransitTo(EmploymentStatus.ACTIVE) shouldBe false
            }
            then("어떤 상태로도 전이 불가하다 — ON_LEAVE") {
                EmploymentStatus.RESIGNED.canTransitTo(EmploymentStatus.ON_LEAVE) shouldBe false
            }
            then("어떤 상태로도 전이 불가하다 — RESIGNED") {
                EmploymentStatus.RESIGNED.canTransitTo(EmploymentStatus.RESIGNED) shouldBe false
            }
        }
    }
})
