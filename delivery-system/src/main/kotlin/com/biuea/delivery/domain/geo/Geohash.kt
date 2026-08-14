package com.biuea.delivery.domain.geo

import kotlin.math.cos
import kotlin.math.pow

/**
 * Geohash 직접 구현. 위경도를 비트로 번갈아 이등분해 base32 문자열로 만든다.
 *
 * 같은 접두사 = 같은 사각 셀이라는 성질을 이용해 "셀 키 하나로 후보를 모으는" 인덱스를 만들 수 있다.
 * 다만 접두사가 같으면 가깝다는 명제의 역은 성립하지 않는다 — 셀 경계를 사이에 둔 두 점은
 * 몇십 미터 거리에도 접두사가 갈린다. 그래서 조회 시 이웃 8셀을 함께 훑어야 한다.
 */
object Geohash {

    private const val BASE32_DIGITS = "0123456789bcdefghjkmnpqrstuvwxyz"
    private val BIT_MASKS = intArrayOf(16, 8, 4, 2, 1)
    private const val BITS_PER_CHARACTER = 5
    private const val MIN_PRECISION = 1
    private const val MAX_PRECISION = 12

    /** 위도 1도의 거리. 경도 1도는 여기에 cos(위도)를 곱한다. */
    private const val METERS_PER_LATITUDE_DEGREE = 111_320.0

    fun encode(coordinate: Coordinate, precision: Int): String {
        require(precision in MIN_PRECISION..MAX_PRECISION) {
            "정밀도는 $MIN_PRECISION ~ $MAX_PRECISION 범위여야 합니다: $precision"
        }
        var minLatitude = Coordinate.MIN_LATITUDE
        var maxLatitude = Coordinate.MAX_LATITUDE
        var minLongitude = Coordinate.MIN_LONGITUDE
        var maxLongitude = Coordinate.MAX_LONGITUDE
        val encoded = StringBuilder(precision)
        var isLongitudeTurn = true
        var bitIndex = 0
        var characterBits = 0
        while (encoded.length < precision) {
            if (isLongitudeTurn) {
                val middle = (minLongitude + maxLongitude) / 2
                if (coordinate.longitude > middle) {
                    characterBits = characterBits or BIT_MASKS[bitIndex]
                    minLongitude = middle
                } else {
                    maxLongitude = middle
                }
            } else {
                val middle = (minLatitude + maxLatitude) / 2
                if (coordinate.latitude > middle) {
                    characterBits = characterBits or BIT_MASKS[bitIndex]
                    minLatitude = middle
                } else {
                    maxLatitude = middle
                }
            }
            isLongitudeTurn = !isLongitudeTurn
            if (bitIndex < BIT_MASKS.lastIndex) {
                bitIndex++
            } else {
                encoded.append(BASE32_DIGITS[characterBits])
                bitIndex = 0
                characterBits = 0
            }
        }
        return encoded.toString()
    }

    fun decode(geohash: String): GeohashCell {
        require(geohash.isNotEmpty()) { "geohash 는 비어 있을 수 없습니다" }
        var minLatitude = Coordinate.MIN_LATITUDE
        var maxLatitude = Coordinate.MAX_LATITUDE
        var minLongitude = Coordinate.MIN_LONGITUDE
        var maxLongitude = Coordinate.MAX_LONGITUDE
        var isLongitudeTurn = true
        geohash.forEach { character ->
            val characterBits = BASE32_DIGITS.indexOf(character)
            require(characterBits >= 0) { "geohash 에 쓸 수 없는 문자입니다: $character" }
            BIT_MASKS.forEach { mask ->
                if (isLongitudeTurn) {
                    val middle = (minLongitude + maxLongitude) / 2
                    if (characterBits and mask != 0) minLongitude = middle else maxLongitude = middle
                } else {
                    val middle = (minLatitude + maxLatitude) / 2
                    if (characterBits and mask != 0) minLatitude = middle else maxLatitude = middle
                }
                isLongitudeTurn = !isLongitudeTurn
            }
        }
        return GeohashCell(minLatitude, maxLatitude, minLongitude, maxLongitude)
    }

    /**
     * 인접 8셀. 셀 크기만큼 상하좌우로 이동한 좌표를 같은 정밀도로 다시 인코딩해 구한다.
     * 셀 경계에 걸친 후보를 놓치지 않으려면 조회 시 중심 셀과 함께 반드시 훑어야 한다.
     */
    fun neighbors(geohash: String): List<String> {
        val cell = decode(geohash)
        return NEIGHBOR_STEPS
            .map { (latitudeSteps, longitudeSteps) ->
                encode(cell.neighborCenterAt(latitudeSteps, longitudeSteps), geohash.length)
            }
            .filter { it != geohash }
            .distinct()
    }

    /**
     * 반경을 중심 셀 + 이웃 8셀로 덮을 수 있는 가장 촘촘한 정밀도.
     * 셀 한 변이 반경보다 크면 3x3 블록이 반경을 항상 포함한다.
     */
    fun precisionFor(radiusMeters: Int, latitude: Double): Int {
        require(radiusMeters > 0) { "반경은 양수여야 합니다: $radiusMeters" }
        return (MAX_PRECISION downTo MIN_PRECISION).firstOrNull { precision ->
            minOf(cellHeightMeters(precision), cellWidthMeters(precision, latitude)) >= radiusMeters
        } ?: MIN_PRECISION
    }

    fun cellHeightMeters(precision: Int): Double =
        latitudeSpanDegrees(precision) * METERS_PER_LATITUDE_DEGREE

    fun cellWidthMeters(precision: Int, latitude: Double): Double =
        longitudeSpanDegrees(precision) * METERS_PER_LATITUDE_DEGREE * cos(Math.toRadians(latitude))

    private fun latitudeSpanDegrees(precision: Int): Double {
        val latitudeBits = precision * BITS_PER_CHARACTER / 2
        return (Coordinate.MAX_LATITUDE - Coordinate.MIN_LATITUDE) / 2.0.pow(latitudeBits)
    }

    private fun longitudeSpanDegrees(precision: Int): Double {
        val longitudeBits = (precision * BITS_PER_CHARACTER + 1) / 2
        return (Coordinate.MAX_LONGITUDE - Coordinate.MIN_LONGITUDE) / 2.0.pow(longitudeBits)
    }

    private val NEIGHBOR_STEPS: List<Pair<Int, Int>> =
        listOf(-1, 0, 1).flatMap { latitudeSteps ->
            listOf(-1, 0, 1).map { longitudeSteps -> latitudeSteps to longitudeSteps }
        }.filterNot { (latitudeSteps, longitudeSteps) -> latitudeSteps == 0 && longitudeSteps == 0 }
}
