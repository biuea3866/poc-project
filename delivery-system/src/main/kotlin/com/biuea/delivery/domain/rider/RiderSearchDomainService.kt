package com.biuea.delivery.domain.rider

import com.biuea.delivery.domain.geo.Coordinate
import org.springframework.stereotype.Service

/**
 * 가게 좌표 기준 배차 후보 라이더를 찾는다.
 *
 * 인덱스 조회 결과에 신선도 필터를 적용하고, 후보가 최소 인원에 못 미치면 반경을 단계적으로 넓힌다.
 */
@Service
class RiderSearchDomainService(
    private val riderLocationIndex: RiderLocationIndex,
) {

    fun searchNearby(center: Coordinate, vehicleType: VehicleType, limit: Int): NearbyRiderSearchResult {
        require(limit > 0) { "조회 인원은 양수여야 합니다: $limit" }
        // limit 이 최소 인원보다 작으면 요청 수량을 채운 시점이 곧 충분한 상태다.
        val requiredRiderCount = minOf(MINIMUM_RIDER_COUNT, limit)
        var lastResult = NearbyRiderSearchResult(vehicleType.defaultSearchRadiusMeters, emptyList())
        for (radiusMeters in vehicleType.searchRadiusLadderMeters()) {
            lastResult = NearbyRiderSearchResult(radiusMeters, freshRidersWithin(center, radiusMeters, limit))
            if (lastResult.hasAtLeast(requiredRiderCount)) break
        }
        return lastResult
    }

    private fun freshRidersWithin(center: Coordinate, radiusMeters: Int, limit: Int): List<NearbyRider> =
        riderLocationIndex.searchWithin(center, radiusMeters, limit).filter { it.isFresh() }

    companion object {
        /** 이 인원을 못 채우면 반경을 넓힌다. 한 명뿐인 후보가 거절하면 배차가 처음부터 다시 돌기 때문이다. */
        private const val MINIMUM_RIDER_COUNT = 5
    }
}
