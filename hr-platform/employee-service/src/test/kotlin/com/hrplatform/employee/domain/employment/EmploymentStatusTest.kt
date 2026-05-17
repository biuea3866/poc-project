package com.hrplatform.employee.domain.employment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class EmploymentStatusTest : BehaviorSpec({

    given("PRE_HIRED 상태") {
        `when`("ACTIVE로 전이") {
            then("성공") {
                EmploymentStatus.PRE_HIRED.canTransitTo(EmploymentStatus.ACTIVE) shouldBe true
            }
        }
        `when`("RESIGNED로 직접 전이") {
            then("차단 — 금지 전이") {
                EmploymentStatus.PRE_HIRED.canTransitTo(EmploymentStatus.RESIGNED) shouldBe false
            }
        }
        `when`("ON_LEAVE로 직접 전이") {
            then("차단 — 금지 전이") {
                EmploymentStatus.PRE_HIRED.canTransitTo(EmploymentStatus.ON_LEAVE) shouldBe false
            }
        }
    }

    given("ACTIVE 상태") {
        `when`("ON_LEAVE로 전이") {
            then("성공") {
                EmploymentStatus.ACTIVE.canTransitTo(EmploymentStatus.ON_LEAVE) shouldBe true
            }
        }
        `when`("RESIGNED로 전이") {
            then("성공") {
                EmploymentStatus.ACTIVE.canTransitTo(EmploymentStatus.RESIGNED) shouldBe true
            }
        }
        `when`("PRE_HIRED로 역행") {
            then("차단") {
                EmploymentStatus.ACTIVE.canTransitTo(EmploymentStatus.PRE_HIRED) shouldBe false
            }
        }
    }

    given("ON_LEAVE 상태") {
        `when`("ACTIVE로 복직") {
            then("성공") {
                EmploymentStatus.ON_LEAVE.canTransitTo(EmploymentStatus.ACTIVE) shouldBe true
            }
        }
        `when`("RESIGNED로 전이") {
            then("성공") {
                EmploymentStatus.ON_LEAVE.canTransitTo(EmploymentStatus.RESIGNED) shouldBe true
            }
        }
        `when`("PRE_HIRED로 역행") {
            then("차단") {
                EmploymentStatus.ON_LEAVE.canTransitTo(EmploymentStatus.PRE_HIRED) shouldBe false
            }
        }
    }

    given("RESIGNED 상태") {
        `when`("ACTIVE로 전이 시도") {
            then("차단 — 퇴사 후 재활성 불가") {
                EmploymentStatus.RESIGNED.canTransitTo(EmploymentStatus.ACTIVE) shouldBe false
            }
        }
        `when`("ON_LEAVE로 전이 시도") {
            then("차단") {
                EmploymentStatus.RESIGNED.canTransitTo(EmploymentStatus.ON_LEAVE) shouldBe false
            }
        }
        `when`("PRE_HIRED로 역행 시도") {
            then("차단") {
                EmploymentStatus.RESIGNED.canTransitTo(EmploymentStatus.PRE_HIRED) shouldBe false
            }
        }
    }
})
