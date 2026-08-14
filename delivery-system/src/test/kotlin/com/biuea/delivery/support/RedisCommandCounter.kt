package com.biuea.delivery.support

import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.Properties

/**
 * Redis 서버가 처리한 누적 명령 수(`INFO stats` 의 `total_commands_processed`)를 읽는다.
 *
 * "폴링 대기와 pub/sub 대기 중 어느 쪽이 Redis 를 더 많이 때리는가" 는 클라이언트 코드로는 알 수 없다.
 * 스핀락은 20ms 마다 SET NX 를 던지고, Redisson 은 구독 후 블로킹하다 PUBLISH 로 깨어난다 —
 * 둘의 차이는 서버가 실제로 처리한 명령 수로만 드러난다. 그 수치가 폴링 병목 가설의 직접 근거다.
 *
 * 주의: INFO 자체도 1건으로 집계된다. 구간 시작 시점의 INFO 1건이 증분에 섞인다(상수 +1).
 * 수백~수만 건 규모를 비교하는 용도라 보정하지 않는다.
 */
class RedisCommandCounter(private val stringRedisTemplate: StringRedisTemplate) {

    fun totalCommandsProcessed(): Long {
        val statistics = stringRedisTemplate.execute(
            RedisCallback<Properties> { connection -> connection.serverCommands().info(STATS_SECTION) },
        )
        val processedCommands = requireNotNull(statistics?.getProperty(TOTAL_COMMANDS_PROCESSED)) {
            "INFO $STATS_SECTION 응답에 $TOTAL_COMMANDS_PROCESSED 가 없다"
        }
        return processedCommands.toLong()
    }

    /** block 을 실행하는 동안 서버가 처리한 명령 수를 결과와 함께 돌려준다. */
    fun <T> countDuring(block: () -> T): CountedCommands<T> {
        val countBefore = totalCommandsProcessed()
        val result = block()
        return CountedCommands(result, totalCommandsProcessed() - countBefore)
    }

    /**
     * 명령별 호출 수(`INFO commandstats` 의 `cmdstat_{명령}:calls=N`).
     *
     * 총량만으로는 "무엇 때문에 명령이 늘었는지" 를 알 수 없다. 폴링의 `set` 과 pub/sub 의
     * `subscribe`·`publish`·`eval` 을 분리해야 대기 방식의 비용 구조를 말할 수 있다.
     */
    fun commandCallCounts(): Map<String, Long> {
        val commandStatistics = stringRedisTemplate.execute(
            RedisCallback<Properties> { connection -> connection.serverCommands().info(COMMAND_STATS_SECTION) },
        )
        return commandStatistics
            ?.stringPropertyNames()
            .orEmpty()
            .filter { it.startsWith(COMMAND_STAT_PREFIX) }
            .associate { propertyName ->
                propertyName.removePrefix(COMMAND_STAT_PREFIX) to callCountOf(commandStatistics?.getProperty(propertyName))
            }
    }

    /** block 실행 구간에서 늘어난 명령별 호출 수만 남긴다. */
    fun <T> countCallsByCommandDuring(block: () -> T): CountedCommandCalls<T> {
        val countsBefore = commandCallCounts()
        val result = block()
        val callCountsByCommand = commandCallCounts()
            .mapValues { (command, callCount) -> callCount - countsBefore.getOrDefault(command, 0L) }
            .filterValues { it > 0 }
        return CountedCommandCalls(result, callCountsByCommand)
    }

    private fun callCountOf(commandStatistic: String?): Long =
        commandStatistic
            ?.split(FIELD_SEPARATOR)
            ?.firstOrNull { it.startsWith(CALLS_FIELD_PREFIX) }
            ?.removePrefix(CALLS_FIELD_PREFIX)
            ?.toLong()
            ?: 0L

    companion object {
        private const val STATS_SECTION = "stats"
        private const val TOTAL_COMMANDS_PROCESSED = "total_commands_processed"
        private const val COMMAND_STATS_SECTION = "commandstats"
        private const val COMMAND_STAT_PREFIX = "cmdstat_"
        private const val CALLS_FIELD_PREFIX = "calls="
        private const val FIELD_SEPARATOR = ","
    }
}

data class CountedCommands<T>(val result: T, val commandCount: Long)

data class CountedCommandCalls<T>(val result: T, val callCountsByCommand: Map<String, Long>) {
    /** 사람이 표로 읽을 수 있게 호출 수 내림차순 문자열로 만든다. */
    fun formatted(): String =
        callCountsByCommand.entries
            .sortedByDescending { it.value }
            .joinToString(", ") { (command, callCount) -> "$command=$callCount" }
}
