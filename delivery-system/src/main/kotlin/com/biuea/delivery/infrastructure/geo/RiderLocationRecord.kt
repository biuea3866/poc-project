package com.biuea.delivery.infrastructure.geo

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.RiderLocation

/**
 * 셀 기반 인덱스(Geohash·H3)가 Redis 해시에 담는 라이더 위치 레코드.
 *
 * 갱신 시각은 문자열 그대로 들고 다니다가 반경 필터를 통과한 라이더에게만 파싱한다.
 * 한 번 조회에 후보가 수천 건이라, 버려질 후보까지 시각을 파싱하면 그 비용이 측정값을 오염시킨다.
 */
data class RiderLocationRecord(
    val coordinate: Coordinate,
    val updatedAtText: String,
) {
    fun serialize(): String =
        "${coordinate.latitude}$FIELD_SEPARATOR${coordinate.longitude}$FIELD_SEPARATOR$updatedAtText"

    fun toNearbyRider(riderId: Long, distanceMeters: Double): NearbyRider =
        NearbyRider(
            riderId = riderId,
            coordinate = coordinate,
            distanceMeters = distanceMeters,
            updatedAt = RiderLocationTimestamp.parse(updatedAtText),
        )

    companion object {
        private const val FIELD_SEPARATOR = '|'
        private const val FIELD_COUNT = 3

        fun from(location: RiderLocation): RiderLocationRecord =
            RiderLocationRecord(location.coordinate, RiderLocationTimestamp.format(location.updatedAt))

        fun parse(text: String): RiderLocationRecord {
            val fields = text.split(FIELD_SEPARATOR)
            require(fields.size == FIELD_COUNT) { "라이더 위치 레코드 형식이 아닙니다: $text" }
            return RiderLocationRecord(Coordinate(fields[0].toDouble(), fields[1].toDouble()), fields[2])
        }
    }
}
