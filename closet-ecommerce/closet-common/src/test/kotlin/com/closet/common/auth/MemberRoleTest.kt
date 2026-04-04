package com.closet.common.auth

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class MemberRoleTest : BehaviorSpec({

    Given("MemberRole.fromStringOrDefault") {

        When("유효한 role 문자열이 주어지면") {
            Then("해당 MemberRole을 반환한다") {
                MemberRole.fromStringOrDefault("BUYER") shouldBe MemberRole.BUYER
                MemberRole.fromStringOrDefault("SELLER") shouldBe MemberRole.SELLER
                MemberRole.fromStringOrDefault("ADMIN") shouldBe MemberRole.ADMIN
            }
        }

        When("소문자 role 문자열이 주어지면") {
            Then("대소문자 무관하게 해당 MemberRole을 반환한다") {
                MemberRole.fromStringOrDefault("buyer") shouldBe MemberRole.BUYER
                MemberRole.fromStringOrDefault("seller") shouldBe MemberRole.SELLER
                MemberRole.fromStringOrDefault("admin") shouldBe MemberRole.ADMIN
            }
        }

        When("null이 주어지면") {
            Then("기본값 BUYER를 반환한다") {
                MemberRole.fromStringOrDefault(null) shouldBe MemberRole.BUYER
            }
        }

        When("빈 문자열이 주어지면") {
            Then("기본값 BUYER를 반환한다") {
                MemberRole.fromStringOrDefault("") shouldBe MemberRole.BUYER
                MemberRole.fromStringOrDefault("  ") shouldBe MemberRole.BUYER
            }
        }

        When("유효하지 않은 문자열이 주어지면") {
            Then("기본값 BUYER를 반환한다") {
                MemberRole.fromStringOrDefault("UNKNOWN") shouldBe MemberRole.BUYER
                MemberRole.fromStringOrDefault("MANAGER") shouldBe MemberRole.BUYER
            }
        }
    }
})
