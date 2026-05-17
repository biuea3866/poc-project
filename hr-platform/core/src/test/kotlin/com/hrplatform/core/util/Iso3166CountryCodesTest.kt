package com.hrplatform.core.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class Iso3166CountryCodesTest : BehaviorSpec({

    given("ISO 3166-1 alpha-2 코드 유효성 검사") {
        `when`("유효한 코드 'KR'을 전달하면") {
            then("true를 반환한다") {
                Iso3166CountryCodes.isValid("KR") shouldBe true
            }
        }

        `when`("유효하지 않은 코드 'XX'를 전달하면") {
            then("false를 반환한다") {
                Iso3166CountryCodes.isValid("XX") shouldBe false
            }
        }

        `when`("유효한 코드 'US'를 전달하면") {
            then("true를 반환한다") {
                Iso3166CountryCodes.isValid("US") shouldBe true
            }
        }

        `when`("소문자 코드 'kr'을 전달하면") {
            then("false를 반환한다 (대문자만 유효)") {
                Iso3166CountryCodes.isValid("kr") shouldBe false
            }
        }

        `when`("빈 문자열을 전달하면") {
            then("false를 반환한다") {
                Iso3166CountryCodes.isValid("") shouldBe false
            }
        }
    }
})
