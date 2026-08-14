package com.biuea.delivery.application

import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.NearbyRiderSearchResult

/**
 * 배차 후보 응답. 어느 반경까지 넓혀서 찾았는지를 함께 내려 호출자가 배차 난이도를 알 수 있게 한다.
 */
data class FindNearbyRidersResponse(
    val searchRadiusMeters: Int,
    val riders: List<NearbyRiderResponse>,
) {
    companion object {
        fun of(result: NearbyRiderSearchResult): FindNearbyRidersResponse =
            FindNearbyRidersResponse(
                searchRadiusMeters = result.searchRadiusMeters,
                riders = result.riders.map { NearbyRiderResponse.from(it) },
            )
    }
}

data class NearbyRiderResponse(
    val riderId: Long,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
) {
    companion object {
        fun from(nearbyRider: NearbyRider): NearbyRiderResponse =
            NearbyRiderResponse(
                riderId = nearbyRider.riderId,
                latitude = nearbyRider.coordinate.latitude,
                longitude = nearbyRider.coordinate.longitude,
                distanceMeters = nearbyRider.distanceMeters,
            )
    }
}
