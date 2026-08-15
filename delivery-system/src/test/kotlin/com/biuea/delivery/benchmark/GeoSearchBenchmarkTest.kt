package com.biuea.delivery.benchmark

import com.biuea.delivery.domain.geo.Coordinate
import com.biuea.delivery.domain.rider.RiderLocation
import com.biuea.delivery.domain.rider.RiderLocationIndex
import com.biuea.delivery.infrastructure.geo.RiderLocationIndexes
import com.biuea.delivery.infrastructure.geo.RiderLocationRecord
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.ZonedDateTime
import java.util.Properties
import java.util.Random
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 라이더 30,000명을 서울 좌표 범위에 뿌려 다섯 인덱스 구현의 반경 검색 지연과 갱신 처리량을 실측한다.
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
            measure(
                implementationName,
                riderLocationIndex,
                stringRedisTemplate,
                riderLocations,
                warmupCenters,
                queryCenters,
            )
        }
        val distanceProfilesByImplementation = indexes.associate { (implementationName, riderLocationIndex) ->
            implementationName to correctnessCenters.map { center -> distanceProfile(riderLocationIndex, center) }
        }
        val deviationsByImplementation = distanceProfilesByImplementation.mapValues { (_, profiles) ->
            maxDistanceDeviationMeters(profiles, distanceProfilesByImplementation.getValue(FULL_SCAN_NAME))
        }

        printSearchLatencyTable(measurements, deviationsByImplementation)
        printRedisTrafficTable(measurements, averageLocationEntryBytes(riderLocations))
        printUpdateThroughputTable(measurements)

        verifyRemoteFullScanBaseline(measurements)

        // 측정값이 의미를 가지려면 다섯 구현이 같은 라이더를 찾아야 한다.
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
        stringRedisTemplate: StringRedisTemplate,
        riderLocations: List<RiderLocation>,
        warmupCenters: List<Coordinate>,
        queryCenters: List<Coordinate>,
    ): BenchmarkMeasurement {
        riderLocationIndex.clear()
        val loadStartedAt = System.nanoTime()
        riderLocations.forEach { riderLocationIndex.update(it) }
        val loadSeconds = (System.nanoTime() - loadStartedAt) / NANOS_PER_SECOND

        warmupCenters.forEach { riderLocationIndex.searchWithin(it, SEARCH_RADIUS_METERS, SEARCH_LIMIT) }

        // 트래픽 스냅샷은 질의 구간만 감싼다 — 적재·후보 표본·이동 갱신이 섞이면 질의당 전송량이 오염된다.
        val trafficBeforeQueries = captureRedisTraffic(stringRedisTemplate)
        val querySamples = queryCenters.map { center -> measureQuery(riderLocationIndex, center) }
        val trafficAfterQueries = captureRedisTraffic(stringRedisTemplate)

        val latenciesMillis = querySamples.map { it.latencyMillis }
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
            averageResultCount = querySamples.map { it.resultCount.toDouble() }.average(),
            redisTraffic = RedisQueryTraffic.of(trafficBeforeQueries, trafficAfterQueries, querySamples.size),
            loadSeconds = loadSeconds,
            updatesPerSecond = movedLocations.size / updateSeconds,
        )
    }

    private fun measureQuery(riderLocationIndex: RiderLocationIndex, center: Coordinate): QuerySample {
        val startedAt = System.nanoTime()
        val riders = riderLocationIndex.searchWithin(center, SEARCH_RADIUS_METERS, SEARCH_LIMIT)
        val latencyMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND
        return QuerySample(latencyMillis, riders.size)
    }

    /**
     * Redis 서버가 집계한 누적 명령 수·출력 바이트를 읽는다.
     *
     * 클라이언트에서 세지 않고 서버 통계를 쓰는 이유: 왕복 수와 전송량은 구현이 "몇 번 호출했는가"가 아니라
     * 서버가 실제로 무엇을 내보냈는가로 판정해야 한다. Lettuce 의 파이프라인·재시도가 섞여도 서버 집계는 흔들리지 않는다.
     */
    private fun captureRedisTraffic(stringRedisTemplate: StringRedisTemplate): RedisTrafficSnapshot {
        val statistics = requireNotNull(
            stringRedisTemplate.execute(
                RedisCallback { connection -> connection.serverCommands().info(REDIS_STATS_SECTION) },
            ),
        ) { "Redis INFO $REDIS_STATS_SECTION 응답이 비었습니다" }
        return RedisTrafficSnapshot(
            commandCount = statisticOf(statistics, COMMANDS_PROCESSED_STATISTIC),
            outputBytes = statisticOf(statistics, OUTPUT_BYTES_STATISTIC),
        )
    }

    private fun statisticOf(statistics: Properties, statisticName: String): Long =
        requireNotNull(statistics.getProperty(statisticName)) {
            "Redis INFO $REDIS_STATS_SECTION 에 $statisticName 이 없습니다"
        }.toLong()

    /**
     * 원격 전수 스캔 베이스라인이 전제대로 동작했는지 확인한다.
     * 왕복이 1회를 넘거나 후보가 전체 라이더가 아니면 어딘가에 공간 인덱스가 끼어든 것이고,
     * 그러면 "인덱스 없는 원격 저장소의 비용"이라는 이 측정의 의미가 사라진다.
     */
    private fun verifyRemoteFullScanBaseline(measurements: List<BenchmarkMeasurement>) {
        val remoteFullScan = measurements.first { it.implementationName == REMOTE_FULL_SCAN_NAME }
        val geohash = measurements.first { it.implementationName == GEOHASH_NAME }
        check(remoteFullScan.redisTraffic.commandsPerQuery in MIN_ROUND_TRIPS_PER_QUERY..MAX_ROUND_TRIPS_PER_QUERY) {
            "RemoteFullScan 의 질의당 Redis 왕복이 1회가 아닙니다: ${remoteFullScan.redisTraffic.commandsPerQuery}"
        }
        check(remoteFullScan.averageCandidateCount == RIDER_COUNT.toDouble()) {
            "RemoteFullScan 의 후보수가 전체 라이더 수가 아닙니다: ${remoteFullScan.averageCandidateCount}"
        }
        check(remoteFullScan.redisTraffic.receivedBytesPerQuery > geohash.redisTraffic.receivedBytesPerQuery) {
            "RemoteFullScan 이 Geohash 보다 적게 전송했습니다: " +
                "${remoteFullScan.redisTraffic.receivedBytesPerQuery} vs ${geohash.redisTraffic.receivedBytesPerQuery}"
        }
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
        println("* FullScan 은 JVM 힙 순회(왕복 0회), RemoteFullScan 은 같은 전수 스캔을 Redis 전 건 전송으로 수행한다.")
        println("* 거리오차 = 전수 스캔과 같은 순위의 라이더까지 거리 차이의 최댓값 (질의 ${CORRECTNESS_QUERY_COUNT}회 기준).")
    }

    /**
     * 질의 1회가 Redis 에서 끌어오는 라이더 건수·전송량·왕복 수.
     *
     * 지연 표만 보면 "셀 기반이 인메모리 전수 스캔보다 느리다"로 읽히는데, 그 차이의 원인이
     * 인덱스 방식인지 원격 저장소 전송량인지는 이 표가 갈라준다.
     */
    private fun printRedisTrafficTable(measurements: List<BenchmarkMeasurement>, locationEntryBytes: Int) {
        println()
        println(
            "=== 질의당 Redis 전송량·왕복 (질의 ${QUERY_COUNT}회 평균 / 라이더 1건당 ${locationEntryBytes}바이트) ===",
        )
        println(TRAFFIC_TABLE_SEPARATOR)
        println(
            String.format(
                TRAFFIC_ROW_FORMAT,
                "구현", "읽은 라이더수", "전송량 추정", "전송량 실측", "왕복(실측)", "읽기 구성",
            ),
        )
        println(TRAFFIC_TABLE_SEPARATOR)
        measurements.forEach { measurement ->
            val readProfile = READ_PROFILES_BY_IMPLEMENTATION.getValue(measurement.implementationName)
            val ridersReadPerQuery = readProfile.ridersReadPerQuery(measurement)
            println(
                String.format(
                    TRAFFIC_ROW_FORMAT,
                    measurement.implementationName,
                    String.format("%,.1f", ridersReadPerQuery),
                    formatBytes(ridersReadPerQuery * locationEntryBytes),
                    formatBytes(measurement.redisTraffic.receivedBytesPerQuery),
                    String.format("%.2f회", measurement.redisTraffic.commandsPerQuery),
                    readProfile.readComposition,
                ),
            )
        }
        println(TRAFFIC_TABLE_SEPARATOR)
        println(
            "* 전송량 추정 = 읽은 라이더 수 × 라이더 1건당 직렬화 크기(${locationEntryBytes}B: " +
                "HASH 필드 라이더 ID + \"위도|경도|갱신시각\" 레코드 + RESP 오버헤드 ${RESP_BULK_OVERHEAD_BYTES}B×2, " +
                "적재한 라이더 ${RIDER_COUNT}명의 실제 평균).",
        )
        println(
            "* 전송량 실측·왕복 실측 = Redis INFO $REDIS_STATS_SECTION 의 " +
                "$OUTPUT_BYTES_STATISTIC·$COMMANDS_PROCESSED_STATISTIC 증분 / 질의 수.",
        )
        println("* 실측값에는 구간 종료 시 INFO 호출 1회가 섞여 질의당 +${String.format("%.3f", 1.0 / QUERY_COUNT)}회의 오차가 있다.")
        println("* RedisGeo 는 서버가 필터·정렬·limit 을 끝내므로 읽은 건수 = 결과 건수(최대 $SEARCH_LIMIT)다.")
    }

    /**
     * 라이더 1건이 응답에 실려 오는 평균 바이트.
     *
     * 매직 넘버나 대표 좌표 하나로 잡지 않고 적재한 30,000건의 실제 직렬화 크기를 평균한다 —
     * 좌표 소수 자릿수가 레코드 길이를 좌우해서, 짧은 대표 좌표로 잡으면 추정이 실측보다 20% 이상 작아진다.
     */
    private fun averageLocationEntryBytes(riderLocations: List<RiderLocation>): Int =
        riderLocations.map { location ->
            (
                location.riderId.toString().toByteArray().size +
                    RiderLocationRecord.from(location).serialize().toByteArray().size +
                    RESP_BULK_OVERHEAD_BYTES * BULK_STRINGS_PER_RIDER
                ).toDouble()
        }.average().roundToInt()

    private fun formatBytes(bytes: Double): String = when {
        bytes >= BYTES_PER_MEGABYTE -> String.format("%.2f MB", bytes / BYTES_PER_MEGABYTE)
        bytes >= BYTES_PER_KILOBYTE -> String.format("%.1f KB", bytes / BYTES_PER_KILOBYTE)
        else -> String.format("%.0f B", bytes)
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
        val averageResultCount: Double,
        val redisTraffic: RedisQueryTraffic,
        val loadSeconds: Double,
        val updatesPerSecond: Double,
    ) {
        fun percentileMillis(percentile: Int): Double {
            val sorted = latenciesMillis.sorted()
            val rank = ceil(sorted.size * percentile / 100.0).toInt().coerceIn(1, sorted.size)
            return sorted[rank - 1]
        }
    }

    /** 질의 1회의 지연과 결과 건수. 결과 건수는 RedisGeo 의 전송량 산정 기준이라 함께 남긴다. */
    private data class QuerySample(
        val latencyMillis: Double,
        val resultCount: Int,
    )

    /** Redis 서버가 집계한 누적 명령 수·출력 바이트. */
    private data class RedisTrafficSnapshot(
        val commandCount: Long,
        val outputBytes: Long,
    )

    /** 질의 구간의 서버 통계 증분을 질의당으로 환산한 값. */
    private data class RedisQueryTraffic(
        val commandsPerQuery: Double,
        val receivedBytesPerQuery: Double,
    ) {
        companion object {
            fun of(before: RedisTrafficSnapshot, after: RedisTrafficSnapshot, queryCount: Int): RedisQueryTraffic =
                RedisQueryTraffic(
                    commandsPerQuery = (after.commandCount - before.commandCount).toDouble() / queryCount,
                    receivedBytesPerQuery = (after.outputBytes - before.outputBytes).toDouble() / queryCount,
                )
        }
    }

    /** 질의 1회가 Redis 에서 읽는 라이더 건수와 그 구성. 전송량 추정의 건수 항이 구현마다 다른 이유를 함께 출력한다. */
    private data class RiderReadProfile(
        val readComposition: String,
        val ridersReadPerQuery: (BenchmarkMeasurement) -> Double,
    )

    private companion object {
        private const val RIDER_COUNT = 30_000
        private const val QUERY_COUNT = 500
        private const val WARMUP_QUERY_COUNT = 50
        private const val CANDIDATE_SAMPLE_COUNT = 100
        private const val CORRECTNESS_QUERY_COUNT = 20
        private const val MAX_DISTANCE_DEVIATION_METERS = 3.0
        private const val LOCATION_UPDATE_COUNT = 5_000
        private const val SEARCH_RADIUS_METERS = 3_000

        /**
         * 인덱스에서 꺼내 다음 필터 단계로 넘길 후보 수.
         *
         * 배차 정책의 브로드캐스트 대상이 20명이므로 같은 값으로 맞춘다.
         * 이 값을 바꿔도 전수 스캔과 셀 기반 구현은 후보를 전부 끌어온 뒤 마지막에 자르므로 영향이 없다.
         * 서버가 limit 까지 처리하는 RedisGeo 만 전송량이 비례해 움직인다.
         */
        private const val SEARCH_LIMIT = 20

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
        private const val REMOTE_FULL_SCAN_NAME = "RemoteFullScan"
        private const val GEOHASH_NAME = "Geohash"
        private const val REDIS_GEO_NAME = "RedisGeo"

        private const val REDIS_STATS_SECTION = "stats"
        private const val COMMANDS_PROCESSED_STATISTIC = "total_commands_processed"
        private const val OUTPUT_BYTES_STATISTIC = "total_net_output_bytes"

        /** RemoteFullScan 은 HGETALL 한 번이므로 질의당 왕복이 1회여야 한다 (INFO 1회 오차 허용). */
        private const val MIN_ROUND_TRIPS_PER_QUERY = 0.9
        private const val MAX_ROUND_TRIPS_PER_QUERY = 1.1

        /** RESP 벌크 문자열 1개의 프로토콜 오버헤드 — `$41\r\n` + 끝의 `\r\n`. */
        private const val RESP_BULK_OVERHEAD_BYTES = 7

        /** 라이더 1건이 차지하는 벌크 문자열 수 — 라이더 ID 와 위치 레코드 두 개다 (HGETALL·SUNION+HMGET 모두 동일). */
        private const val BULK_STRINGS_PER_RIDER = 2
        private const val BYTES_PER_KILOBYTE = 1_024.0
        private const val BYTES_PER_MEGABYTE = 1_024.0 * 1_024.0

        private const val SEARCH_ROW_FORMAT = "%-15s %10s %10s %10s %10s %12s %10s %10s %12s"
        private const val TRAFFIC_ROW_FORMAT = "%-15s %14s %13s %13s %12s  %s"
        private const val UPDATE_ROW_FORMAT = "%-15s %14s %10s  %s"
        private val TABLE_SEPARATOR = "-".repeat(107)
        private val TRAFFIC_TABLE_SEPARATOR = "-".repeat(125)
        private val UPDATE_TABLE_SEPARATOR = "-".repeat(85)

        /** 갱신 1건이 쓰는 Redis 왕복 수와 그 구성. 지연 차이의 원인을 표에서 바로 읽을 수 있게 함께 출력한다. */
        private val ROUND_TRIPS_BY_IMPLEMENTATION = mapOf(
            FULL_SCAN_NAME to ("0" to "인메모리 맵 갱신"),
            REMOTE_FULL_SCAN_NAME to ("1" to "HSET 만 — 셀 색인이 없어 SREM/SADD 도 이전 위치 조회도 없다"),
            GEOHASH_NAME to ("2~8" to "HGET + 정밀도 3개의 SREM/SADD(셀 이동 시) + HSET"),
            "H3" to ("2~8" to "HGET + 해상도 3개의 SREM/SADD(셀 이동 시) + HSET"),
            REDIS_GEO_NAME to ("2" to "GEOADD + HSET (이전 위치 삭제 불필요)"),
        )

        /** 질의 1회가 Redis 에서 읽는 라이더 건수. 구현마다 "무엇을 끌어오는가"가 달라 전송량이 갈린다. */
        private val READ_PROFILES_BY_IMPLEMENTATION = mapOf(
            FULL_SCAN_NAME to RiderReadProfile("네트워크 없음 — JVM 힙의 ConcurrentHashMap 순회") { 0.0 },
            REMOTE_FULL_SCAN_NAME to
                RiderReadProfile("HGETALL — 공간 인덱스가 없어 전체 라이더를 매 질의마다 전송") { RIDER_COUNT.toDouble() },
            GEOHASH_NAME to RiderReadProfile("SUNION(후보 ID) + HMGET(후보 위치) — 후보 전체") { it.averageCandidateCount },
            "H3" to RiderReadProfile("SUNION(후보 ID) + HMGET(후보 위치) — 후보 전체") { it.averageCandidateCount },
            REDIS_GEO_NAME to
                RiderReadProfile("GEOSEARCH(서버가 필터·정렬·limit) + HMGET(결과 갱신시각)") { it.averageResultCount },
        )
    }
}
