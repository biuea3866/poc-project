package com.biuea.delivery.infrastructure.geo

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.RiderLocation
import com.biuea.delivery.domain.rider.RiderLocationIndex
import java.util.concurrent.ConcurrentHashMap

/**
 * 전체 라이더를 순회하며 하버사인 거리를 계산하는 베이스라인 구현.
 *
 * 공간 인덱스가 없으므로 후보 수 = 전체 라이더 수다. 다른 세 구현의 후보 축소 효과를
 * 이 값과의 비율로 재기 위해 존재한다.
 */
class FullScanRiderLocationIndex : RiderLocationIndex {

    private val locationsByRiderId = ConcurrentHashMap<Long, RiderLocation>()

    override fun update(location: RiderLocation) {
        locationsByRiderId[location.riderId] = location
    }

    override fun searchWithin(center: Coordinate, radiusMeters: Int, limit: Int): List<NearbyRider> =
        locationsByRiderId.values.asSequence()
            .map { location ->
                NearbyRider(
                    riderId = location.riderId,
                    coordinate = location.coordinate,
                    distanceMeters = center.distanceMetersTo(location.coordinate),
                    updatedAt = location.updatedAt,
                )
            }
            .filter { it.distanceMeters <= radiusMeters }
            .sortedBy { it.distanceMeters }
            .take(limit)
            .toList()

    override fun candidateCount(center: Coordinate, radiusMeters: Int): Int = locationsByRiderId.size

    override fun clear() {
        locationsByRiderId.clear()
    }
}
