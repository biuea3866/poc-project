package com.biuea.delivery.domain.geo

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random

class GeohashTest : BehaviorSpec({

    given("geohash 공개 테스트 벡터 좌표 (57.64911, 10.40744)") {
        val coordinate = Coordinate(57.64911, 10.40744)

        `when`("정밀도 11 로 인코딩하면") {
            val encoded = Geohash.encode(coordinate, 11)

            then("알려진 값 u4pruydqqvj 와 일치한다") {
                encoded shouldBe "u4pruydqqvj"
            }
        }

        `when`("정밀도를 낮춰 인코딩하면") {
            then("낮은 정밀도 해시는 높은 정밀도 해시의 접두사다") {
                Geohash.encode(coordinate, 5) shouldBe "u4pru"
                Geohash.encode(coordinate, 11).startsWith(Geohash.encode(coordinate, 5)) shouldBe true
            }
        }
    }

    given("서울시청 좌표") {
        val seoulCityHall = Coordinate(37.5663, 126.9779)

        `when`("정밀도 7 로 인코딩하면") {
            then("wydm9qw 다") {
                Geohash.encode(seoulCityHall, 7) shouldBe "wydm9qw"
            }
        }

        `when`("인코딩 후 디코딩하면") {
            val cell = Geohash.decode(Geohash.encode(seoulCityHall, 7))

            then("원래 좌표가 셀 경계 안에 들어간다") {
                cell.contains(seoulCityHall) shouldBe true
            }

            then("셀 중심과 원래 좌표의 거리는 셀 크기(153m)보다 작다") {
                cell.center.distanceMetersTo(seoulCityHall) shouldBeLessThan 153.0
            }
        }

        `when`("이웃 셀을 구하면") {
            val neighbors = Geohash.neighbors(Geohash.encode(seoulCityHall, 7))

            then("자기 자신을 제외한 인접 8셀이 나온다") {
                neighbors shouldHaveSize 8
                neighbors.toSet() shouldHaveSize 8
                neighbors shouldNotBe listOf(Geohash.encode(seoulCityHall, 7))
            }

            then("모든 이웃 셀은 같은 정밀도다") {
                neighbors.forEach { neighbor -> neighbor.length shouldBe 7 }
            }
        }
    }

    given("반경에 맞는 정밀도 계산") {
        `when`("3km 반경을 요청하면") {
            then("셀 한 변이 3km 이상인 정밀도 5 를 쓴다") {
                Geohash.precisionFor(3_000, 37.5) shouldBe 5
            }
        }

        `when`("500m·100m 반경을 요청하면") {
            then("반경이 좁을수록 정밀도가 올라간다") {
                Geohash.precisionFor(500, 37.5) shouldBe 6
                Geohash.precisionFor(100, 37.5) shouldBe 7
            }
        }
    }

    given("반경 3km 안에 결정적으로 흩어진 좌표 500개") {
        val center = Coordinate(37.4979, 127.0276)
        val radiusMeters = 3_000
        val precision = Geohash.precisionFor(radiusMeters, center.latitude)
        val centerCell = Geohash.encode(center, precision)
        val searchCells = (Geohash.neighbors(centerCell) + centerCell).toSet()
        val random = Random(7)
        val points = (1..500).map {
            val latitudeDelta = (random.nextDouble() - 0.5) * 2 * radiusMeters / 111_320.0
            val longitudeDelta = (random.nextDouble() - 0.5) * 2 * radiusMeters / 88_300.0
            Coordinate(center.latitude + latitudeDelta, center.longitude + longitudeDelta)
        }.filter { it.distanceMetersTo(center) <= radiusMeters }

        `when`("중심 셀 + 이웃 8셀 로 조회하면") {
            then("반경 안 좌표가 하나도 누락되지 않는다") {
                points.forEach { point ->
                    searchCells shouldContain Geohash.encode(point, precision)
                }
            }
        }
    }

    given("정밀도 7 셀 경계를 사이에 둔 79m 거리의 두 좌표") {
        // 강남역 인근 실제 좌표. 경계 경도 127.028045654296875 를 사이에 두고 40m 씩 떨어져 있다.
        val storeSide = Coordinate(37.4979, 127.0276)
        val riderSide = Coordinate(37.4979, 127.0285)

        `when`("두 좌표의 거리를 재면") {
            then("100m 이내로 사실상 같은 골목이다") {
                storeSide.distanceMetersTo(riderSide) shouldBeLessThan 100.0
            }
        }

        `when`("같은 정밀도로 인코딩하면") {
            val storeCell = Geohash.encode(storeSide, 7)
            val riderCell = Geohash.encode(riderSide, 7)

            then("접두사가 달라져 서로 다른 셀에 속한다 — 중심 셀만 조회하면 누락된다") {
                storeCell shouldBe "wydm6d6"
                riderCell shouldBe "wydm6d7"
                storeCell shouldNotBe riderCell
            }

            then("이웃 8셀까지 조회하면 찾아진다") {
                Geohash.neighbors(storeCell) shouldContain riderCell
            }
        }
    }

    given("정밀도 5 셀 경계를 사이에 둔 88m 거리의 두 좌표") {
        // 3km 반경 검색이 쓰는 정밀도 5 에서도 같은 경계 문제가 재현된다.
        val storeSide = Coordinate(37.4979, 127.0454)
        val riderSide = Coordinate(37.4979, 127.0464)

        `when`("정밀도 5 로 인코딩하면") {
            val storeCell = Geohash.encode(storeSide, 5)
            val riderCell = Geohash.encode(riderSide, 5)

            then("88m 거리인데 셀이 갈린다") {
                storeSide.distanceMetersTo(riderSide) shouldBeLessThan 100.0
                storeCell shouldBe "wydm6"
                riderCell shouldBe "wydm7"
            }

            then("이웃 8셀까지 조회하면 찾아진다") {
                Geohash.neighbors(storeCell) shouldContain riderCell
            }
        }
    }
})
