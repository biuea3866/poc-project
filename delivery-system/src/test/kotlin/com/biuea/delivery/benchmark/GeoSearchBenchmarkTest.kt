package com.biuea.delivery.benchmark

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.RiderLocation
import com.biuea.delivery.domain.rider.RiderLocationIndex
import com.biuea.delivery.infrastructure.geo.RiderLocationIndexes
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.Random
import kotlin.math.abs
import kotlin.math.ceil

/**
 * 라이더 30,000명을 서울 좌표 범위에 뿌려 네 인덱스 구현의 반경 검색 지연과 갱신 처리량을 실측한다.
 *
 * JUnit 애노테이션을 쓰는 이유: 벤치마크 태스크(benchmarkTest)가 JUnit 플랫폼 태그로 대상을 고르는데,
 * Kotest 5.8 의 @Tags 는 플랫폼 태그로 노출되지 않아 태그 필터에 걸리지 않는다.
 * 동작 검증 테스트는 전부 Kotest 로 작성했고, 이 파일만 측정 하네스 목적으로 예외를 둔다.
 */
@Tag("benchmark")
class GeoSearchBenchmarkTest {

    @Test
    fun `반경 검색 지연과 후보 축소 효과를 실측한다`() {
        val stringRedisTemplate = RiderLocationIndexes.createStringRedisTemplate()
        val indexes = RiderLocationIndexes.createAll(stringRedisTemplate)
        val riderLocations = createRiderLocations()
        val warmupCenters = createQueryCenters(WARMUP_QUERY_COUNT, seed = 99)
        val queryCenters = createQueryCenters(QUERY_COUNT, seed = 1_234)
        val correctnessCenters = createQueryCenters(CORRECTNESS_QUERY_COUNT, seed = 7)

        val measurements = indexes.map { (implementationName, riderLocationIndex) ->
            measure(implementationName, riderLocationIndex, riderLocations, warmupCenters, queryCenters)
        }
        val distanceProfilesByImplementation = indexes.associate { (implementationName, riderLocationIndex) ->
            implementationName to correctnessCenters.map { center -> distanceProfile(riderLocationIndex, center) }
        }
        val deviationsByImplementation = distanceProfilesByImplementation.mapValues { (_, profiles) ->
            maxDistanceDeviationMeters(profiles, distanceProfilesByImplementation.getValue(FULL_SCAN_NAME))
        }

        printSearchLatencyTable(measurements, deviationsByImplementation)
        printUpdateThroughputTable(measurements)

        // 측정값이 의미를 가지려면 네 구현이 같은 라이더를 찾아야 한다.
        // 순서가 아니라 거리 프로파일로 비교하는 이유: Redis GEO 는 좌표를 52비트 geohash 로 양자화해
        // 0.6m 오차가 생기고, 밀집 지역에서는 이 오차만으로 근소한 차이의 두 라이더 순위가 뒤집힌다.
        // 라이더를 실제로 놓쳤다면 거리 프로파일이 수십 m 단위로 어긋나므로 이 검증으로 충분히 잡힌다.
        deviationsByImplementation.forEach { (implementationName, deviationMeters) ->
            check(deviationMeters <= MAX_DISTANCE_DEVIATION_METERS) {
                "$implementationName 의 거리 프로파일이 전수 스캔과 ${deviationMeters}m 어긋났습니다"
            }
        }
        measurements.map { it.implementationName } shouldContainExactly indexes.map { it.first }
    }

