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
 * Platform Thread (고정 크기 스레드 풀) 벤치마크.
 *
 * Executors.newFixedThreadPool(50)을 사용하여
 * 블로킹 HTTP 클라이언트 7종의 동시성 성능을 비교한다.
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
class PlatformThreadBenchmarkTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var restClient: RestClient

    @Autowired
    private lateinit var blockingHttpInterfaceApi: BlockingHttpInterfaceApi

    @Autowired
    private lateinit var okHttpClient: OkHttpClient

    @Autowired
    private lateinit var apacheHttpClient: CloseableHttpClient

    @Autowired
    private lateinit var retrofitMockApi: RetrofitMockApi

    private lateinit var feignMockApi: FeignMockApi
    private lateinit var executor: ExecutorService
    private val results = CopyOnWriteArrayList<BenchmarkResult>()

    private fun baseUrl() = "http://localhost:$port"
    private fun delayUrl() = "${baseUrl()}/api/mock/delay?delayMs=$DELAY_MS"

    @BeforeAll
    fun setup() {
        // Feign은 런타임 baseUrl이 필요하므로 여기서 생성
        // Feign의 기본 디코더(Decoder.Default)는 String 반환 타입을 처리한다.
        // JacksonDecoder는 JSON → DTO 매핑 시 사용하며, String 반환 시 역직렬화 오류 발생.
        feignMockApi = feign.Feign.builder()
            .encoder(feign.jackson.JacksonEncoder())
            .options(feign.Request.Options(3000, java.util.concurrent.TimeUnit.MILLISECONDS, 5000, java.util.concurrent.TimeUnit.MILLISECONDS, true))
            .target(FeignMockApi::class.java, baseUrl())

        executor = Executors.newFixedThreadPool(PLATFORM_THREAD_POOL_SIZE)
    }

    @AfterAll
    fun tearDown() {
        executor.shutdown()
        println("\n[Platform Thread 벤치마크 종합 결과]")
        BenchmarkReporter.print(results)
    }

    // ================================================================
    // 1. RestTemplate
    // ================================================================
    @Test
    @Order(1)
    fun `RestTemplate + Platform Thread`() {
        val result = benchmarkBlocking(
            name = "RestTemplate + PlatformThread",
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
    fun `RestClient + Platform Thread`() {
        val result = benchmarkBlocking(
            name = "RestClient + PlatformThread",
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
    fun `HTTP Interface (Blocking) + Platform Thread`() {
        // HTTP Interface는 baseUrl이 빌드 타임에 설정되므로, 여기서는 직접 RestClient로 재빌드
        // 기본 RestClient는 SimpleClientHttpRequestFactory 사용 → 커넥션 풀 없음 → 동시성 병목
        // JDK HttpClient 백엔드를 사용하여 공정한 비교 수행
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
            name = "HTTPInterface(Blocking) + PT",
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
    fun `OpenFeign + Platform Thread`() {
        val result = benchmarkBlocking(
            name = "OpenFeign + PlatformThread",
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
    fun `Retrofit + Platform Thread`() {
        // Retrofit은 런타임 baseUrl이 필요하므로 여기서 재생성
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
            name = "Retrofit + PlatformThread",
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
    fun `OkHttp + Platform Thread`() {
        val result = benchmarkBlocking(
            name = "OkHttp + PlatformThread",
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
    fun `Apache HttpClient 5 + Platform Thread`() {
        val result = benchmarkBlocking(
            name = "ApacheHC5 + PlatformThread",
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
