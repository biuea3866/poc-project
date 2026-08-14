package com.biuea.delivery.infrastructure.geo

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.RiderLocationIndex
import com.biuea.delivery.support.RedisContainer
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * 네 가지 인덱스 구현을 같은 조건으로 세우는 테스트 픽스처.
 * 계약 테스트와 벤치마크가 같은 구성을 쓰도록 이 파일 하나가 소유한다.
 */
object RiderLocationIndexes {

    fun createStringRedisTemplate(): StringRedisTemplate {
        // Spring 컨텍스트 없이 Testcontainers Redis 에 직접 붙는다 — 인덱스 구현은 템플릿만 있으면 동작한다.
        val connectionFactory = LettuceConnectionFactory(RedisContainer.host, RedisContainer.port)
        connectionFactory.afterPropertiesSet()
        return StringRedisTemplate(connectionFactory).also { it.afterPropertiesSet() }
    }

    fun createAll(stringRedisTemplate: StringRedisTemplate): List<Pair<String, RiderLocationIndex>> =
        listOf(
            "FullScan" to FullScanRiderLocationIndex(),
            "Geohash" to GeohashRiderLocationIndex(stringRedisTemplate),
            "H3" to H3RiderLocationIndex(stringRedisTemplate),
            "RedisGeo" to RedisGeoRiderLocationIndex(stringRedisTemplate),
        )

    /** 중심에서 정북 방향으로 지정한 거리만큼 떨어진 좌표. */
    fun northOf(center: Coordinate, meters: Double): Coordinate =
        Coordinate(center.latitude + meters / METERS_PER_LATITUDE_DEGREE, center.longitude)

    private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
}
