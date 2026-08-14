package com.biuea.delivery.infrastructure.geo

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.RiderLocation
import com.biuea.delivery.domain.rider.RiderLocationIndex
import com.uber.h3core.H3Core
import com.uber.h3core.LengthUnit
import org.springframework.data.redis.core.StringRedisTemplate
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Uber H3 육각 격자로 셀 키를 만드는 인덱스.
 *
 * 육각 셀은 이웃 6방향의 중심 간 거리가 모두 같아, geohash 처럼 위도에 따라 셀이 찌그러지지 않는다.
 * 덕분에 "반경을 덮으려면 몇 링이 필요한가"를 변 길이 하나로 계산할 수 있다.
 *
 * geohash 와 같은 이유로 해상도 3개(6~8)에 중복 색인한다 — 조회 반경에 맞는 해상도를 update 시점에 모르기 때문이다.
 */
class H3RiderLocationIndex(
    stringRedisTemplate: StringRedisTemplate,
) : RiderLocationIndex {

    private val h3Core: H3Core = H3Core.newInstance()
    private val cellRiderLocationStore =
        CellRiderLocationStore(stringRedisTemplate, CELL_KEY_PREFIX, LOCATION_KEY)

    /** JNI 호출 비용을 조회 경로에서 빼기 위해 해상도별 평균 변 길이를 미리 읽어둔다. */
    private val edgeLengthMetersByResolution: Map<Int, Double> =
        INDEXED_RESOLUTIONS.associateWith { resolution ->
            h3Core.getHexagonEdgeLengthAvg(resolution, LengthUnit.m)
        }

    override fun update(location: RiderLocation) {
        val previousCells = cellRiderLocationStore.findRecordBy(location.riderId)
            ?.let { cellsOf(it.coordinate) }
            ?: emptyList()
        cellRiderLocationStore.save(location, previousCells, cellsOf(location.coordinate))
    }

    override fun searchWithin(center: Coordinate, radiusMeters: Int, limit: Int): List<NearbyRider> =
        cellRiderLocationStore.nearestRiders(
            candidateRiderIds(center, radiusMeters),
            center,
            radiusMeters,
            limit,
        )

    override fun candidateCount(center: Coordinate, radiusMeters: Int): Int =
        candidateRiderIds(center, radiusMeters).size

    override fun clear() = cellRiderLocationStore.clear()

    private fun candidateRiderIds(center: Coordinate, radiusMeters: Int): Set<String> =
        cellRiderLocationStore.candidateRiderIds(searchCells(center, radiusMeters))

    /** 중심 셀에서 k-ring(gridDisk)을 펼쳐 반경을 덮는 셀 목록. */
    private fun searchCells(center: Coordinate, radiusMeters: Int): List<String> {
        val resolution = resolutionFor(radiusMeters)
        val centerCell = h3Core.latLngToCellAddress(center.latitude, center.longitude, resolution)
        return h3Core.gridDisk(centerCell, ringCountFor(radiusMeters, resolution))
    }

    /** 평균 변 길이가 반경의 절반에 가장 가까운 해상도. 링 수가 2~3에 머물러 조회 키 수가 적게 유지된다. */
    private fun resolutionFor(radiusMeters: Int): Int =
        INDEXED_RESOLUTIONS.minBy { resolution ->
            abs(edgeLengthMetersOf(resolution) - radiusMeters / 2.0)
        }

    /**
     * 필요한 링 수.
     * 질의 지점은 중심 셀 안 어디에나 있을 수 있어 외접 반경(변 길이 E)만큼 여유가 필요하고,
     * 육각 격자에서 링 하나는 최소 1.5E 를 덮는다. 그래서 k = ceil((R + E) / 1.5E) 다.
     */
    private fun ringCountFor(radiusMeters: Int, resolution: Int): Int {
        val edgeLengthMeters = edgeLengthMetersOf(resolution)
        return ceil((radiusMeters + edgeLengthMeters) / (RING_COVERAGE_FACTOR * edgeLengthMeters)).toInt()
    }

    private fun edgeLengthMetersOf(resolution: Int): Double =
        requireNotNull(edgeLengthMetersByResolution[resolution]) { "색인되지 않은 해상도입니다: $resolution" }

    private fun cellsOf(coordinate: Coordinate): List<String> =
        INDEXED_RESOLUTIONS.map { resolution ->
            h3Core.latLngToCellAddress(coordinate.latitude, coordinate.longitude, resolution)
        }

    companion object {
        /** 해상도 6~8 은 평균 변 길이 3.7km ~ 0.5km 로 반경 1km ~ 9km 조회를 덮는다. */
        private val INDEXED_RESOLUTIONS = listOf(6, 7, 8)
        private const val RING_COVERAGE_FACTOR = 1.5

        private const val CELL_KEY_PREFIX = "delivery:rider:h3-cell:"
        private const val LOCATION_KEY = "delivery:rider:h3-location"
    }
}
