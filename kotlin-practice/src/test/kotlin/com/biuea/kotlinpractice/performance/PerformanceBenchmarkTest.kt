package com.biuea.kotlinpractice.performance

import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

@TestMethodOrder(OrderAnnotation::class)
@Timeout(600)
class PerformanceBenchmarkTest {

    companion object {
        private const val TOTAL_REQUESTS = 10_000
        private const val WARMUP_REQUESTS = 500
        private val CONCURRENCY_LEVELS = listOf(500, 1000, 2000)

        private val results = mutableListOf<BenchmarkResult>()

        @JvmStatic
        @AfterAll
        fun printResults() {
            for (concurrency in CONCURRENCY_LEVELS) {
                val filtered = results.filter { it.concurrency == concurrency }
                if (filtered.isEmpty()) continue

                println()
                println("=".repeat(95))
                println(" Concurrency: $concurrency | Total: $TOTAL_REQUESTS | Delay: ${SIMULATED_DELAY_MS}ms")
                println("=".repeat(95))
                println(
                    String.format(
                        "%-50s %12s %12s %12s %8s",
                        "Environment", "Avg (ms)", "Min (ms)", "Max (ms)", "Errors"
                    )
                )
                println("-".repeat(95))
                filtered.forEach { r ->
                    println(
                        String.format(
                            "%-50s %12.2f %12d %12d %8d",
                            r.name, r.avgMs, r.minMs, r.maxMs, r.errors
                        )
                    )
                }
                println("=".repeat(95))
            }
        }
    }

    data class BenchmarkResult(
        val name: String,
        val concurrency: Int,
        val avgMs: Double,
        val minMs: Long,
        val maxMs: Long,
        val errors: Int
    )

    private data class RequestStats(
        val avgMs: Double,
        val minMs: Long,
        val maxMs: Long,
        val errors: Int
    )

    private fun startApp(
        appClass: Class<*>,
        webType: String,
        virtualThreads: Boolean
    ): ConfigurableApplicationContext {
        return SpringApplicationBuilder(appClass)
            .run(
                "--server.port=0",
                "--spring.main.web-application-type=$webType",
                "--spring.threads.virtual.enabled=$virtualThreads",
                "--spring.main.banner-mode=off",
                "--logging.level.root=WARN",
                "--logging.level.org.springframework=WARN",
                "--spring.config.location=optional:classpath:/nonexistent/"
            )
    }

    private fun getPort(app: ConfigurableApplicationContext): Int {
        return app.environment.getProperty("local.server.port")!!.toInt()
    }

    private fun sendRequests(url: String, totalRequests: Int, concurrency: Int): RequestStats {
        val client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(60))
            .build()

        val semaphore = Semaphore(concurrency)
        val responseTimes = ConcurrentLinkedQueue<Long>()
        val errors = AtomicInteger(0)
        val executor = Executors.newVirtualThreadPerTaskExecutor()

        val futures = (1..totalRequests).map {
            CompletableFuture.runAsync({
                semaphore.acquire()
                try {
                    val start = System.nanoTime()
                    val request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .timeout(Duration.ofSeconds(60))
                        .build()
                    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                    if (response.statusCode() == 200) {
                        val elapsedMs = (System.nanoTime() - start) / 1_000_000
                        responseTimes.add(elapsedMs)
                    } else {
                        errors.incrementAndGet()
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    semaphore.release()
                }
            }, executor)
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
        executor.close()

        val times = responseTimes.toList()
        return if (times.isEmpty()) {
            RequestStats(0.0, 0, 0, errors.get())
        } else {
            RequestStats(
                avgMs = times.average(),
                minMs = times.min(),
                maxMs = times.max(),
                errors = errors.get()
            )
        }
    }

    /**
     * Boots the app once, then runs benchmarks at all concurrency levels (500, 1000, 2000).
     */
    private fun runBenchmarkSuite(
        name: String,
        appClass: Class<*>,
        webType: String,
        virtualThreads: Boolean = false
    ) {
        println("\n>>> [$name] Starting app...")
        val app = startApp(appClass, webType, virtualThreads)
        val port = getPort(app)
        val url = "http://localhost:$port/performance/test"

        // Warmup
        println(">>> [$name] Warming up...")
        sendRequests(url, WARMUP_REQUESTS, 50)

        for (concurrency in CONCURRENCY_LEVELS) {
            println(">>> [$name] Running $TOTAL_REQUESTS requests (concurrency=$concurrency)...")
            val stats = sendRequests(url, TOTAL_REQUESTS, concurrency)
            results.add(BenchmarkResult(name, concurrency, stats.avgMs, stats.minMs, stats.maxMs, stats.errors))
            println(">>> [$name] c=$concurrency: avg=%.2fms, min=%dms, max=%dms, errors=%d".format(
                stats.avgMs, stats.minMs, stats.maxMs, stats.errors
            ))
            Assertions.assertEquals(0, stats.errors, "[$name] concurrency=$concurrency had ${stats.errors} errors")
        }

        app.close()
        println(">>> [$name] Done.")
    }

    // ============================================================
    // Test Scenarios
    // ============================================================

    @Test
    @Order(1)
    fun `1 - Tomcat MVC`() {
        runBenchmarkSuite("Tomcat MVC", TomcatMvcBlockingApp::class.java, "servlet", false)
    }

    @Test
    @Order(2)
    fun `2 - Netty WebFlux`() {
        runBenchmarkSuite("Netty WebFlux", NettyWebFluxReactiveApp::class.java, "reactive", false)
    }

    @Test
    @Order(3)
    fun `3 - Tomcat MVC + Coroutine`() {
        runBenchmarkSuite("Tomcat MVC + Coroutine", TomcatMvcCoroutineApp::class.java, "servlet", false)
    }

    @Test
    @Order(4)
    fun `4 - Netty WebFlux + Coroutine`() {
        runBenchmarkSuite("Netty WebFlux + Coroutine", NettyWebFluxCoroutineApp::class.java, "reactive", false)
    }

    @Test
    @Order(5)
    fun `5 - Tomcat MVC + Virtual Thread`() {
        runBenchmarkSuite("Tomcat MVC + Virtual Thread", TomcatMvcBlockingApp::class.java, "servlet", true)
    }

    @Test
    @Order(6)
    fun `6 - Netty WebFlux + Virtual Thread`() {
        runBenchmarkSuite("Netty WebFlux + Virtual Thread", NettyWebFluxReactiveApp::class.java, "reactive", true)
    }

    @Test
    @Order(7)
    fun `7 - Tomcat MVC + Coroutine + Virtual Thread`() {
        runBenchmarkSuite("Tomcat MVC + Coroutine + Virtual Thread", TomcatMvcCoroutineApp::class.java, "servlet", true)
    }

    @Test
    @Order(8)
    fun `8 - Netty WebFlux + Coroutine + Virtual Thread`() {
        runBenchmarkSuite("Netty WebFlux + Coroutine + Virtual Thread", NettyWebFluxCoroutineApp::class.java, "reactive", true)
    }
}
