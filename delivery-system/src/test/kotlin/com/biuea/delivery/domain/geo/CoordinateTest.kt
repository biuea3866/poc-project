package com.biuea.delivery.domain.geo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class CoordinateTest : BehaviorSpec({

    given("서울시청과 강남역 좌표") {
        val seoulCityHall = Coordinate(37.5663, 126.9779)
        val gangnamStation = Coordinate(37.4979, 127.0276)

        `when`("두 좌표 사이 거리를 계산하면") {
            val distanceMeters = seoulCityHall.distanceMetersTo(gangnamStation)

            then("실제 직선 거리 8,780m 와 오차 1% 이내다") {
                // 하버사인 공식의 정확도 기준. 지구를 구로 근사하므로 타원체 기준 대비 0.3% 내외 오차가 난다.
                distanceMeters shouldBe (8_780.0 plusOrMinus 87.8)
            }

            then("거리는 방향과 무관하게 같다") {
                gangnamStation.distanceMetersTo(seoulCityHall) shouldBe (distanceMeters plusOrMinus 0.000_1)
            }
        }
    }

    given("동일한 좌표") {
        val gangnamStation = Coordinate(37.4979, 127.0276)

        `when`("자기 자신과의 거리를 계산하면") {
            val distanceMeters = gangnamStation.distanceMetersTo(gangnamStation)

            then("0m 다") {
                distanceMeters shouldBe (0.0 plusOrMinus 0.000_1)
            }
        }
    }

    given("경도 1도 차이가 나는 적도 위 두 좌표") {
        val origin = Coordinate(0.0, 0.0)
        val oneDegreeEast = Coordinate(0.0, 1.0)

        `when`("거리를 계산하면") {
            val distanceMeters = origin.distanceMetersTo(oneDegreeEast)

            then("적도 기준 경도 1도인 111.2km 와 오차 1% 이내다") {
                distanceMeters shouldBe (111_195.0 plusOrMinus 1_112.0)
            }
        }
    }

    given("위도 범위를 벗어난 값") {
        `when`("좌표를 생성하면") {
            then("생성에 실패한다") {
                shouldThrow<IllegalArgumentException> { Coordinate(90.1, 127.0) }
                shouldThrow<IllegalArgumentException> { Coordinate(-90.1, 127.0) }
            }
        }
    }

    given("경도 범위를 벗어난 값") {
        `when`("좌표를 생성하면") {
            then("생성에 실패한다") {
                shouldThrow<IllegalArgumentException> { Coordinate(37.5, 180.1) }
                shouldThrow<IllegalArgumentException> { Coordinate(37.5, -180.1) }
            }
        }
    }

    given("경계값인 위도 90도, 경도 180도") {
        `when`("좌표를 생성하면") {
            then("정상 생성된다") {
                Coordinate(90.0, 180.0).latitude shouldBe 90.0
                Coordinate(-90.0, -180.0).longitude shouldBe -180.0
            }
        }
    }
})
