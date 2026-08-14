package com.biuea.delivery.infrastructure.geo

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.RiderLocation
import com.biuea.delivery.domain.rider.RiderLocationIndex
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * 공간 인덱스를 두지 않고, 매 질의마다 Redis 의 위치 HASH 전체를 끌어와 애플리케이션에서 하버사인 거리를 계산하는 베이스라인.
 *
 * 존재 이유는 비교의 공정성이다. FullScanRiderLocationIndex 는 JVM 힙의 ConcurrentHashMap 을 순회해
 * 네트워크 왕복이 0회인데, Geohash·H3 는 Redis 에서 후보 수천~수만 건을 끌어온다. 이 둘을 그대로 견주면
 * "셀 인덱스가 전수 스캔보다 느리다"는 결론이 나오지만, 그 차이에는 인덱스 방식이 아닌
 * 원격 저장소 왕복·직렬화·전송 비용이 섞여 있다. 이 구현이 그 빠진 비교군 —
 * "원격 저장소 + 공간 인덱스 없음" — 을 채워, 지연 차이를 인덱스 효과와 원격 비용으로 분리한다.
 *
 * 운영에 쓸 구현이 아니다. 질의 1회가 전체 라이더를 전송하므로 전송량이 라이더 수에 정비례한다.
 *
 * 셀 기반 구현과 같은 위치 HASH 구조(라이더 ID → "위도|경도|갱신시각")를 쓰되 셀 SET 을 만들지 않는다.
 * 덕분에 갱신은 HSET 한 번(왕복 1회)으로 끝나 셀 기반의 쓰기 증폭이 사라지고, 대신 조회가 전 건 전송을 낸다.
 */
class RemoteFullScanRiderLocationIndex(
    private val stringRedisTemplate: StringRedisTemplate,
) : RiderLocationIndex {

    /** 셀 색인이 없으므로 이전 위치를 조회할 필요도, SREM/SADD 로 셀을 옮길 필요도 없다. */
    override fun update(location: RiderLocation) {
        stringRedisTemplate.opsForHash<String, String>().put(
            LOCATION_KEY,
            location.riderId.toString(),
            RiderLocationRecord.from(location).serialize(),
        )
    }

    /**
     * HGETALL 한 번으로 전 건을 받아 애플리케이션에서 거리를 계산한다.
     *
     * 왕복은 1회뿐이고 전송량만 전체 라이더 수에 비례하므로, 이 구현의 지연은
     * "왕복 수"가 아니라 "전송량 + 클라이언트 계산량"이 지배한다 — 벤치마크가 재려는 값이 그것이다.
     * 갱신 시각 파싱을 반경 통과 라이더에게만 미루는 것은 셀 기반 구현과 동일한 이유다
     * (버려질 후보의 파싱 비용이 측정을 오염시킨다).
     */
    override fun searchWithin(center: Coordinate, radiusMeters: Int, limit: Int): List<NearbyRider> =
        stringRedisTemplate.opsForHash<String, String>().entries(LOCATION_KEY).asSequence()
            .map { (riderIdText, recordText) -> measure(riderIdText, recordText, center) }
            .filter { it.distanceMeters <= radiusMeters }
            .sortedBy { it.distanceMeters }
            .take(limit)
            .map { it.record.toNearbyRider(it.riderId, it.distanceMeters) }
            .toList()

    /**
     * 공간 인덱스가 없어 후보는 항상 전체 라이더다.
     * HLEN 으로 건수만 읽어, 후보수 관측이 조회 경로의 전송량 측정에 섞이지 않게 한다.
     */
    override fun candidateCount(center: Coordinate, radiusMeters: Int): Int =
        stringRedisTemplate.opsForHash<String, String>().size(LOCATION_KEY).toInt()

    override fun clear() {
        stringRedisTemplate.delete(LOCATION_KEY)
    }

    private fun measure(riderIdText: String, recordText: String, center: Coordinate): MeasuredRider {
        val record = RiderLocationRecord.parse(recordText)
        return MeasuredRider(riderIdText.toLong(), record, center.distanceMetersTo(record.coordinate))
    }

    private data class MeasuredRider(
        val riderId: Long,
        val record: RiderLocationRecord,
        val distanceMeters: Double,
    )

    private companion object {
        private const val LOCATION_KEY = "delivery:rider:remote-full-scan-location"
    }
}
