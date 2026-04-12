package com.biuea.httpclient.benchmark

import com.biuea.httpclient.client.ReactiveHttpInterfaceApi
import com.biuea.httpclient.support.*
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.core.publisher.Mono
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Reactor (Mono/Flux) 벤치마크.
 *
 * Reactor의 Flux.flatMap()을 사용하여
 * 리액티브 HTTP 클라이언트의 동시성 성능을 비교한다.
 *
 * Reactor 특징:
 * - 논블로킹 I/O (Netty 이벤트 루프)
 * - 적은 수의 스레드로 높은 동시성 달성
 * - 백프레셔(backpressure) 지원
 * - flatMap의 concurrency 파라미터로 동시성 수준 제어
 *
 * 테스트 대상:
 * 1. WebClient (Reactor Netty)
 * 2. HTTP Interface (WebClient/Reactive 백엔드)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ReactorBenchmarkTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var webClient: WebClient

    private val results = CopyOnWriteArrayList<BenchmarkResult>()

    private fun baseUrl() = "http://localhost:$port"
    private fun delayUrl() = "${baseUrl()}/api/mock/delay?delayMs=$DELAY_MS"

    @AfterAll
    fun tearDown() {
        println("\n[Reactor 벤치마크 종합 결과]")
        BenchmarkReporter.print(results)
    }

    // ================================================================
    // 1. WebClient (Reactor Netty)
    //
    // WebClient는 Reactor Netty의 이벤트 루프 위에서 동작한다.
    // Flux.flatMap(concurrency)로 동시 요청 수를 제어한다.
    // 스레드를 블로킹하지 않으므로 소수의 스레드로 수천 건 동시 요청 가능.
    // ================================================================
    @Test
    @Order(1)
    fun `WebClient + Reactor`() {
        // 테스트용 WebClient (baseUrl 포함)
        val testWebClient = WebClient.builder()
            .clientConnector(
                org.springframework.http.client.reactive.ReactorClientHttpConnector(
                    reactor.netty.http.client.HttpClient.create()
                        .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                        .responseTimeout(java.time.Duration.ofMillis(5000))
                )
            )
            .baseUrl(baseUrl())
            .build()

        val result = benchmarkReactive(
            name = "WebClient + Reactor",
            concurrency = REACTOR_CONCURRENCY
        ) {
            testWebClient.get()
                .uri("/api/mock/delay?delayMs=$DELAY_MS")
                .retrieve()
                .bodyToMono(String::class.java)
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 2. WebClient + Reactor (높은 동시성)
    //
    // 동시성을 TOTAL_REQUESTS까지 올려 모든 요청을 동시에 실행한다.
    // 커넥션 풀 크기가 동시성보다 작으면 pendingAcquireTimeout이 발생할 수 있다.
    // ================================================================
    @Test
    @Order(2)
    fun `WebClient + Reactor (high concurrency)`() {
        val testWebClient = WebClient.builder()
            .clientConnector(
                org.springframework.http.client.reactive.ReactorClientHttpConnector(
                    reactor.netty.http.client.HttpClient.create(
                        reactor.netty.resources.ConnectionProvider.builder("high-concurrency")
                            .maxConnections(500)
                            .pendingAcquireTimeout(java.time.Duration.ofSeconds(10))
                            .build()
                    )
                        .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                        .responseTimeout(java.time.Duration.ofMillis(5000))
                )
            )
            .baseUrl(baseUrl())
            .build()

        val result = benchmarkReactive(
            name = "WebClient + Reactor(High)",
            concurrency = TOTAL_REQUESTS  // 모든 요청을 동시에
        ) {
            testWebClient.get()
                .uri("/api/mock/delay?delayMs=$DELAY_MS")
                .retrieve()
                .bodyToMono(String::class.java)
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 3. HTTP Interface (Reactive / WebClient 백엔드)
    //
    // @GetExchange 어노테이션으로 선언한 인터페이스가
    // WebClient 백엔드를 통해 리액티브로 동작한다.
    // Mono<String>을 반환하므로 flatMap으로 동시 실행 가능.
    // ================================================================
    @Test
    @Order(3)
    fun `HTTP Interface (Reactive) + Reactor`() {
        val testWebClient = WebClient.builder()
            .clientConnector(
                org.springframework.http.client.reactive.ReactorClientHttpConnector(
                    reactor.netty.http.client.HttpClient.create()
                        .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                        .responseTimeout(java.time.Duration.ofMillis(5000))
                )
            )
            .baseUrl(baseUrl())
            .build()

        val factory = HttpServiceProxyFactory.builderFor(
            WebClientAdapter.create(testWebClient)
        ).build()
        val api = factory.createClient(ReactiveHttpInterfaceApi::class.java)

        val result = benchmarkReactive(
            name = "HTTPInterface(Reactive) + Reactor",
            concurrency = REACTOR_CONCURRENCY
        ) {
            api.getWithDelay(DELAY_MS)
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }
}
