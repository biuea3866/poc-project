package com.biuea.chat.e2e

import com.biuea.chat.support.ChatCluster
import com.biuea.chat.support.CollectingHandler
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.ceil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.web.socket.TextMessage

/**
 * 전달 지연(P50/P95/P99)과 처리량(TPS)을 실측하는 벤치마크.
 * 3개 인스턴스(단일 JVM, 공유 clock)를 띄우고 발신 시각을 메시지에 실어 지연을 계산한다.
 *
 * - 지연: 순차 1건씩(무포화) 보내 큐 대기 없는 실제 전달 지연을 잰다.
 * - 처리량: 연속 발행(burst)해 초당 전달량을 잰다.
 *
 * 기본 test 에서 제외되고 `./gradlew benchmarkTest` 로만 실행된다. 값은 실행 머신에 의존하는 참고치다.
 */
@Tag("benchmark")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatBenchmarkTest {

    private lateinit var cluster: ChatCluster
    private val contentMillis = Regex("\"content\":\"(\\d+)\"")

    @BeforeAll
    fun bootstrap() {
        cluster = ChatCluster(listOf("server-1", "server-2", "server-3"))
    }

    @AfterAll
    fun shutdown() {
        cluster.close()
    }

    @Test
    fun `지연과 처리량을 실측한다`() {
        val latency = measureDirectLatency(samples = 500, warmup = 50)
        val directTps = measureDirectThroughput(messages = 5000)
        val fanoutTps = measureFanoutThroughput(memberCount = 150, messages = 100)

        val report = buildString {
            appendLine("# 채팅 시스템 벤치마크 결과")
            appendLine()
            appendLine("환경: 단일 호스트, 3개 인스턴스(단일 JVM), 로컬 Redis. 값은 참고치다.")
            appendLine()
            appendLine("## 전달 지연 (순차 1건씩, 무포화)")
            appendLine()
            appendLine("| 경로 | 표본 | P50 | P95 | P99 | Max |")
            appendLine("| --- | ---: | ---: | ---: | ---: | ---: |")
            appendLine(latency.latencyRow("크로스 인스턴스 1:1 (인스턴스1→3)"))
            appendLine()
            appendLine("## 처리량 (연속 발행 burst)")
            appendLine()
            appendLine("| 시나리오 | 전달 건수 | 소요(s) | 처리량 |")
            appendLine("| --- | ---: | ---: | ---: |")
            appendLine(directTps.throughputRow("직접 1:1", "msg/s"))
            appendLine(fanoutTps.throughputRow("그룹 팬아웃 150명(3인스턴스 분산)", "deliveries/s"))
        }
        File("build/reports/benchmark.md").apply { parentFile.mkdirs() }.writeText(report)
        println(report)

        assertThat(latency.count).isEqualTo(500)
        assertThat(directTps.count).isEqualTo(5000)
        assertThat(fanoutTps.count).isEqualTo(150 * 100)
    }

    /** 순차 1건씩: 보내고 그 메시지가 도착할 때까지 기다린 뒤 다음을 보낸다 (큐 대기 없음). */
    private fun measureDirectLatency(samples: Int, warmup: Int): Stats {
        val arrival = LinkedBlockingQueue<Long>()
        val receiver = ChatCluster.connect(cluster.ports[2], userId = 2, CollectingHandler { payload ->
            arrival += System.currentTimeMillis() - sendMillisOf(payload)
        })
        val sender = ChatCluster.connect(cluster.ports[0], userId = 1, CollectingHandler {})
        ChatCluster.awaitUntil(10_000) { cluster.sessionLocator().locate(2) != null }

        val latencies = ArrayList<Long>(samples)
        repeat(warmup + samples) { index ->
            sender.sendMessage(TextMessage("""{"type":"direct","receiverId":2,"content":"${System.currentTimeMillis()}"}"""))
            val latency = arrival.poll(5, SECONDS) ?: error("no delivery")
            if (index >= warmup) latencies += latency
        }

        receiver.close(); sender.close()
        return Stats.ofLatency(latencies.sorted())
    }

    /** 크로스 인스턴스 1:1 을 연속 발행해 초당 전달량을 잰다. */
    private fun measureDirectThroughput(messages: Int): Stats {
        val latch = CountDownLatch(messages)
        val receiver = ChatCluster.connect(cluster.ports[2], userId = 4, CollectingHandler { latch.countDown() })
        val sender = ChatCluster.connect(cluster.ports[0], userId = 3, CollectingHandler {})
        ChatCluster.awaitUntil(10_000) { cluster.sessionLocator().locate(4) != null }

        val startNanos = System.nanoTime()
        repeat(messages) {
            sender.sendMessage(TextMessage("""{"type":"direct","receiverId":4,"content":"x"}"""))
        }
        latch.await(120, SECONDS)
        val seconds = (System.nanoTime() - startNanos) / 1e9

        receiver.close(); sender.close()
        return Stats.ofThroughput(messages - latch.count.toInt(), seconds)
    }

    /** 150명 방(3인스턴스 분산)에 연속 발행해 초당 전달량을 잰다. */
    private fun measureFanoutThroughput(memberCount: Int, messages: Int): Stats {
        val roomId = 800L
        val senderId = 999_999L
        cluster.roomRepository().let { rooms ->
            rooms.join(roomId, senderId)
            (1..memberCount).forEach { rooms.join(roomId, it.toLong()) }
        }

        val deliveries = memberCount * messages
        val latch = CountDownLatch(deliveries)
        val sessions = (1..memberCount).map { userId ->
            ChatCluster.connect(cluster.ports[userId % cluster.ports.size], userId.toLong(), CollectingHandler { latch.countDown() })
        }
        val sender = ChatCluster.connect(cluster.ports[0], senderId, CollectingHandler {})
        ChatCluster.awaitUntil(30_000) { (1..memberCount).all { cluster.sessionLocator().locate(it.toLong()) != null } }

        val startNanos = System.nanoTime()
        repeat(messages) {
            sender.sendMessage(TextMessage("""{"type":"room","roomId":800,"content":"x"}"""))
        }
        latch.await(120, SECONDS)
        val seconds = (System.nanoTime() - startNanos) / 1e9

        sessions.forEach { it.close() }; sender.close()
        return Stats.ofThroughput(deliveries - latch.count.toInt(), seconds)
    }

    private fun sendMillisOf(payload: String): Long =
        contentMillis.find(payload)?.groupValues?.get(1)?.toLong() ?: error("no content millis in: $payload")

    private class Stats private constructor(
        val count: Int,
        private val sortedLatencies: List<Long>,
        private val seconds: Double,
    ) {
        fun latencyRow(name: String): String =
            "| $name | $count | ${pct(50.0)}ms | ${pct(95.0)}ms | ${pct(99.0)}ms | ${sortedLatencies.lastOrNull() ?: 0}ms |"

        fun throughputRow(name: String, unit: String): String =
            "| $name | $count | ${"%.2f".format(seconds)} | ${"%.0f".format(count / seconds)} $unit |"

        private fun pct(p: Double): Long {
            if (sortedLatencies.isEmpty()) return 0
            val rank = ceil(p / 100.0 * sortedLatencies.size).toInt().coerceIn(1, sortedLatencies.size)
            return sortedLatencies[rank - 1]
        }

        companion object {
            fun ofLatency(sorted: List<Long>) = Stats(sorted.size, sorted, 0.0)
            fun ofThroughput(count: Int, seconds: Double) = Stats(count, emptyList(), seconds)
        }
    }
}
