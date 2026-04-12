package com.biuea.httpclient.benchmark

import com.biuea.httpclient.client.BlockingHttpInterfaceApi
import com.biuea.httpclient.client.FeignMockApi
import com.biuea.httpclient.client.RetrofitMockApi
import com.biuea.httpclient.support.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.body
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Virtual Thread (JDK 21+) 벤치마크.
 *
 * Executors.newVirtualThreadPerTaskExecutor()를 사용하여
 * 블로킹 HTTP 클라이언트 7종의 동시성 성능을 비교한다.
 *
 * Virtual Thread 특징:
 * - JDK 21에서 정식 출시 (Project Loom)
 * - 가벼운 스레드 (수백만 개 생성 가능)
 * - 블로킹 I/O 시 자동으로 캐리어 스레드 해제 → 높은 동시성
 * - 기존 블로킹 코드를 수정 없이 높은 동시성으로 실행 가능
 *
 * 테스트 대상:
 * 1. RestTemplate (Apache HttpClient 5 백엔드)
 * 2. RestClient (JDK HttpClient 백엔드)
 * 3. HTTP Interface (RestClient 백엔드)
 * 4. OpenFeign
 * 5. Retrofit (OkHttp 백엔드)
 * 6. OkHttp
 * 7. Apache HttpClient 5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class VirtualThreadBenchmarkTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var restClient: RestClient

    @Autowired
    private lateinit var okHttpClient: OkHttpClient

    @Autowired
    private lateinit var apacheHttpClient: CloseableHttpClient

    private lateinit var feignMockApi: FeignMockApi
    private lateinit var executor: ExecutorService
    private val results = CopyOnWriteArrayList<BenchmarkResult>()

    private fun baseUrl() = "http://localhost:$port"
    private fun delayUrl() = "${baseUrl()}/api/mock/delay?delayMs=$DELAY_MS"

    @BeforeAll
    fun setup() {
        // Virtual Thread Executor (JDK 21+)
        // 요청마다 새로운 Virtual Thread를 생성한다.
        // Platform Thread와 달리 풀 크기 제한이 없다.
        executor = Executors.newVirtualThreadPerTaskExecutor()

        feignMockApi = feign.Feign.builder()
            .encoder(feign.jackson.JacksonEncoder())
            .options(feign.Request.Options(3000, java.util.concurrent.TimeUnit.MILLISECONDS, 5000, java.util.concurrent.TimeUnit.MILLISECONDS, true))
            .target(FeignMockApi::class.java, baseUrl())
    }

    @AfterAll
    fun tearDown() {
        executor.shutdown()
        println("\n[Virtual Thread 벤치마크 종합 결과]")
        BenchmarkReporter.print(results)
    }

    // ================================================================
    // 1. RestTemplate
    // ================================================================
    @Test
    @Order(1)
    fun `RestTemplate + Virtual Thread`() {
        val result = benchmarkBlocking(
            name = "RestTemplate + VirtualThread",
            executor = executor
        ) {
            restTemplate.getForObject(delayUrl(), String::class.java)
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 2. RestClient
    // ================================================================
    @Test
    @Order(2)
    fun `RestClient + Virtual Thread`() {
        val result = benchmarkBlocking(
            name = "RestClient + VirtualThread",
            executor = executor
        ) {
            restClient.get()
                .uri(delayUrl())
                .retrieve()
                .body<String>()
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 3. HTTP Interface (Blocking / RestClient 백엔드)
    // ================================================================
    @Test
    @Order(3)
    fun `HTTP Interface (Blocking) + Virtual Thread`() {
        val jdkHttpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofMillis(3000))
            .build()
        val testRestClient = RestClient.builder()
            .baseUrl(baseUrl())
            .requestFactory(org.springframework.http.client.JdkClientHttpRequestFactory(jdkHttpClient).apply {
                setReadTimeout(java.time.Duration.ofMillis(5000))
            })
            .build()
        val factory = org.springframework.web.service.invoker.HttpServiceProxyFactory.builderFor(
            org.springframework.web.client.support.RestClientAdapter.create(testRestClient)
        ).build()
        val api = factory.createClient(BlockingHttpInterfaceApi::class.java)

        val result = benchmarkBlocking(
            name = "HTTPInterface(Blocking) + VT",
            executor = executor
        ) {
            api.getWithDelay(DELAY_MS)
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 4. OpenFeign
    // ================================================================
    @Test
    @Order(4)
    fun `OpenFeign + Virtual Thread`() {
        val result = benchmarkBlocking(
            name = "OpenFeign + VirtualThread",
            executor = executor
        ) {
            feignMockApi.getWithDelay(DELAY_MS)
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 5. Retrofit
    // ================================================================
    @Test
    @Order(5)
    fun `Retrofit + Virtual Thread`() {
        val okhttp = OkHttpClient.Builder()
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("${baseUrl()}/")
            .client(okhttp)
            .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
            .build()
        val api = retrofit.create(RetrofitMockApi::class.java)

        val result = benchmarkBlocking(
            name = "Retrofit + VirtualThread",
            executor = executor
        ) {
            api.getWithDelay(DELAY_MS).execute().body()
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 6. OkHttp
    // ================================================================
    @Test
    @Order(6)
    fun `OkHttp + Virtual Thread`() {
        val result = benchmarkBlocking(
            name = "OkHttp + VirtualThread",
            executor = executor
        ) {
            val request = Request.Builder()
                .url(delayUrl())
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                response.body?.string()
            }
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }

    // ================================================================
    // 7. Apache HttpClient 5
    // ================================================================
    @Test
    @Order(7)
    fun `Apache HttpClient 5 + Virtual Thread`() {
        val result = benchmarkBlocking(
            name = "ApacheHC5 + VirtualThread",
            executor = executor
        ) {
            val httpGet = HttpGet(delayUrl())
            apacheHttpClient.execute(httpGet) { response ->
                EntityUtils.toString(response.entity)
            }
        }
        results.add(result)
        println("✓ ${result.name}: avg=${result.avgLatencyMs}ms, p99=${result.p99Ms}ms, throughput=${result.throughput} req/s")
    }
}
