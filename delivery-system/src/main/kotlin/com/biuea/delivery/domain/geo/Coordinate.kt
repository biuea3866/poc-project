package com.biuea.delivery.domain.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 위경도 좌표. 가게·라이더 위치를 표현하는 공용 값 객체다.
 *
 * geo 패키지는 특정 도메인(rider·order)에 속하지 않는 공간 계산 공용 커널이다.
 * 그래서 rider 도메인이 geo 를 참조해도 컨텍스트 간 결합이 생기지 않는다 (geo 는 아무 도메인도 참조하지 않는다).
 */
data class Coordinate(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in MIN_LATITUDE..MAX_LATITUDE) {
            "위도는 $MIN_LATITUDE ~ $MAX_LATITUDE 범위여야 합니다: $latitude"
        }
        require(longitude in MIN_LONGITUDE..MAX_LONGITUDE) {
            "경도는 $MIN_LONGITUDE ~ $MAX_LONGITUDE 범위여야 합니다: $longitude"
        }
    }

    /**
     * 하버사인(haversine) 공식으로 두 좌표의 대권 거리를 구한다.
     *
     * 배달 반경(수 km)에서는 지구를 구로 근사해도 타원체 기준 대비 오차가 0.5% 미만이라,
     * 비용이 큰 측지선(Vincenty) 대신 하버사인을 쓴다.
     */
    fun distanceMetersTo(other: Coordinate): Double {
        val latitudeRadians = Math.toRadians(latitude)
        val otherLatitudeRadians = Math.toRadians(other.latitude)
        val latitudeDeltaRadians = Math.toRadians(other.latitude - latitude)
        val longitudeDeltaRadians = Math.toRadians(other.longitude - longitude)
        val haversine = sin(latitudeDeltaRadians / 2).pow(2) +
            cos(latitudeRadians) * cos(otherLatitudeRadians) * sin(longitudeDeltaRadians / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine))
    }

    companion object {
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0

        /** IUGG 평균 지구 반지름. Redis GEO 가 쓰는 6,372,797m 과 0.03% 차이라 결과 비교에 영향이 없다. */
        private const val EARTH_RADIUS_METERS = 6_371_008.8
    }
}
