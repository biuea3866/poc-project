package com.biuea.delivery.domain.rider

import com.biuea.delivery.domain.geo.Coordinate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class RiderSearchDomainServiceTest : BehaviorSpec({

    val storeCoordinate = Coordinate(37.4979, 127.0276)
    val searchLimit = 10

    fun freshRider(riderId: Long, distanceMeters: Double): NearbyRider =
        NearbyRider(riderId, storeCoordinate, distanceMeters, ZonedDateTime.now())

    fun staleRider(riderId: Long, distanceMeters: Double): NearbyRider =
        NearbyRider(riderId, storeCoordinate, distanceMeters, ZonedDateTime.now().minusSeconds(31))

    given("기본 반경 3km 안에 신선한 라이더가 5명 있는 자전거 배차") {
        val riderLocationIndex = mockk<RiderLocationIndex>()
        every { riderLocationIndex.searchWithin(storeCoordinate, 3_000, searchLimit) } returns
            (1L..5L).map { freshRider(it, it * 100.0) }
        val riderSearchDomainService = RiderSearchDomainService(riderLocationIndex)

        `when`("인근 라이더를 검색하면") {
            val result = riderSearchDomainService.searchNearby(storeCoordinate, VehicleType.BICYCLE, searchLimit)

            then("3km 결과를 그대로 쓰고 반경을 넓히지 않는다") {
                result.searchRadiusMeters shouldBe 3_000
                result.riders shouldHaveSize 5
                verify(exactly = 1) { riderLocationIndex.searchWithin(storeCoordinate, 3_000, searchLimit) }
                verify(exactly = 0) { riderLocationIndex.searchWithin(storeCoordinate, 5_000, searchLimit) }
            }

            then("거리 오름차순 순서를 유지한다") {
                result.riders.map { it.riderId } shouldContainExactly listOf(1L, 2L, 3L, 4L, 5L)
            }
        }
    }

    given("3km 에는 2명, 5km 에는 6명이 있는 자전거 배차") {
        val riderLocationIndex = mockk<RiderLocationIndex>()
        every { riderLocationIndex.searchWithin(storeCoordinate, 3_000, searchLimit) } returns
            (1L..2L).map { freshRider(it, it * 100.0) }
        every { riderLocationIndex.searchWithin(storeCoordinate, 5_000, searchLimit) } returns
            (1L..6L).map { freshRider(it, it * 100.0) }
        val riderSearchDomainService = RiderSearchDomainService(riderLocationIndex)

        `when`("인근 라이더를 검색하면") {
            val result = riderSearchDomainService.searchNearby(storeCoordinate, VehicleType.BICYCLE, searchLimit)

            then("최소 5명을 채울 때까지 5km 로 한 단계 확장한다") {
                result.searchRadiusMeters shouldBe 5_000
                result.riders shouldHaveSize 6
            }

            then("7km 까지는 확장하지 않는다") {
                verify(exactly = 0) { riderLocationIndex.searchWithin(storeCoordinate, 7_000, searchLimit) }
            }
        }
    }

    given("모든 반경 단계에서 라이더가 부족한 자전거 배차") {
        val riderLocationIndex = mockk<RiderLocationIndex>()
        every { riderLocationIndex.searchWithin(storeCoordinate, 3_000, searchLimit) } returns emptyList()
        every { riderLocationIndex.searchWithin(storeCoordinate, 5_000, searchLimit) } returns
            listOf(freshRider(1L, 4_000.0))
        every { riderLocationIndex.searchWithin(storeCoordinate, 7_000, searchLimit) } returns
            (1L..3L).map { freshRider(it, it * 1_000.0) }
        val riderSearchDomainService = RiderSearchDomainService(riderLocationIndex)

        `when`("인근 라이더를 검색하면") {
            val result = riderSearchDomainService.searchNearby(storeCoordinate, VehicleType.BICYCLE, searchLimit)

            then("마지막 단계인 7km 결과를 반환한다") {
                result.searchRadiusMeters shouldBe 7_000
                result.riders shouldHaveSize 3
            }

            then("반경 사다리 3단계를 모두 시도한다") {
                verify(exactly = 1) { riderLocationIndex.searchWithin(storeCoordinate, 3_000, searchLimit) }
                verify(exactly = 1) { riderLocationIndex.searchWithin(storeCoordinate, 5_000, searchLimit) }
                verify(exactly = 1) { riderLocationIndex.searchWithin(storeCoordinate, 7_000, searchLimit) }
            }
        }
    }

    given("3km 안에 신선한 라이더 2명과 30초 넘게 갱신되지 않은 라이더 4명") {
        val riderLocationIndex = mockk<RiderLocationIndex>()
        every { riderLocationIndex.searchWithin(storeCoordinate, 3_000, searchLimit) } returns
            listOf(
                freshRider(1L, 100.0),
                staleRider(2L, 200.0),
                staleRider(3L, 300.0),
                freshRider(4L, 400.0),
                staleRider(5L, 500.0),
                staleRider(6L, 600.0),
            )
        every { riderLocationIndex.searchWithin(storeCoordinate, 5_000, searchLimit) } returns
            (1L..5L).map { freshRider(it, it * 100.0) }
        val riderSearchDomainService = RiderSearchDomainService(riderLocationIndex)

        `when`("인근 라이더를 검색하면") {
            val result = riderSearchDomainService.searchNearby(storeCoordinate, VehicleType.BICYCLE, searchLimit)

            then("오래된 위치는 후보에서 빠진다") {
                result.searchRadiusMeters shouldBe 5_000
                result.riders.map { it.riderId } shouldContainExactly listOf(1L, 2L, 3L, 4L, 5L)
            }
        }
    }

    given("요청 수량이 최소 후보 수보다 작은 배차") {
        val riderLocationIndex = mockk<RiderLocationIndex>()
        every { riderLocationIndex.searchWithin(storeCoordinate, 3_000, 2) } returns
            (1L..2L).map { freshRider(it, it * 100.0) }
        val riderSearchDomainService = RiderSearchDomainService(riderLocationIndex)

        `when`("2명만 요청하면") {
            val result = riderSearchDomainService.searchNearby(storeCoordinate, VehicleType.BICYCLE, 2)

            then("요청 수량을 채웠으므로 반경을 넓히지 않는다") {
                result.searchRadiusMeters shouldBe 3_000
                result.riders shouldHaveSize 2
                verify(exactly = 0) { riderLocationIndex.searchWithin(storeCoordinate, 5_000, 2) }
            }
        }
    }

    given("오토바이 배차") {
        val riderLocationIndex = mockk<RiderLocationIndex>()
        every { riderLocationIndex.searchWithin(storeCoordinate, 5_000, searchLimit) } returns
            (1L..5L).map { freshRider(it, it * 100.0) }
        val riderSearchDomainService = RiderSearchDomainService(riderLocationIndex)

        `when`("인근 라이더를 검색하면") {
            val result = riderSearchDomainService.searchNearby(storeCoordinate, VehicleType.MOTORCYCLE, searchLimit)

            then("이동 수단 기본 반경인 5km 부터 탐색한다") {
                result.searchRadiusMeters shouldBe 5_000
            }
        }
    }
})
