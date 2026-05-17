package com.hrplatform.core.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class Iso4217CurrencyCodesTest : BehaviorSpec({

    given("Iso4217CurrencyCodes.isValid") {
        `when`("KRW가 입력되면") {
            val result = Iso4217CurrencyCodes.isValid("KRW")
            then("true 를 반환한다") {
                result shouldBe true
            }
        }

        `when`("USD가 입력되면") {
            val result = Iso4217CurrencyCodes.isValid("USD")
            then("true 를 반환한다") {
                result shouldBe true
            }
        }

        `when`("JPY가 입력되면") {
            val result = Iso4217CurrencyCodes.isValid("JPY")
            then("true 를 반환한다") {
                result shouldBe true
            }
        }

        `when`("정의되지 않은 ZZZ가 입력되면") {
            val result = Iso4217CurrencyCodes.isValid("ZZZ")
            then("false 를 반환한다") {
                result shouldBe false
            }
        }

        `when`("소문자 krw가 입력되면") {
            val result = Iso4217CurrencyCodes.isValid("krw")
            then("false 를 반환한다 (대문자 강제)") {
                result shouldBe false
            }
        }
    }
})
