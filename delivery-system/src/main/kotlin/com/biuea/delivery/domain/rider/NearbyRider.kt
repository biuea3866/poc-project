package com.biuea.delivery.domain.rider

import com.biuea.delivery.domain.geo.Coordinate
import java.time.ZonedDateTime

/**
 * 반경 검색으로 찾은 라이더와 가게로부터의 실제 거리.
 *
 * updatedAt 을 함께 싣는 이유: 신선도 판정은 도메인 정책인데, 인덱스 구현마다 이 판정을
 * 따로 구현하면 4개 구현이 서로 다른 배차 정책을 갖게 된다. 인덱스는 시각만 실어 나르고
 * 판정은 도메인이 한다.
 */
data class NearbyRider(
    val riderId: Long,
    val coordinate: Coordinate,
    val distanceMeters: Double,
    val updatedAt: ZonedDateTime,
) {
    fun isFresh(): Boolean = RiderLocationFreshness.isFresh(updatedAt)
}
