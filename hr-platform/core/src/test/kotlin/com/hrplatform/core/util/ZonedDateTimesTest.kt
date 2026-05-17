package com.hrplatform.core.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ZonedDateTimesTest : BehaviorSpec({

    given("ZonedDateTimes 유틸") {
        `when`("nowUtc()를 호출하면") {
            val result = ZonedDateTimes.nowUtc()

            then("ZoneId가 UTC이다") {
                result.zone shouldBe ZoneOffset.UTC
            }
        }

        `when`("초 단위 ISO-8601 문자열을 parseIso8601로 파싱하면") {
            val isoString = "2026-05-16T09:00:00Z"
            val parsed = ZonedDateTimes.parseIso8601(isoString)

            then("다시 toIso8601로 변환하면 마이크로초까지 채워진 형식이 된다") {
                val roundTrip = ZonedDateTimes.toIso8601(parsed)
                roundTrip shouldBe "2026-05-16T09:00:00.000000Z"
            }
        }

        `when`("마이크로초 정밀도 ZonedDateTime을 toIso8601 → parse 라운드트립 하면") {
            // DATETIME(6)·Kafka occurredAt 정밀도 보존 검증
            val original = ZonedDateTime.of(2026, 5, 17, 4, 12, 45, 123_456_000, ZoneOffset.UTC)
            val serialized = ZonedDateTimes.toIso8601(original)
            val parsed = ZonedDateTimes.parseIso8601(serialized)

            then("문자열에 마이크로초 6자리가 포함된다") {
                serialized shouldBe "2026-05-17T04:12:45.123456Z"
            }

            then("파싱 결과가 원본과 동일하다 (정밀도 손실 0)") {
                parsed shouldBe original
            }
        }

        `when`("nowUtc()를 toIso8601로 변환하면") {
            val result = ZonedDateTimes.toIso8601(ZonedDateTimes.nowUtc())

            then("ISO-8601 형식의 문자열이 반환된다 (마이크로초 포함)") {
                result shouldMatch Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{6}Z""")
            }
        }
    }
})
