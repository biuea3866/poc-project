package com.biuea.delivery.domain.geo

/**
 * geohash 문자열 하나가 가리키는 사각 셀. 디코딩 결과이자 이웃 셀 계산의 기준이다.
 */
data class GeohashCell(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
) {
    val center: Coordinate
        get() = Coordinate((minLatitude + maxLatitude) / 2, (minLongitude + maxLongitude) / 2)

    val latitudeSpan: Double get() = maxLatitude - minLatitude

    val longitudeSpan: Double get() = maxLongitude - minLongitude

    fun contains(coordinate: Coordinate): Boolean =
        coordinate.latitude in minLatitude..maxLatitude && coordinate.longitude in minLongitude..maxLongitude

    /**
     * 셀 크기만큼 이동한 이웃 셀의 중심 좌표. (1, -1) 이면 북쪽 한 칸, 서쪽 한 칸이다.
     * 극지방은 위도를 클램프하므로 이웃이 자기 자신과 겹칠 수 있고, 날짜변경선은 경도를 감아 넘긴다.
     */
    fun neighborCenterAt(latitudeSteps: Int, longitudeSteps: Int): Coordinate {
        val shiftedLatitude = (center.latitude + latitudeSteps * latitudeSpan)
            .coerceIn(Coordinate.MIN_LATITUDE, Coordinate.MAX_LATITUDE)
        val shiftedLongitude = wrapLongitude(center.longitude + longitudeSteps * longitudeSpan)
        return Coordinate(shiftedLatitude, shiftedLongitude)
    }

    private fun wrapLongitude(longitude: Double): Double {
        val fullTurn = Coordinate.MAX_LONGITUDE - Coordinate.MIN_LONGITUDE
        val normalized = (longitude - Coordinate.MIN_LONGITUDE) % fullTurn
        val positive = if (normalized < 0) normalized + fullTurn else normalized
        return positive + Coordinate.MIN_LONGITUDE
    }
}
