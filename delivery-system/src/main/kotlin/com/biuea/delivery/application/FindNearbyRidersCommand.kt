package com.biuea.delivery.application

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.VehicleType

/**
 * 가게 좌표 기준 배차 후보 라이더 조회 파라미터.
 */
data class FindNearbyRidersCommand(
    val storeCoordinate: Coordinate,
    val vehicleType: VehicleType,
    val limit: Int,
)
