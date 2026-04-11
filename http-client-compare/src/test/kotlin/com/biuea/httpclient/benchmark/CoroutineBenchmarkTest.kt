package com.biuea.httpclient.benchmark

import com.biuea.httpclient.support.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Kotlin Coroutine 벤치마크.
 *
 * Dispatchers.IO + structured concurrency를 사용하여
 * 코루틴 네이티브 HTTP 클라이언트의 동시성 성능을 비교한다.
 *
 * Kotlin Coroutine 특징:
 * - suspend 함수로 자연스러운 비동기 프로그래밍
 * - Structured Concurrency (부모-자식 코루틴 관계)
 * - Dispatchers.IO: 블로킹 I/O 전용 디스패처 (스레드 풀 자동 확장)
 * - Dispatchers.Default: CPU 집약적 작업 (코어 수만큼 스레드)
 * - async/await로 동시성 제어
 *
 * 테스트 대상:
 * 1. Ktor HttpClient (CIO 엔진, 코루틴 네이티브)
 * 2. WebClient + awaitSingle (리액터→코루틴 브릿지)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CoroutineBenchmarkTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var ktorHttpClient: io.ktor.client.HttpClient

    @Autowired
    private lateinit var webClient: WebClient

    private val results = CopyOnWriteArrayList<BenchmarkResult>()

    private fun baseUrl() = "http://localhost:$port"
    private fun delayUrl() = "${baseUrl()}/api/mock/delay?delayMs=$DELAY_MS"

    @AfterAll
    fun tearDown() {
        println("\n[Coroutine 벤치마크 종합 결과]")
        BenchmarkReporter.print(results)
    }

    // ================================================================
    // 1. Ktor HttpClient (CIO 엔진)
    //
    // Ktor Client는 Kotlin 코루틴 네이티브 HTTP 클라이언트이다.
    // 모든 HTTP 호출이 suspend 함수이므로 코루틴과 가장 자연스럽게 통합된다.
    // CIO(Coroutine I/O) 엔진은 Ktor 자체 비동기 I/O를 사용한다.
    //
    // 장점:
    // - suspend 함수로 자연스러운 비동기 호출
    // - Kotlin Multiplatform (KMP) 지원
    // - 경량 (별도 리액티브 프레임워크 불필요)
    //
    // 주의:
    // - CIO 엔진은 HTTP/1.1만 지원 (HTTP/2는 OkHttp/Java 엔진 필요)
    // - Reactor/WebFlux 생태계와 직접 통합되지 않음
    // ================================================================
    @Test
    @Order(1)
    fun `Ktor HttpClient + Coroutine (Dispatchers IO)`() {
        val result = benchmarkCoroutine(
            name = "Ktor(CIO) + Coroutine(IO)",
            dispatcher = Dispatchers.IO
        ) {
            ktorHttpClient.get(delayUrl()).bodyAsText()
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 2. Ktor HttpClient + Default Dispatcher
    //
    // Dispatchers.Default는 CPU 바운드 작업용이지만,
    // Ktor의 CIO 엔진이 내부적으로 논블로킹이므로
    // Default 디스패처에서도 높은 동시성을 달성할 수 있다.
    // ================================================================
    @Test
    @Order(2)
    fun `Ktor HttpClient + Coroutine (Dispatchers Default)`() {
        val result = benchmarkCoroutine(
            name = "Ktor(CIO) + Coroutine(Default)",
            dispatcher = Dispatchers.Default
        ) {
            ktorHttpClient.get(delayUrl()).bodyAsText()
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 3. WebClient + awaitSingle (코루틴 브릿지)
    //
    // Spring WebClient의 Mono를 kotlinx-coroutines-reactor의
    // awaitSingle()로 변환하여 코루틴에서 사용한다.
    //
    // 이 방식은 Reactor의 논블로킹 I/O를 유지하면서
    // 코루틴의 구조적 동시성을 활용할 수 있다.
    //
    // 사용 시나리오:
    // - Spring WebFlux 프로젝트에서 Kotlin 코루틴 전환 시
    // - 기존 WebClient를 코루틴 서비스에서 재사용할 때
    // ================================================================
    @Test
    @Order(3)
    fun `WebClient + awaitSingle (Coroutine Bridge)`() {
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

        val result = benchmarkCoroutine(
            name = "WebClient + awaitSingle",
            dispatcher = Dispatchers.IO
        ) {
            testWebClient.get()
                .uri("/api/mock/delay?delayMs=$DELAY_MS")
                .retrieve()
                .bodyToMono(String::class.java)
                .awaitSingle()
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }
}
