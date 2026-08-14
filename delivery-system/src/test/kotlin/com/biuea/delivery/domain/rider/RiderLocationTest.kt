package com.biuea.delivery.domain.rider

import com.biuea.delivery.domain.geo.Coordinate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

class RiderLocationTest : BehaviorSpec({

    val gangnamStation = Coordinate(37.4979, 127.0276)

    given("방금 갱신된 라이더 위치") {
        val location = RiderLocation(1L, gangnamStation, ZonedDateTime.now())

        `when`("신선도를 확인하면") {
            then("배차 후보로 쓸 수 있다") {
                location.isFresh() shouldBe true
            }
        }
    }

    given("29초 전에 갱신된 라이더 위치") {
        val location = RiderLocation(1L, gangnamStation, ZonedDateTime.now().minusSeconds(29))

        `when`("신선도를 확인하면") {
            then("30초 기준을 넘지 않아 여전히 신선하다") {
                location.isFresh() shouldBe true
            }
        }
    }

    given("31초 전에 갱신된 라이더 위치") {
        val location = RiderLocation(1L, gangnamStation, ZonedDateTime.now().minusSeconds(31))

        `when`("신선도를 확인하면") {
            then("오래된 위치로 판정한다") {
                location.isFresh() shouldBe false
            }
        }
    }

    given("0 이하의 라이더 식별자") {
        `when`("위치를 생성하면") {
            then("생성에 실패한다") {
                shouldThrow<IllegalArgumentException> { RiderLocation(0L, gangnamStation, ZonedDateTime.now()) }
            }
        }
    }

    given("31초 전에 갱신된 인근 라이더 검색 결과") {
        val nearbyRider = NearbyRider(1L, gangnamStation, 120.0, ZonedDateTime.now().minusSeconds(31))

        `when`("신선도를 확인하면") {
            then("검색 결과에도 같은 30초 기준이 적용된다") {
                nearbyRider.isFresh() shouldBe false
            }
        }
    }
})
