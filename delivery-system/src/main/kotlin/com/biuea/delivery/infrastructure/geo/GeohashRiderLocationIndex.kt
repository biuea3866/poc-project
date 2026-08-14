package com.biuea.delivery.infrastructure.geo

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.geo.Geohash
import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.RiderLocation
import com.biuea.delivery.domain.rider.RiderLocationIndex
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * 직접 구현한 Geohash 로 셀 키를 만드는 인덱스.
 *
 * 조회는 중심 셀 + 이웃 8셀을 SUNION 으로 모아 후보를 만들고, 정확 거리로 다시 거른다.
 * 셀 경계에 걸친 라이더를 놓치지 않으려면 이웃 8셀 조회가 필수다 — 중심 셀만 보면
 * 79m 떨어진 라이더도 접두사가 갈려 사라진다.
 *
 * 정밀도를 3개(4~6)로 중복 색인하는 이유: geohash 셀 크기는 정밀도 단위로 4배씩 뛴다.
 * 반경 3km 는 정밀도 5, 반경 7km 는 정밀도 4가 필요한데 update 시점에는 어떤 반경으로
 * 조회할지 알 수 없다. 이 쓰기 증폭이 geohash 방식의 실제 비용이다.
 */
class GeohashRiderLocationIndex(
    stringRedisTemplate: StringRedisTemplate,
) : RiderLocationIndex {

    private val cellRiderLocationStore =
        CellRiderLocationStore(stringRedisTemplate, CELL_KEY_PREFIX, LOCATION_KEY)

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

    /** 반경을 덮는 정밀도의 중심 셀 + 이웃 8셀. */
    private fun searchCells(center: Coordinate, radiusMeters: Int): List<String> {
        val centerCell = Geohash.encode(center, searchPrecisionFor(radiusMeters, center.latitude))
        return listOf(centerCell) + Geohash.neighbors(centerCell)
    }

    private fun searchPrecisionFor(radiusMeters: Int, latitude: Double): Int {
        val precision = Geohash.precisionFor(radiusMeters, latitude)
        require(precision >= MIN_INDEXED_PRECISION) {
            "색인 정밀도가 감당하지 못하는 반경입니다: ${radiusMeters}m"
        }
        // 색인보다 촘촘한 정밀도가 나오면 색인된 가장 촘촘한 정밀도로 낮춘다 — 셀이 커질 뿐 누락은 없다.
        return precision.coerceAtMost(MAX_INDEXED_PRECISION)
    }

    private fun cellsOf(coordinate: Coordinate): List<String> =
        INDEXED_PRECISIONS.map { precision -> Geohash.encode(coordinate, precision) }

    companion object {
        /** 정밀도 4~6 은 위도 37.5 기준 반경 611m ~ 19.5km 조회를 덮는다. */
        private const val MIN_INDEXED_PRECISION = 4
        private const val MAX_INDEXED_PRECISION = 6
        private val INDEXED_PRECISIONS = (MIN_INDEXED_PRECISION..MAX_INDEXED_PRECISION).toList()

        private const val CELL_KEY_PREFIX = "delivery:rider:geohash-cell:"
        private const val LOCATION_KEY = "delivery:rider:geohash-location"
    }
}
