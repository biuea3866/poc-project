package com.biuea.delivery.domain.rider

import com.biuea.delivery.domain.geo.Coordinate
import java.time.ZonedDateTime

/**
 * 인덱스에 적재하는 라이더의 현재 위치.
 */
data class RiderLocation(
    val riderId: Long,
    val coordinate: Coordinate,
    val updatedAt: ZonedDateTime,
) {
    init {
        require(riderId > 0) { "라이더 식별자는 양수여야 합니다: $riderId" }
    }

    fun isFresh(): Boolean = RiderLocationFreshness.isFresh(updatedAt)
}