    private fun measure(
        implementationName: String,
        riderLocationIndex: RiderLocationIndex,
        riderLocations: List<RiderLocation>,
        warmupCenters: List<Coordinate>,
        queryCenters: List<Coordinate>,
    ): BenchmarkMeasurement {
        riderLocationIndex.clear()
        val loadStartedAt = System.nanoTime()
        riderLocations.forEach { riderLocationIndex.update(it) }
        val loadSeconds = (System.nanoTime() - loadStartedAt) / NANOS_PER_SECOND

        warmupCenters.forEach { riderLocationIndex.searchWithin(it, SEARCH_RADIUS_METERS, SEARCH_LIMIT) }

        val latenciesMillis = queryCenters.map { center ->
            val startedAt = System.nanoTime()
            riderLocationIndex.searchWithin(center, SEARCH_RADIUS_METERS, SEARCH_LIMIT)
            (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND
        }
        val averageCandidateCount = queryCenters.take(CANDIDATE_SAMPLE_COUNT)
            .map { riderLocationIndex.candidateCount(it, SEARCH_RADIUS_METERS).toDouble() }
            .average()

        // 이미 30,000명이 적재된 상태에서 라이더가 이동하는 상황 — 셀 이동(SREM/SADD)이 실제로 발생한다.
        val movedLocations = createMovedLocations(riderLocations)
        val updateStartedAt = System.nanoTime()
        movedLocations.forEach { riderLocationIndex.update(it) }
        val updateSeconds = (System.nanoTime() - updateStartedAt) / NANOS_PER_SECOND

        return BenchmarkMeasurement(
            implementationName = implementationName,
            latenciesMillis = latenciesMillis,
            averageCandidateCount = averageCandidateCount,
            loadSeconds = loadSeconds,
            updatesPerSecond = movedLocations.size / updateSeconds,
        )
    }

    /** 한 질의의 결과를 가까운 순 거리 목록으로 뽑는다. 구현 간 비교의 기준값이다. */
    private fun distanceProfile(riderLocationIndex: RiderLocationIndex, center: Coordinate): List<Double> =
        riderLocationIndex.searchWithin(center, SEARCH_RADIUS_METERS, SEARCH_LIMIT).map { it.distanceMeters }

    private fun maxDistanceDeviationMeters(
        profiles: List<List<Double>>,
        fullScanProfiles: List<List<Double>>,
    ): Double =
        profiles.zip(fullScanProfiles).maxOf { (profile, fullScanProfile) ->
            check(profile.size == fullScanProfile.size) {
                "결과 개수가 전수 스캔과 다릅니다: ${profile.size} vs ${fullScanProfile.size}"
            }
            profile.zip(fullScanProfile).maxOfOrNull { (distance, fullScanDistance) ->
                abs(distance - fullScanDistance)
            } ?: 0.0
        }

    private fun printSearchLatencyTable(
        measurements: List<BenchmarkMeasurement>,
        deviationsByImplementation: Map<String, Double>,
    ) {
        println()
        println(
            "=== 반경 검색 지연 (라이더 ${RIDER_COUNT}명 / 반경 ${SEARCH_RADIUS_METERS}m / " +
                "질의 ${QUERY_COUNT}회 / limit $SEARCH_LIMIT) ===",
        )
        println(TABLE_SEPARATOR)
        println(
            String.format(
                SEARCH_ROW_FORMAT,
                "구현", "P50(ms)", "P95(ms)", "P99(ms)", "평균(ms)", "평균 후보수", "후보 축소", "적재(초)", "거리오차(m)",
            ),
        )
        println(TABLE_SEPARATOR)
        measurements.forEach { measurement ->
            println(
                String.format(
                    SEARCH_ROW_FORMAT,
                    measurement.implementationName,
                    format(measurement.percentileMillis(50)),
                    format(measurement.percentileMillis(95)),
                    format(measurement.percentileMillis(99)),
                    format(measurement.latenciesMillis.average()),
                    String.format("%.1f", measurement.averageCandidateCount),
                    String.format("%.1f배", RIDER_COUNT / measurement.averageCandidateCount),
                    String.format("%.1f", measurement.loadSeconds),
                    String.format("%.3f", deviationsByImplementation.getValue(measurement.implementationName)),
                ),
            )
        }
        println(TABLE_SEPARATOR)
        println("* 후보수 = 정확 거리 계산 전에 인덱스가 훑은 라이더 수. 축소 배수 = 전체 라이더 수 / 평균 후보수.")
        println("* RedisGeo 는 서버가 반경 필터까지 끝내고 결과만 돌려주므로 후보수 = 반경 안 라이더 수다 (내부 스캔량은 관측 불가).")
        println("* 거리오차 = 전수 스캔과 같은 순위의 라이더까지 거리 차이의 최댓값 (질의 ${CORRECTNESS_QUERY_COUNT}회 기준).")
    }

    private fun printUpdateThroughputTable(measurements: List<BenchmarkMeasurement>) {
        println()
        println("=== 위치 갱신 처리량 (적재 완료 후 라이더 ${LOCATION_UPDATE_COUNT}명 이동) ===")
        println(UPDATE_TABLE_SEPARATOR)
        println(String.format(UPDATE_ROW_FORMAT, "구현", "초당 갱신", "건당 왕복", "왕복 구성"))
        println(UPDATE_TABLE_SEPARATOR)
        measurements.forEach { measurement ->
            val roundTrip = ROUND_TRIPS_BY_IMPLEMENTATION.getValue(measurement.implementationName)
            println(
                String.format(
                    UPDATE_ROW_FORMAT,
                    measurement.implementationName,
                    String.format("%,.0f건/s", measurement.updatesPerSecond),
                    roundTrip.first,
                    roundTrip.second,
                ),
            )
        }
        println(UPDATE_TABLE_SEPARATOR)
        println()
    }

    private fun format(millis: Double): String = String.format("%.3f", millis)

    private fun createRiderLocations(): List<RiderLocation> {
        val random = Random(RIDER_SEED)
        val updatedAt = ZonedDateTime.now()
        return (1L..RIDER_COUNT.toLong()).map { riderId ->
            RiderLocation(riderId, randomCoordinate(random, riderId % 10 < DENSE_RIDER_RATIO), updatedAt)
        }
    }

    /** 라이더의 10%가 반경 500m 안에서 이동한 상황. 셀 경계를 넘는 이동이 섞이도록 방향을 무작위로 잡는다. */
    private fun createMovedLocations(riderLocations: List<RiderLocation>): List<RiderLocation> {
        val random = Random(MOVE_SEED)
        val updatedAt = ZonedDateTime.now()
        return riderLocations.take(LOCATION_UPDATE_COUNT).map { location ->
            RiderLocation(
                location.riderId,
                Coordinate(
                    location.coordinate.latitude + (random.nextDouble() - 0.5) * MOVE_DEGREES,
                    location.coordinate.longitude + (random.nextDouble() - 0.5) * MOVE_DEGREES,
                ),
                updatedAt,
            )
        }
    }

    private fun createQueryCenters(count: Int, seed: Long): List<Coordinate> {
        val random = Random(seed)
        return (1..count).map { index -> randomCoordinate(random, index % 10 < DENSE_QUERY_RATIO) }
    }

    /** 밀집 지역(강남)은 정규분포로, 그 외는 서울 사각 범위 균등분포로 뿌려 실제 편중을 재현한다. */
    private fun randomCoordinate(random: Random, dense: Boolean): Coordinate =
        if (dense) {
            Coordinate(
                GANGNAM_LATITUDE + random.nextGaussian() * DENSE_SIGMA_DEGREES,
                GANGNAM_LONGITUDE + random.nextGaussian() * DENSE_SIGMA_DEGREES,
            )
        } else {
            Coordinate(
                SEOUL_MIN_LATITUDE + random.nextDouble() * (SEOUL_MAX_LATITUDE - SEOUL_MIN_LATITUDE),
                SEOUL_MIN_LONGITUDE + random.nextDouble() * (SEOUL_MAX_LONGITUDE - SEOUL_MIN_LONGITUDE),
            )
        }

    private data class BenchmarkMeasurement(
        val implementationName: String,
        val latenciesMillis: List<Double>,
        val averageCandidateCount: Double,
        val loadSeconds: Double,
        val updatesPerSecond: Double,
    ) {
        fun percentileMillis(percentile: Int): Double {
            val sorted = latenciesMillis.sorted()
            val rank = ceil(sorted.size * percentile / 100.0).toInt().coerceIn(1, sorted.size)
            return sorted[rank - 1]
        }
    }

    private companion object {
        private const val RIDER_COUNT = 30_000
        private const val QUERY_COUNT = 500
        private const val WARMUP_QUERY_COUNT = 50
        private const val CANDIDATE_SAMPLE_COUNT = 100
        private const val CORRECTNESS_QUERY_COUNT = 20
        private const val MAX_DISTANCE_DEVIATION_METERS = 3.0
        private const val LOCATION_UPDATE_COUNT = 5_000
        private const val SEARCH_RADIUS_METERS = 3_000
        private const val SEARCH_LIMIT = 10

        private const val RIDER_SEED = 42L
        private const val MOVE_SEED = 4_242L
        private const val DENSE_RIDER_RATIO = 4
        private const val DENSE_QUERY_RATIO = 7
        private const val MOVE_DEGREES = 0.009

        private const val GANGNAM_LATITUDE = 37.4979
        private const val GANGNAM_LONGITUDE = 127.0276
        private const val DENSE_SIGMA_DEGREES = 0.012
        private const val SEOUL_MIN_LATITUDE = 37.45
        private const val SEOUL_MAX_LATITUDE = 37.65
        private const val SEOUL_MIN_LONGITUDE = 126.85
        private const val SEOUL_MAX_LONGITUDE = 127.15

        private const val NANOS_PER_MILLISECOND = 1_000_000.0
        private const val NANOS_PER_SECOND = 1_000_000_000.0

        private const val FULL_SCAN_NAME = "FullScan"
        private const val SEARCH_ROW_FORMAT = "%-10s %10s %10s %10s %10s %12s %10s %10s %10s"
        private const val UPDATE_ROW_FORMAT = "%-10s %14s %10s  %s"
        private val TABLE_SEPARATOR = "-".repeat(100)
        private val UPDATE_TABLE_SEPARATOR = "-".repeat(80)

        /** 갱신 1건이 쓰는 Redis 왕복 수와 그 구성. 지연 차이의 원인을 표에서 바로 읽을 수 있게 함께 출력한다. */
        private val ROUND_TRIPS_BY_IMPLEMENTATION = mapOf(
            "FullScan" to ("0" to "인메모리 맵 갱신"),
            "Geohash" to ("2~8" to "HGET + 정밀도 3개의 SREM/SADD(셀 이동 시) + HSET"),
            "H3" to ("2~8" to "HGET + 해상도 3개의 SREM/SADD(셀 이동 시) + HSET"),
            "RedisGeo" to ("2" to "GEOADD + HSET (이전 위치 삭제 불필요)"),
        )
    }
}
