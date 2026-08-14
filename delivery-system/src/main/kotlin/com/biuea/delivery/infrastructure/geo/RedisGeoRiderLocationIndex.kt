package com.biuea.delivery.infrastructure.geo

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.RiderLocation
import com.biuea.delivery.domain.rider.RiderLocationIndex
import org.springframework.data.geo.Distance
import org.springframework.data.geo.GeoResult
import org.springframework.data.geo.GeoResults
import org.springframework.data.geo.Point
import org.springframework.data.redis.connection.RedisGeoCommands.DistanceUnit
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.domain.geo.GeoReference
import org.springframework.stereotype.Component

/**
 * Redis 내장 GEO 명령(GEOADD / GEOSEARCH BYRADIUS)을 쓰는 인덱스.
 *
 * 후보 수집과 정확 거리 필터를 서버가 처리하므로 네트워크로는 결과만 넘어온다.
 * 셀 기반 구현이 후보 수천 건을 클라이언트로 끌어오는 것과 대비되는 지점이다.
 *
 * 운영 기본 구현이라 이 구현만 빈으로 등록한다. 나머지 세 구현은 비교 실험용이라
 * 빈 등록 없이 벤치마크·테스트에서 직접 생성한다.
 */
@Component
class RedisGeoRiderLocationIndex(
    private val stringRedisTemplate: StringRedisTemplate,
) : RiderLocationIndex {

    override fun update(location: RiderLocation) {
        val riderIdText = location.riderId.toString()
        // GEOADD 는 같은 member 를 덮어쓰므로 이전 위치 삭제가 따로 필요 없다 — 셀 기반 구현과 갈리는 지점이다.
        stringRedisTemplate.opsForGeo().add(GEO_KEY, toPoint(location.coordinate), riderIdText)
        stringRedisTemplate.opsForHash<String, String>()
            .put(UPDATED_AT_KEY, riderIdText, RiderLocationTimestamp.format(location.updatedAt))
    }

    override fun searchWithin(center: Coordinate, radiusMeters: Int, limit: Int): List<NearbyRider> {
        val searchResults = search(center, radiusMeters, limit)?.content.orEmpty()
        if (searchResults.isEmpty()) return emptyList()
        val riderIdTexts = searchResults.map { it.content.name }
        val updatedAtTexts = stringRedisTemplate.opsForHash<String, String>()
            .multiGet(UPDATED_AT_KEY, riderIdTexts)
        return searchResults.zip(updatedAtTexts).mapNotNull { (searchResult, updatedAtText) ->
            updatedAtText?.let { toNearbyRider(searchResult, it) }
        }
    }

    /**
     * Redis 가 서버에서 후보를 걸러 결과만 돌려주므로, 클라이언트가 볼 수 있는 후보 수는 곧 반경 안 라이더 수다.
     * 서버 내부에서 훑은 셀 개수는 관측할 수 없어 셀 기반 구현과 직접 비교할 때 이 한계를 감안해야 한다.
     */
    override fun candidateCount(center: Coordinate, radiusMeters: Int): Int =
        search(center, radiusMeters, limit = NO_LIMIT)?.content?.size ?: 0

    override fun clear() {
        stringRedisTemplate.delete(listOf(GEO_KEY, UPDATED_AT_KEY))
    }

    private fun search(center: Coordinate, radiusMeters: Int, limit: Int): GeoResults<GeoLocation<String>>? {
        val searchArguments = GeoSearchCommandArgs.newGeoSearchArgs()
            .includeDistance()
            .includeCoordinates()
            .sortAscending()
        if (limit != NO_LIMIT) {
            searchArguments.limit(limit.toLong())
        }
        return stringRedisTemplate.opsForGeo().search(
            GEO_KEY,
            GeoReference.fromCoordinate(toPoint(center)),
            Distance(radiusMeters.toDouble(), DistanceUnit.METERS),
            searchArguments,
        )
    }

    private fun toNearbyRider(searchResult: GeoResult<GeoLocation<String>>, updatedAtText: String): NearbyRider {
        val point = searchResult.content.point
        return NearbyRider(
            riderId = searchResult.content.name.toLong(),
            coordinate = Coordinate(point.y, point.x),
            distanceMeters = searchResult.distance.value,
            updatedAt = RiderLocationTimestamp.parse(updatedAtText),
        )
    }

    /** Redis GEO 의 Point 는 (x, y) = (경도, 위도) 순서다. */
    private fun toPoint(coordinate: Coordinate): Point = Point(coordinate.longitude, coordinate.latitude)

    companion object {
        private const val GEO_KEY = "delivery:rider:redis-geo"
        private const val UPDATED_AT_KEY = "delivery:rider:redis-geo-updated-at"
        private const val NO_LIMIT = -1
    }
}
