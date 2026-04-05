package com.closet.review.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * ReviewStatus 상태 전이 규칙 테스트.
 */
class ReviewStatusTest : BehaviorSpec({

    Given("VISIBLE 상태") {

        When("HIDDEN으로 전이 가능 여부를 확인하면") {
            Then("true") {
                ReviewStatus.VISIBLE.canTransitionTo(ReviewStatus.HIDDEN) shouldBe true
            }
        }

        When("DELETED로 전이 가능 여부를 확인하면") {
            Then("true") {
                ReviewStatus.VISIBLE.canTransitionTo(ReviewStatus.DELETED) shouldBe true
            }
        }

        When("VISIBLE로 전이 가능 여부를 확인하면") {
            Then("false (자기 자신으로 전이 불가)") {
                ReviewStatus.VISIBLE.canTransitionTo(ReviewStatus.VISIBLE) shouldBe false
            }
        }
    }

    Given("HIDDEN 상태") {

        When("VISIBLE로 전이 가능 여부를 확인하면") {
            Then("true") {
                ReviewStatus.HIDDEN.canTransitionTo(ReviewStatus.VISIBLE) shouldBe true
            }
        }

        When("DELETED로 전이 가능 여부를 확인하면") {
            Then("true") {
                ReviewStatus.HIDDEN.canTransitionTo(ReviewStatus.DELETED) shouldBe true
            }
        }
    }

    Given("DELETED 상태") {

        When("어떤 상태로도 전이를 시도하면") {
            Then("모두 false (종단 상태)") {
                ReviewStatus.DELETED.canTransitionTo(ReviewStatus.VISIBLE) shouldBe false
                ReviewStatus.DELETED.canTransitionTo(ReviewStatus.HIDDEN) shouldBe false
                ReviewStatus.DELETED.canTransitionTo(ReviewStatus.DELETED) shouldBe false
            }
        }

        When("validateTransitionTo를 호출하면") {
            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    ReviewStatus.DELETED.validateTransitionTo(ReviewStatus.VISIBLE)
                }
            }
        }
    }
})
