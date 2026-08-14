package com.biuea.delivery.infrastructure.geo

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.RiderLocation
import com.biuea.delivery.domain.rider.RiderLocationIndex
import com.biuea.delivery.infrastructure.geo.RiderLocationIndexes.northOf
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * 네 인덱스 구현이 동일한 계약을 만족하는지 하나의 시나리오로 검증한다.
 * 구현별로 결과가 갈리면 배차 결과가 인프라 선택에 따라 달라진다는 뜻이라 반드시 같아야 한다.
 */
class RiderLocationIndexContractTest : FunSpec({

    val stringRedisTemplate = RiderLocationIndexes.createStringRedisTemplate()
    val indexes = RiderLocationIndexes.createAll(stringRedisTemplate)
    val gangnamStation = Coordinate(37.4979, 127.0276)
    val searchRadiusMeters = 3_000
    val searchLimit = 500

    fun locationOf(riderId: Long, coordinate: Coordinate, secondsAgo: Long = 0): RiderLocation =
        RiderLocation(riderId, coordinate, ZonedDateTime.now().minusSeconds(secondsAgo))

    indexes.forEach { (implementationName, riderLocationIndex) ->
        context("$implementationName 인덱스") {

            test("반경 안 라이더만 거리 오름차순으로 반환한다") {
                riderLocationIndex.clear()
                riderLocationIndex.update(locationOf(1L, northOf(gangnamStation, 200.0)))
                riderLocationIndex.update(locationOf(2L, northOf(gangnamStation, 1_200.0)))
                riderLocationIndex.update(locationOf(3L, northOf(gangnamStation, 2_500.0)))
                riderLocationIndex.update(locationOf(4L, northOf(gangnamStation, 4_000.0)))
                riderLocationIndex.update(locationOf(5L, northOf(gangnamStation, 12_000.0)))

                val riders = riderLocationIndex.searchWithin(gangnamStation, searchRadiusMeters, searchLimit)

                riders.map { it.riderId } shouldContainExactly listOf(1L, 2L, 3L)
                riders.first().distanceMeters shouldBe (200.0 plusOrMinus 5.0)
                riders.map { it.distanceMeters } shouldContainExactly riders.map { it.distanceMeters }.sorted()
            }

            test("limit 만큼만 가까운 순으로 자른다") {
                riderLocationIndex.clear()
                riderLocationIndex.update(locationOf(1L, northOf(gangnamStation, 200.0)))
                riderLocationIndex.update(locationOf(2L, northOf(gangnamStation, 1_200.0)))
                riderLocationIndex.update(locationOf(3L, northOf(gangnamStation, 2_500.0)))

                val riders = riderLocationIndex.searchWithin(gangnamStation, searchRadiusMeters, 2)

                riders.map { it.riderId } shouldContainExactly listOf(1L, 2L)
            }

            test("위치를 갱신하면 이전 위치는 인덱스에서 사라진다") {
                riderLocationIndex.clear()
                riderLocationIndex.update(locationOf(9L, northOf(gangnamStation, 12_000.0)))
                riderLocationIndex.searchWithin(gangnamStation, searchRadiusMeters, searchLimit).shouldBeEmpty()

                riderLocationIndex.update(locationOf(9L, northOf(gangnamStation, 300.0)))
                riderLocationIndex.searchWithin(gangnamStation, searchRadiusMeters, searchLimit)
                    .map { it.riderId } shouldContainExactly listOf(9L)

                riderLocationIndex.update(locationOf(9L, northOf(gangnamStation, 12_000.0)))
                riderLocationIndex.searchWithin(gangnamStation, searchRadiusMeters, searchLimit).shouldBeEmpty()
            }

            test("셀 경계 건너편 79m 라이더도 누락하지 않는다") {
                // 두 좌표는 geohash 정밀도 7 기준 wydm6d6 / wydm6d7 로 셀이 갈린다.
                riderLocationIndex.clear()
                val storeSide = Coordinate(37.4979, 127.0276)
                riderLocationIndex.update(locationOf(7L, Coordinate(37.4979, 127.0285)))

                val riders = riderLocationIndex.searchWithin(storeSide, searchRadiusMeters, searchLimit)

                riders.map { it.riderId } shouldContainExactly listOf(7L)
                riders.first().distanceMeters shouldBe (79.4 plusOrMinus 2.0)
            }

            test("오래된 위치도 그대로 반환하고 신선도 판정은 도메인에 맡긴다") {
                riderLocationIndex.clear()
                riderLocationIndex.update(locationOf(1L, northOf(gangnamStation, 200.0), secondsAgo = 60))

                val riders = riderLocationIndex.searchWithin(gangnamStation, searchRadiusMeters, searchLimit)

                riders.map { it.riderId } shouldContainExactly listOf(1L)
                riders.first().isFresh() shouldBe false
            }

            test("candidateCount 는 반경 안 라이더 수 이상이다") {
                riderLocationIndex.clear()
                riderLocationIndex.update(locationOf(1L, northOf(gangnamStation, 200.0)))
                riderLocationIndex.update(locationOf(2L, northOf(gangnamStation, 1_200.0)))
                riderLocationIndex.update(locationOf(3L, northOf(gangnamStation, 2_500.0)))

                riderLocationIndex.candidateCount(gangnamStation, searchRadiusMeters) shouldBeGreaterThanOrEqual 3
            }

            test("clear 하면 결과가 비어 있다") {
                riderLocationIndex.update(locationOf(1L, northOf(gangnamStation, 200.0)))
                riderLocationIndex.clear()

                riderLocationIndex.searchWithin(gangnamStation, searchRadiusMeters, searchLimit).shouldBeEmpty()
                riderLocationIndex.candidateCount(gangnamStation, searchRadiusMeters) shouldBe 0
            }
        }
    }

    test("네 구현이 같은 무작위 데이터에서 반경별로 같은 라이더 집합을 반환한다") {
        val random = Random(2026)
        val riderLocations = (1L..300L).map { riderId ->
            RiderLocation(
                riderId,
                Coordinate(
                    37.4979 + (random.nextDouble() - 0.5) * 0.18,
                    127.0276 + (random.nextDouble() - 0.5) * 0.22,
                ),
                ZonedDateTime.now(),
            )
        }
        indexes.forEach { (_, riderLocationIndex) ->
            riderLocationIndex.clear()
            riderLocations.forEach { riderLocationIndex.update(it) }
        }

        // 반경 단계 확장(1.5km → 3km → 7km)마다 셀 정밀도·H3 해상도가 바뀌므로 각 단계를 모두 검증한다.
        listOf(1_500, 3_000, 7_000).forEach { radiusMeters ->
            val resultsByImplementation: Map<String, List<NearbyRider>> = indexes.associate { (name, index) ->
                name to index.searchWithin(gangnamStation, radiusMeters, searchLimit)
            }
            val expectedRiderIds = resultsByImplementation.getValue("FullScan").map { it.riderId }.sorted()

            resultsByImplementation.forEach { (implementationName, riders) ->
                withClue("반경 ${radiusMeters}m 에서 $implementationName 결과가 전수 스캔과 달라졌다") {
                    riders.map { it.riderId }.sorted() shouldContainExactly expectedRiderIds
                    riders.map { it.distanceMeters } shouldContainExactly riders.map { it.distanceMeters }.sorted()
                }
            }
        }
    }

    test("Geohash 인덱스는 정밀도 5 셀 경계에 걸친 라이더도 3km 검색에서 찾는다") {
        val geohashIndex: RiderLocationIndex = indexes.first { it.first == "Geohash" }.second
        geohashIndex.clear()
        val storeSide = Coordinate(37.4979, 127.0454)
        geohashIndex.update(RiderLocation(1L, Coordinate(37.4979, 127.0464), ZonedDateTime.now()))

        val riders = geohashIndex.searchWithin(storeSide, searchRadiusMeters, searchLimit)

        riders.map { it.riderId } shouldContain 1L
    }
})
