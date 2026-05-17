package com.hrplatform.core.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import java.time.ZoneOffset

class ZonedDateTimesTest : BehaviorSpec({

    given("ZonedDateTimes 유틸") {
        `when`("nowUtc()를 호출하면") {
            val result = ZonedDateTimes.nowUtc()

            then("ZoneId가 UTC이다") {
                result.zone shouldBe ZoneOffset.UTC
            }
        }

        `when`("ISO-8601 문자열을 parseIso8601로 파싱하면") {
            val isoString = "2026-05-16T09:00:00Z"
            val parsed = ZonedDateTimes.parseIso8601(isoString)

            then("다시 toIso8601로 변환한 결과가 원본과 동일하다 (round-trip)") {
                val roundTrip = ZonedDateTimes.toIso8601(parsed)
                roundTrip shouldBe isoString
            }
        }

        `when`("nowUtc()를 toIso8601로 변환하면") {
            val result = ZonedDateTimes.toIso8601(ZonedDateTimes.nowUtc())

            then("ISO-8601 형식의 문자열이 반환된다") {
                result shouldMatch Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""")
            }
        }
    }
})
