package com.biuea.delivery.infrastructure.geo

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.NearbyRider
import com.biuea.delivery.domain.rider.RiderLocation
import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * 셀 키 → 라이더 ID SET, 라이더 ID → 위치 레코드 HASH 로 이루어진 공통 저장 구조.
 *
 * Geohash 와 H3 는 "좌표를 어떤 셀 문자열로 바꾸는가"만 다르고 저장·조회 방식은 같다.
 * 그래서 셀 계산은 각 인덱스가, Redis 접근은 이 클래스가 맡는다.
 *
 * 키에 TTL 을 두지 않는 이유: 라이더 위치는 주행 중 계속 덮어써지는 상시 인덱스이고,
 * 오래된 좌표는 도메인의 30초 신선도 필터가 걸러낸다. 퇴직 라이더 정리는 별도 스위퍼의 몫이다.
 */
class CellRiderLocationStore(
    private val stringRedisTemplate: StringRedisTemplate,
    private val cellKeyPrefix: String,
    private val locationKey: String,
) {

    fun findRecordBy(riderId: Long): RiderLocationRecord? =
        stringRedisTemplate.opsForHash<String, String>()
            .get(locationKey, riderId.toString())
            ?.let { RiderLocationRecord.parse(it) }

    /**
     * 셀 이동(SREM/SADD)과 위치 해시 갱신을 파이프라인 한 번으로 보낸다.
     * 정밀도 3개를 개별 왕복하면 갱신 한 건에 왕복이 7번 필요해 처리량이 왕복 수에 지배당한다.
     */
    fun save(location: RiderLocation, previousCells: List<String>, currentCells: List<String>) {
        val riderIdText = location.riderId.toString()
        val recordText = RiderLocationRecord.from(location).serialize()
        val leftCells = previousCells - currentCells.toSet()
        val enteredCells = currentCells - previousCells.toSet()
        stringRedisTemplate.executePipelined(
            RedisCallback<Any?> { connection ->
                val stringConnection = connection as StringRedisConnection
                leftCells.forEach { stringConnection.sRem(cellKeyOf(it), riderIdText) }
                enteredCells.forEach { stringConnection.sAdd(cellKeyOf(it), riderIdText) }
                stringConnection.hSet(locationKey, riderIdText, recordText)
                null
            },
        )
    }

    /** 여러 셀의 합집합을 한 번의 SUNION 으로 가져온다 — 셀마다 SMEMBERS 를 돌리면 왕복이 셀 수만큼 늘어난다. */
    fun candidateRiderIds(cells: List<String>): Set<String> =
        stringRedisTemplate.opsForSet().union(cells.map { cellKeyOf(it) }) ?: emptySet()

    fun nearestRiders(
        candidateRiderIds: Set<String>,
        center: Coordinate,
        radiusMeters: Int,
        limit: Int,
    ): List<NearbyRider> {
        if (candidateRiderIds.isEmpty()) return emptyList()
        val riderIdTexts = candidateRiderIds.toList()
        val recordTexts = stringRedisTemplate.opsForHash<String, String>().multiGet(locationKey, riderIdTexts)
        return riderIdTexts.asSequence()
            .zip(recordTexts.asSequence())
            .mapNotNull { (riderIdText, recordText) -> measure(riderIdText, recordText, center) }
            .filter { it.distanceMeters <= radiusMeters }
            .sortedBy { it.distanceMeters }
            .take(limit)
            .map { it.record.toNearbyRider(it.riderId, it.distanceMeters) }
            .toList()
    }

    fun clear() {
        // KEYS 대신 SCAN 으로 셀 키를 훑는다. 초기화는 테스트·벤치마크 전용 경로다.
        val cellKeys = stringRedisTemplate
            .scan(ScanOptions.scanOptions().match("$cellKeyPrefix*").count(SCAN_BATCH_SIZE).build())
            .use { cursor: Cursor<String> -> cursor.asSequence().toList() }
        if (cellKeys.isNotEmpty()) {
            stringRedisTemplate.delete(cellKeys)
        }
        stringRedisTemplate.delete(locationKey)
    }

    private fun measure(riderIdText: String, recordText: String?, center: Coordinate): MeasuredCandidate? {
        val record = recordText?.let { RiderLocationRecord.parse(it) } ?: return null
        return MeasuredCandidate(riderIdText.toLong(), record, center.distanceMetersTo(record.coordinate))
    }

    private fun cellKeyOf(cell: String): String = "$cellKeyPrefix$cell"

    private data class MeasuredCandidate(
        val riderId: Long,
        val record: RiderLocationRecord,
        val distanceMeters: Double,
    )

    private companion object {
        private const val SCAN_BATCH_SIZE = 1_000L
    }
}
