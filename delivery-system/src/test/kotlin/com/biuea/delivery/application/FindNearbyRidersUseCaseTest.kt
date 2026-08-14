package com.biuea.delivery.application

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.NearbyRiderSearchResult
import com.biuea.delivery.domain.rider.RiderSearchDomainService
import com.biuea.delivery.domain.rider.VehicleType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class FindNearbyRidersUseCaseTest : BehaviorSpec({

    val storeCoordinate = Coordinate(37.4979, 127.0276)
    val riderCoordinate = Coordinate(37.4985, 127.0280)

    given("3km 안에서 라이더 2명을 찾은 배차 요청") {
        val riderSearchDomainService = mockk<RiderSearchDomainService>()
        every { riderSearchDomainService.searchNearby(storeCoordinate, VehicleType.BICYCLE, 10) } returns
            NearbyRiderSearchResult(
                3_000,
                listOf(
                    NearbyRider(11L, riderCoordinate, 82.5, ZonedDateTime.now()),
                    NearbyRider(12L, riderCoordinate, 340.0, ZonedDateTime.now()),
                ),
            )
        val findNearbyRidersUseCase = FindNearbyRidersUseCase(riderSearchDomainService)

        `when`("유스케이스를 실행하면") {
            val response = findNearbyRidersUseCase.execute(
                FindNearbyRidersCommand(storeCoordinate, VehicleType.BICYCLE, 10),
            )

            then("검색에 쓰인 반경과 라이더 목록을 응답으로 돌려준다") {
                response.searchRadiusMeters shouldBe 3_000
                response.riders.map { it.riderId } shouldContainExactly listOf(11L, 12L)
                response.riders.first().distanceMeters shouldBe 82.5
                response.riders.first().latitude shouldBe riderCoordinate.latitude
                response.riders.first().longitude shouldBe riderCoordinate.longitude
            }

            then("커맨드 값을 그대로 도메인 서비스에 전달한다") {
                verify(exactly = 1) {
                    riderSearchDomainService.searchNearby(storeCoordinate, VehicleType.BICYCLE, 10)
                }
            }
        }
    }

    given("모든 반경 단계에서 라이더를 찾지 못한 배차 요청") {
        val riderSearchDomainService = mockk<RiderSearchDomainService>()
        every { riderSearchDomainService.searchNearby(storeCoordinate, VehicleType.WALK, 10) } returns
            NearbyRiderSearchResult(5_500, emptyList())
        val findNearbyRidersUseCase = FindNearbyRidersUseCase(riderSearchDomainService)

        `when`("유스케이스를 실행하면") {
            val response = findNearbyRidersUseCase.execute(
                FindNearbyRidersCommand(storeCoordinate, VehicleType.WALK, 10),
            )

            then("마지막으로 시도한 반경과 빈 목록을 돌려준다") {
                response.searchRadiusMeters shouldBe 5_500
                response.riders.shouldBeEmpty()
            }
        }
    }
})
