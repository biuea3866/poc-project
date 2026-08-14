package com.biuea.delivery.domain.rider

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class VehicleTypeTest : BehaviorSpec({

    given("이동 수단별 기본 탐색 반경") {
        `when`("도보를 조회하면") {
            then("1.5km 다") {
                VehicleType.WALK.defaultSearchRadiusMeters shouldBe 1_500
            }
        }

        `when`("자전거를 조회하면") {
            then("3km 다") {
                VehicleType.BICYCLE.defaultSearchRadiusMeters shouldBe 3_000
            }
        }

        `when`("오토바이를 조회하면") {
            then("5km 다") {
                VehicleType.MOTORCYCLE.defaultSearchRadiusMeters shouldBe 5_000
            }
        }
    }

    given("자전거 라이더") {
        `when`("반경 확장 사다리를 조회하면") {
            val ladder = VehicleType.BICYCLE.searchRadiusLadderMeters()

            then("3km → 5km → 7km 순서로 확장한다") {
                ladder shouldContainExactly listOf(3_000, 5_000, 7_000)
            }
        }
    }

    given("도보 라이더") {
        `when`("반경 확장 사다리를 조회하면") {
            val ladder = VehicleType.WALK.searchRadiusLadderMeters()

            then("기본 반경에서 2km 씩 두 번 확장한다") {
                ladder shouldContainExactly listOf(1_500, 3_500, 5_500)
            }
        }
    }

    given("모든 이동 수단") {
        `when`("반경 확장 사다리를 조회하면") {
            then("첫 단계는 항상 기본 반경이고 오름차순이다") {
                VehicleType.entries.forEach { vehicleType ->
                    val ladder = vehicleType.searchRadiusLadderMeters()
                    ladder.first() shouldBe vehicleType.defaultSearchRadiusMeters
                    ladder shouldContainExactly ladder.sorted()
                }
            }
        }
    }
})
