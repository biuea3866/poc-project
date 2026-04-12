package com.biuea.httpclient.config

import com.biuea.httpclient.client.BlockingHttpInterfaceApi
import com.biuea.httpclient.client.FeignMockApi
import com.biuea.httpclient.client.ReactiveHttpInterfaceApi
import com.biuea.httpclient.client.RetrofitMockApi
import com.fasterxml.jackson.databind.ObjectMapper
import feign.Feign
import feign.Logger
import feign.Request
import feign.Retryer
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.http.HttpClient as JdkHttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * HTTP 클라이언트 종합 비교 설정.
 *
 * 9개 HTTP 클라이언트의 모든 설정 옵션을 상세하게 구성하고 비교한다.
 * 각 클라이언트별 지원하는 설정 항목, 기본값, 권장값을 한국어 주석으로 설명한다.
 */
@Configuration
class HttpClientConfig {

    @Value("\${server.port:0}")
    private var serverPort: Int = 0

    // ================================================================
    // 1. RestTemplate
    //    - Spring Framework 3.0+ (2009~)
    //    - 동기(Blocking) 클라이언트
    //    - maintenance 모드 (Spring 5.0+부터 RestClient 권장)
    //    - 기본 백엔드: SimpleClientHttpRequestFactory (java.net.HttpURLConnection)
    //    - 권장 백엔드: HttpComponentsClientHttpRequestFactory (Apache HttpClient 5)
    //
    //    설정 가능 항목:
    //    ✅ connectTimeout         — TCP 연결 타임아웃
    //    ✅ readTimeout (socket)   — 소켓 읽기 타임아웃
    //    ✅ connectionRequestTimeout — 풀에서 커넥션 대기 타임아웃
    //    ✅ responseTimeout        — 전체 응답 타임아웃
    //    ✅ 커넥션 풀              — Apache 백엔드 사용 시 (maxConnTotal, maxConnPerRoute)
    //    ✅ 유휴/만료 커넥션 퇴거   — evictExpiredConnections, evictIdleConnections
    //    ✅ Keep-Alive / TTL       — ConnectionConfig.timeToLive
    //    ✅ Interceptor            — ClientHttpRequestInterceptor
    //    ✅ ErrorHandler           — ResponseErrorHandler
    //    ✅ MessageConverter       — HttpMessageConverter 리스트
    //    ✅ 재시도                  — Apache 백엔드의 HttpRequestRetryStrategy
    //    ✅ 리다이렉트             — disableRedirectHandling 또는 RedirectStrategy
    //    ✅ SSL/TLS               — Apache 백엔드의 SSLContext
    //    ✅ 압축(gzip)            — Apache 백엔드의 Content-Encoding 지원
    //    ✅ 프록시                 — Apache 백엔드의 routePlanner 또는 setProxy
    //    ❌ writeTimeout           — 지원하지 않음 (Apache 백엔드도 미지원)
    //    ❌ HTTP/2                — Apache HttpClient 5 classic은 HTTP/1.1만 지원
    //    ❌ 자체 비동기 지원        — AsyncRestTemplate은 deprecated
    //    ❌ DNS 커스텀             — Apache 백엔드의 DnsResolver로 간접 설정
    // ================================================================
    @Bean
    fun restTemplate(): RestTemplate {
        // Apache HttpClient 5 커넥션 매니저 구성
        val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            // ── 커넥션 풀 크기 ──
            .setMaxConnTotal(200)                  // 전체 최대 커넥션 수 (모든 호스트 합계, 기본값: 25)
            .setMaxConnPerRoute(100)               // 호스트(라우트)당 최대 커넥션 수 (기본값: 5)
            // ── 커넥션 레벨 설정 ──
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(3000))        // TCP 연결 타임아웃 (3초)
                    .setSocketTimeout(Timeout.ofMilliseconds(5000))         // 소켓 읽기 타임아웃 (5초)
                    .setTimeToLive(TimeValue.ofMinutes(5))                  // 커넥션 최대 수명 (5분, 이후 폐기)
                    .setValidateAfterInactivity(TimeValue.ofSeconds(10))    // 유휴 후 재사용 전 유효성 검증 (10초)
                    .build()
            )
            .build()

        // Apache HttpClient 5 빌더
        val httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            // ── 요청 레벨 설정 ──
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(1000))  // 풀에서 커넥션 획득 대기 타임아웃 (1초)
                    .setResponseTimeout(Timeout.ofMilliseconds(5000))           // 전체 응답 타임아웃 (5초)
                    .setRedirectsEnabled(true)                                  // 리다이렉트 허용 (기본값: true)
                    .setMaxRedirects(50)                                        // 최대 리다이렉트 횟수 (기본값: 50)
                    .setContentCompressionEnabled(true)                         // gzip/deflate 압축 지원 (기본값: true)
                    .build()
            )
            // ── 커넥션 퇴거 정책 ──
            .evictExpiredConnections()                              // 만료된 커넥션 자동 퇴거 (백그라운드 스레드)
            .evictIdleConnections(TimeValue.ofSeconds(30))         // 30초 이상 유휴 커넥션 퇴거
            // ── 재시도 정책 ──
            // .setRetryStrategy(DefaultHttpRequestRetryStrategy(3, TimeValue.ofSeconds(1)))
            //   → 최대 3회 재시도, 1초 간격 (멱등 요청만, 기본값: 1회)
            // ── SSL/TLS ──
            // .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
            //     .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
            //         .setSslContext(SSLContexts.custom()
            //             .loadTrustMaterial(trustStore, TrustSelfSignedStrategy())
            //             .build())
            //         .setTlsVersions(TLS.V_1_3)
            //         .build())
            //     .build())
            // ── 프록시 ──
            // .setProxy(HttpHost("proxy.example.com", 8080))
            .build()

        return RestTemplate(HttpComponentsClientHttpRequestFactory(httpClient))
    }

    // ================================================================
    // 2. RestClient
    //    - Spring Framework 6.1+ (2023~)
    //    - 동기(Blocking), Fluent API
    //    - RestTemplate의 현대적 대체품
    //    - 기본 백엔드: JdkClientHttpRequestFactory (java.net.http.HttpClient)
    //    - 대안 백엔드: HttpComponentsClientHttpRequestFactory, Reactor Netty
    //
    //    설정 가능 항목:
    //    ✅ connectTimeout         — JDK HttpClient의 connectTimeout
    //    ✅ readTimeout            — JdkClientHttpRequestFactory의 readTimeout
    //    ✅ 커넥션 풀              — JDK HttpClient 내부 풀 (자동 관리, 세밀한 제어 불가)
    //    ✅ HTTP/2                — JDK HttpClient가 기본 지원 (Version.HTTP_2)
    //    ✅ Interceptor            — ClientHttpRequestInterceptor (exchange filter)
    //    ✅ ErrorHandler           — defaultStatusHandler
    //    ✅ MessageConverter       — messageConverters
    //    ✅ 리다이렉트             — JDK HttpClient의 followRedirects
    //    ✅ SSL/TLS               — JDK HttpClient의 sslContext
    //    ✅ 프록시                 — JDK HttpClient의 proxy (ProxySelector)
    //    ✅ Executor               — JDK HttpClient의 executor (Virtual Thread 가능)
    //    ❌ writeTimeout           — JDK HttpClient에서 직접 지원하지 않음
    //    ❌ 커넥션 풀 세밀 제어     — maxConnTotal, maxConnPerRoute 등 불가
    //    ❌ 유휴/만료 퇴거          — JDK HttpClient가 자동 관리
    //    ❌ Keep-Alive 세밀 제어   — JDK HttpClient가 자동 관리
    //    ❌ DNS 커스텀             — 지원하지 않음
    //    ❌ 재시도                  — 내장 재시도 없음 (별도 구현 필요)
    //    ❌ 압축(gzip)            — 수동으로 Accept-Encoding 헤더 설정 필요
    //
    //    참고: Apache 백엔드를 사용하면 RestTemplate과 동일한 세밀한 설정 가능
    // ================================================================
    @Bean
    fun restClient(): RestClient {
        // JDK HttpClient 구성 (Java 11+, HTTP/2 지원)
        val jdkHttpClient = JdkHttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(3000))                   // TCP 연결 타임아웃 (3초)
            .followRedirects(JdkHttpClient.Redirect.NORMAL)            // 리다이렉트 정책 (NORMAL: 3xx → 동일 프로토콜만)
            .version(JdkHttpClient.Version.HTTP_2)                     // HTTP/2 사용 (서버가 지원하지 않으면 HTTP/1.1 폴백)
            // ── Virtual Thread Executor (JDK 21+) ──
            // .executor(Executors.newVirtualThreadPerTaskExecutor())   // Virtual Thread로 내부 I/O 처리
            // ── SSL/TLS ──
            // .sslContext(SSLContext.getDefault())                     // 커스텀 SSL Context
            // .sslParameters(SSLParameters().apply {
            //     protocols = arrayOf("TLSv1.3")                      // TLS 1.3 강제
            // })
            // ── 프록시 ──
            // .proxy(ProxySelector.of(InetSocketAddress("proxy.example.com", 8080)))
            .build()

        val requestFactory = JdkClientHttpRequestFactory(jdkHttpClient)
        requestFactory.setReadTimeout(Duration.ofMillis(5000))         // 읽기 타임아웃 (5초)

        return RestClient.builder()
            .requestFactory(requestFactory)
            // ── 기본 헤더 ──
            .defaultHeader("Accept", "application/json")
            // ── 에러 핸들러 ──
            .defaultStatusHandler({ status -> status.is4xxClientError }) { _, response ->
                throw RuntimeException("클라이언트 에러: ${response.statusCode}")
            }
            .defaultStatusHandler({ status -> status.is5xxServerError }) { _, response ->
                throw RuntimeException("서버 에러: ${response.statusCode}")
            }
            .build()
    }

    // ================================================================
    // 3. WebClient
    //    - Spring WebFlux (Spring 5.0+, 2017~)
    //    - 비동기/논블로킹 (Reactive), Mono/Flux 반환
    //    - 기본 백엔드: Reactor Netty (io.projectreactor.netty:reactor-netty)
    //    - 대안 백엔드: Jetty Reactive, Apache HC5 Async
    //
    //    설정 가능 항목:
    //    ✅ connectTimeout         — HttpClient.option(CONNECT_TIMEOUT_MILLIS)
    //    ✅ responseTimeout        — HttpClient.responseTimeout (전체 응답 대기)
    //    ✅ readTimeout            — .doOnConnected() + ReadTimeoutHandler (Netty)
    //    ✅ writeTimeout           — .doOnConnected() + WriteTimeoutHandler (Netty)
    //    ✅ 커넥션 풀              — ConnectionProvider (maxConnections, pendingAcquireMaxCount)
    //    ✅ 유휴 타임아웃          — ConnectionProvider.maxIdleTime
    //    ✅ 커넥션 최대 수명       — ConnectionProvider.maxLifeTime
    //    ✅ 풀 대기 타임아웃       — ConnectionProvider.pendingAcquireTimeout
    //    ✅ 퇴거 정책              — ConnectionProvider.evictInBackground (백그라운드 퇴거)
    //    ✅ Keep-Alive             — Netty 채널 옵션 (SO_KEEPALIVE)
    //    ✅ HTTP/2                — HttpClient.protocol(HttpProtocol.H2, H2C)
    //    ✅ SSL/TLS               — HttpClient.secure() + SslContext
    //    ✅ 압축(gzip)            — HttpClient.compress(true)
    //    ✅ 프록시                 — HttpClient.proxy(PROXY_TYPE, host, port)
    //    ✅ DNS                   — HttpClient.resolver(AddressResolverGroup)
    //    ✅ Filter                — ExchangeFilterFunction (요청/응답 인터셉터)
    //    ✅ 리다이렉트             — HttpClient.followRedirect(true)
    //    ❌ 자체 재시도             — reactor-extra의 Retry 유틸 사용 (.retryWhen)
    //    ❌ Per-Route 풀 설정      — Reactor Netty는 전체 풀만 설정 가능
    // ================================================================
    @Bean
    fun webClient(): WebClient {
        // Reactor Netty ConnectionProvider (커넥션 풀) 구성
        val connectionProvider = ConnectionProvider.builder("custom-pool")
            .maxConnections(200)                                       // 전체 최대 커넥션 수 (기본값: 500)
            .maxIdleTime(Duration.ofSeconds(30))                       // 유휴 커넥션 최대 유지 시간 (30초)
            .maxLifeTime(Duration.ofMinutes(5))                        // 커넥션 최대 수명 (5분)
            .pendingAcquireTimeout(Duration.ofMillis(1000))            // 풀에서 커넥션 획득 대기 타임아웃 (1초)
            .pendingAcquireMaxCount(500)                               // 풀 대기 큐 최대 크기
            .evictInBackground(Duration.ofSeconds(30))                 // 백그라운드 유휴/만료 커넥션 퇴거 주기 (30초)
            // .metrics(true)                                          // Micrometer 메트릭 활성화 (io.micrometer:micrometer-core 의존성 필요)
            .build()

        // Reactor Netty HttpClient 구성
        val httpClient = HttpClient.create(connectionProvider)
            // ── 타임아웃 ──
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)  // TCP 연결 타임아웃 (3초)
            .responseTimeout(Duration.ofMillis(5000))                              // 전체 응답 타임아웃 (5초)
            .doOnConnected { conn ->
                conn.addHandlerLast(io.netty.handler.timeout.ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))   // 읽기 타임아웃 (5초)
                conn.addHandlerLast(io.netty.handler.timeout.WriteTimeoutHandler(3000, TimeUnit.MILLISECONDS))  // 쓰기 타임아웃 (3초)
            }
            // ── Keep-Alive ──
            .option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true)
            // ── 압축(gzip) ──
            .compress(true)
            // ── 리다이렉트 ──
            .followRedirect(true)
            // ── HTTP/2 ──
            // .protocol(HttpProtocol.H2)                               // HTTP/2 사용 (ALPN 필요)
            // ── SSL/TLS ──
            // .secure { spec ->
            //     spec.sslContext(
            //         SslContextBuilder.forClient()
            //             .trustManager(InsecureTrustManagerFactory.INSTANCE)  // 개발용 (프로덕션 금지)
            //             .protocols("TLSv1.3")
            //             .build()
            //     )
            // }
            // ── 프록시 ──
            // .proxy { spec ->
            //     spec.type(ProxyProvider.Proxy.HTTP)
            //         .host("proxy.example.com")
            //         .port(8080)
            // }
            // ── DNS 리졸버 ──
            // .resolver(DefaultAddressResolverGroup.INSTANCE)

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            // ── 기본 헤더 ──
            .defaultHeader("Accept", "application/json")
            // ── 요청/응답 필터 (Interceptor 역할) ──
            // .filter(ExchangeFilterFunction.ofRequestProcessor { request ->
            //     println("[WebClient] ${request.method()} ${request.url()}")
            //     Mono.just(request)
            // })
            // ── 코덱 설정 (최대 메모리 버퍼) ──
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10MB (기본값: 256KB)
            }
            .build()
    }

    // ================================================================
    // 4. HTTP Interface (Declarative)
    //    - Spring Framework 6.0+ (2022~)
    //    - 선언적 HTTP 클라이언트 (@HttpExchange 어노테이션)
    //    - 인터페이스만 정의하면 Spring이 프록시 구현체 생성
    //    - 백엔드: RestClient (blocking) 또는 WebClient (reactive)
    //
    //    설정 가능 항목:
    //    → 백엔드 클라이언트(RestClient/WebClient)의 설정을 그대로 상속
    //    ✅ 어노테이션 기반 URL 매핑   — @GetExchange, @PostExchange 등
    //    ✅ 파라미터 바인딩            — @RequestParam, @PathVariable, @RequestBody
    //    ✅ 커스텀 Converter           — HttpServiceProxyFactory에 conversionService 설정
    //    ✅ 에러 핸들링               — 백엔드 클라이언트의 에러 핸들러 상속
    //
    //    참고: HTTP Interface 자체는 추가적인 타임아웃/풀 설정이 없다.
    //          모든 네트워크 설정은 하위 백엔드 클라이언트가 담당한다.
    // ================================================================

    /**
     * Blocking HTTP Interface (RestClient 백엔드)
     *
     * RestClient의 모든 설정(타임아웃, HTTP/2, JDK 백엔드)을 상속받는다.
     * 반환 타입이 일반 객체(String, DTO)이므로 동기 호출.
     */
    @Bean
    fun blockingHttpInterfaceApi(restClient: RestClient): BlockingHttpInterfaceApi {
        val factory = HttpServiceProxyFactory.builderFor(
            org.springframework.web.client.support.RestClientAdapter.create(restClient)
        ).build()
        return factory.createClient(BlockingHttpInterfaceApi::class.java)
    }

    /**
     * Reactive HTTP Interface (WebClient 백엔드)
     *
     * WebClient의 모든 설정(커넥션 풀, Netty 타임아웃, 압축 등)을 상속받는다.
     * 반환 타입이 Mono/Flux이므로 비동기 논블로킹 호출.
     */
    @Bean
    fun reactiveHttpInterfaceApi(webClient: WebClient): ReactiveHttpInterfaceApi {
        val factory = HttpServiceProxyFactory.builderFor(
            WebClientAdapter.create(webClient)
        ).build()
        return factory.createClient(ReactiveHttpInterfaceApi::class.java)
    }

    // ================================================================
    // 5. OpenFeign
    //    - Netflix OSS → OpenFeign (2016~)
    //    - 선언적 HTTP 클라이언트 (@RequestLine, @Param)
    //    - 기본 백엔드: java.net.HttpURLConnection
    //    - 대안 백엔드: OkHttp, Apache HttpClient
    //
    //    설정 가능 항목:
    //    ✅ connectTimeout         — Request.Options (밀리초)
    //    ✅ readTimeout            — Request.Options (밀리초)
    //    ✅ followRedirects        — Request.Options
    //    ✅ 재시도                  — Retryer (maxAttempts, period, maxPeriod)
    //    ✅ ErrorDecoder           — ErrorDecoder 인터페이스
    //    ✅ Interceptor            — RequestInterceptor (헤더 주입 등)
    //    ✅ Encoder/Decoder        — Jackson, Gson, JAXB 등
    //    ✅ Logger                 — Logger.Level (NONE, BASIC, HEADERS, FULL)
    //    ✅ Contract               — Spring Contract (Spring MVC 어노테이션 사용 가능)
    //    ❌ 커넥션 풀              — 기본 백엔드는 풀 없음 (OkHttp/Apache 백엔드 사용 시 가능)
    //    ❌ writeTimeout           — 지원하지 않음
    //    ❌ responseTimeout        — readTimeout으로 대체
    //    ❌ 유휴/만료 퇴거          — 기본 백엔드 미지원
    //    ❌ Keep-Alive             — 기본 백엔드 미지원
    //    ❌ SSL/TLS 세밀 제어      — 기본 백엔드 미지원 (Client 구현체로 교체 시 가능)
    //    ❌ HTTP/2                — 기본 백엔드 미지원
    //    ❌ 압축(gzip)            — 수동 헤더 설정 필요
    //    ❌ 프록시                 — 기본 백엔드 미지원
    //    ❌ DNS 커스텀             — 지원하지 않음
    //
    //    참고: Spring Cloud OpenFeign을 사용하면 Ribbon/LoadBalancer 통합,
    //          Hystrix/Resilience4j 서킷 브레이커 통합 가능
    // ================================================================
    @Bean
    fun feignMockApi(@Value("\${test.server.url:http://localhost:8080}") baseUrl: String): FeignMockApi {
        return Feign.builder()
            // ── 인코더/디코더 ──
            .encoder(JacksonEncoder())                                 // Jackson JSON 인코더
            // JacksonDecoder는 JSON 응답을 DTO로 역직렬화할 때 사용.
            // 반환 타입이 String이면 기본 디코더(Decoder.Default)가 적합.
            // .decoder(JacksonDecoder())                               // Jackson JSON 디코더 (DTO 반환 시)
            // ── 타임아웃 ──
            .options(
                Request.Options(
                    3000, TimeUnit.MILLISECONDS,                       // connectTimeout: TCP 연결 타임아웃 (3초)
                    5000, TimeUnit.MILLISECONDS,                       // readTimeout: 소켓 읽기 타임아웃 (5초)
                    true                                               // followRedirects: 리다이렉트 허용
                )
            )
            // ── 재시도 정책 ──
            .retryer(
                Retryer.Default(
                    100,                                               // period: 재시도 간격 시작값 (100ms)
                    1000,                                              // maxPeriod: 재시도 간격 최대값 (1초, 지수 백오프)
                    3                                                  // maxAttempts: 최대 재시도 횟수 (3회)
                )
            )
            // ── 로깅 레벨 ──
            .logLevel(Logger.Level.BASIC)                              // NONE, BASIC, HEADERS, FULL
            // ── 에러 디코더 ──
            // .errorDecoder(ErrorDecoder.Default())                   // 기본 에러 디코더 (FeignException 발생)
            // ── 요청 인터셉터 (헤더 주입 등) ──
            // .requestInterceptor { template ->
            //     template.header("Authorization", "Bearer $token")
            //     template.header("X-Request-Id", UUID.randomUUID().toString())
            // }
            .target(FeignMockApi::class.java, baseUrl)
    }

    // ================================================================
    // 6. Retrofit
    //    - Square (2013~)
    //    - 선언적 HTTP 클라이언트 (어노테이션 기반)
    //    - 원래 Android용이지만 서버사이드에서도 활용
    //    - OkHttp를 기본 백엔드로 사용 (필수 의존성)
    //
    //    설정 가능 항목:
    //    → OkHttp의 모든 설정을 상속 (아래 OkHttp 섹션 참조)
    //    ✅ baseUrl                — Retrofit.Builder().baseUrl()
    //    ✅ Converter              — Jackson, Gson, Moshi, Protobuf, Scalars 등
    //    ✅ CallAdapter            — RxJava, CompletableFuture, Coroutine 등
    //    ✅ Interceptor            — OkHttp Interceptor (요청/응답 가로채기)
    //    ✅ 타임아웃/풀/SSL 등     — OkHttp 설정 그대로 적용
    //
    //    참고: Retrofit 자체는 네트워크 설정이 거의 없고,
    //          모든 저수준 설정은 OkHttp에 위임한다.
    // ================================================================
    @Bean
    fun retrofit(@Value("\${test.server.url:http://localhost:8080}") baseUrl: String): Retrofit {
        // Retrofit의 OkHttp 클라이언트 (타임아웃, 풀 등은 아래 OkHttp 섹션과 동일)
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(100, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl("$baseUrl/")                                      // baseUrl (반드시 '/'로 끝나야 함)
            .client(okHttpClient)                                      // OkHttp 클라이언트 주입
            // ── Converter ──
            .addConverterFactory(ScalarsConverterFactory.create())      // String 등 스칼라 타입 변환
            // ── CallAdapter ──
            // .addCallAdapterFactory(RxJava3CallAdapterFactory.create())   // RxJava 지원
            // .addCallAdapterFactory(CoroutineCallAdapterFactory())        // Coroutine 지원
            .build()
    }

    @Bean
    fun retrofitMockApi(retrofit: Retrofit): RetrofitMockApi {
        return retrofit.create(RetrofitMockApi::class.java)
    }

    // ================================================================
    // 7. OkHttp
    //    - Square (2013~)
    //    - 저수준 HTTP 클라이언트 (Retrofit의 백엔드)
    //    - Android + 서버사이드 모두 사용
    //    - HTTP/2, WebSocket 기본 지원
    //
    //    설정 가능 항목:
    //    ✅ connectTimeout         — TCP 연결 타임아웃
    //    ✅ readTimeout            — 소켓 읽기 타임아웃
    //    ✅ writeTimeout           — 요청 쓰기 타임아웃
    //    ✅ callTimeout            — 전체 호출 타임아웃 (연결 + 요청 + 응답 + 리다이렉트 포함)
    //    ✅ 커넥션 풀              — ConnectionPool (maxIdleConnections, keepAliveDuration)
    //    ✅ Keep-Alive             — ConnectionPool의 keepAliveDuration
    //    ✅ 재시도                  — retryOnConnectionFailure (연결 실패 시만)
    //    ✅ 리다이렉트             — followRedirects, followSslRedirects
    //    ✅ Interceptor            — Application Interceptor + Network Interceptor
    //    ✅ SSL/TLS               — sslSocketFactory + HostnameVerifier + CertificatePinner
    //    ✅ HTTP/2                — 기본 지원 (ALPN)
    //    ✅ 프록시                 — proxy(Proxy), proxySelector, proxyAuthenticator
    //    ✅ DNS                   — dns(Dns) 커스텀 DNS 리졸버
    //    ✅ 압축(gzip)            — 기본 Accept-Encoding: gzip (투명 압축)
    //    ✅ 캐시                  — cache(Cache) — 응답 캐싱
    //    ✅ 인증                  — authenticator(Authenticator)
    //    ✅ WebSocket             — newWebSocket() 지원
    //    ❌ Per-Route 풀 설정      — 전체 풀만 설정 가능
    //    ❌ 유휴 커넥션 퇴거 주기   — ConnectionPool이 자동 관리
    //    ❌ 커넥션 최대 수명(TTL)  — keepAliveDuration으로 간접 제어
    // ================================================================
    @Bean
    fun okHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // ── 타임아웃 ──
            .connectTimeout(3, TimeUnit.SECONDS)                       // TCP 연결 타임아웃 (3초, 기본값: 10초)
            .readTimeout(5, TimeUnit.SECONDS)                          // 소켓 읽기 타임아웃 (5초, 기본값: 10초)
            .writeTimeout(3, TimeUnit.SECONDS)                         // 요청 쓰기 타임아웃 (3초, 기본값: 10초)
            .callTimeout(10, TimeUnit.SECONDS)                         // 전체 호출 타임아웃 (10초, 기본값: 0=무제한)
            // ── 커넥션 풀 ──
            .connectionPool(
                ConnectionPool(
                    100,                                               // maxIdleConnections: 최대 유휴 커넥션 수 (기본값: 5)
                    5,                                                 // keepAliveDuration: 유휴 커넥션 유지 시간 (5분)
                    TimeUnit.MINUTES
                )
            )
            // ── 재시도 ──
            .retryOnConnectionFailure(true)                            // 연결 실패 시 재시도 (기본값: true)
            // ── 리다이렉트 ──
            .followRedirects(true)                                     // HTTP 리다이렉트 허용 (기본값: true)
            .followSslRedirects(true)                                  // HTTPS→HTTP 리다이렉트 허용 (기본값: true)
            // ── Interceptor (Application Level) ──
            // .addInterceptor { chain ->
            //     val request = chain.request().newBuilder()
            //         .addHeader("Authorization", "Bearer $token")
            //         .addHeader("X-Request-Id", UUID.randomUUID().toString())
            //         .build()
            //     chain.proceed(request)
            // }
            // ── Interceptor (Network Level) ──
            // .addNetworkInterceptor(HttpLoggingInterceptor().apply {
            //     level = HttpLoggingInterceptor.Level.BODY             // NONE, BASIC, HEADERS, BODY
            // })
            // ── SSL/TLS ──
            // .sslSocketFactory(sslContext.socketFactory, trustManager)
            // .hostnameVerifier { _, _ -> true }                       // 개발용 (프로덕션 금지!)
            // .certificatePinner(CertificatePinner.Builder()
            //     .add("api.example.com", "sha256/AAAA...")            // Certificate Pinning
            //     .build())
            // ── 프록시 ──
            // .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("proxy.example.com", 8080)))
            // .proxyAuthenticator { _, response ->
            //     response.request.newBuilder()
            //         .header("Proxy-Authorization", Credentials.basic("user", "pass"))
            //         .build()
            // }
            // ── DNS ──
            // .dns { hostname ->
            //     // 커스텀 DNS 리졸버 (ex: DoH, 캐싱 등)
            //     InetAddress.getAllByName(hostname).toList()
            // }
            // ── 캐시 ──
            // .cache(Cache(File("/tmp/okhttp-cache"), 50L * 1024 * 1024))  // 50MB 디스크 캐시
            .build()
    }

    // ================================================================
    // 8. Apache HttpClient 5
    //    - Apache HttpComponents (2005~, v5: 2020~)
    //    - 가장 오래되고 가장 설정이 풍부한 Java HTTP 클라이언트
    //    - Blocking (Classic) + Async 모드 모두 지원
    //    - 커넥션 풀, SSL, 프록시, 쿠키, 인증 등 엔터프라이즈급 기능
    //
    //    설정 가능 항목:
    //    ✅ connectTimeout         — ConnectionConfig.connectTimeout
    //    ✅ socketTimeout          — ConnectionConfig.socketTimeout
    //    ✅ responseTimeout        — RequestConfig.responseTimeout
    //    ✅ connectionRequestTimeout — RequestConfig.connectionRequestTimeout (풀 대기)
    //    ✅ 커넥션 풀 (전체)       — setMaxConnTotal
    //    ✅ 커넥션 풀 (호스트당)    — setMaxConnPerRoute
    //    ✅ 유휴 타임아웃          — evictIdleConnections
    //    ✅ 만료 커넥션 퇴거       — evictExpiredConnections
    //    ✅ 커넥션 최대 수명(TTL)  — ConnectionConfig.timeToLive
    //    ✅ 유휴 후 유효성 검증    — ConnectionConfig.validateAfterInactivity
    //    ✅ Keep-Alive 전략       — ConnectionKeepAliveStrategy
    //    ✅ 재시도 전략            — HttpRequestRetryStrategy (횟수, 간격, 멱등 판단)
    //    ✅ 리다이렉트 전략        — RedirectStrategy (최대 횟수, 순환 감지)
    //    ✅ SSL/TLS               — SSLContext + TlsStrategy (TLS 버전, 인증서, 호스트명 검증)
    //    ✅ 압축(gzip)            — ContentCompressionEnabled (기본 활성)
    //    ✅ 프록시                 — RoutePlanner + setProxy
    //    ✅ DNS                   — SystemDefaultDnsResolver 또는 커스텀
    //    ✅ 쿠키 관리             — CookieStore + CookieSpec
    //    ✅ 인증                  — CredentialsProvider (Basic, Digest, NTLM, Kerberos)
    //    ✅ 요청/응답 인터셉터     — HttpRequestInterceptor / HttpResponseInterceptor
    //    ❌ writeTimeout           — 직접 지원하지 않음 (소켓 레벨에서만)
    //    ❌ HTTP/2 (Classic)      — Classic 모드는 HTTP/1.1만 (Async 모드에서 HTTP/2 지원)
    //    ❌ WebSocket             — 미지원
    //
    //    참고: Apache HttpClient 5는 가장 세밀한 커넥션 풀 제어가 가능하며,
    //          엔터프라이즈 환경에서 프록시, 인증, 쿠키 관리 등이 필요할 때 최적.
    // ================================================================
    @Bean
    fun apacheHttpClient(): org.apache.hc.client5.http.impl.classic.CloseableHttpClient {
        // 커넥션 매니저 (풀 설정의 핵심)
        val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            // ── 커넥션 풀 크기 ──
            .setMaxConnTotal(200)                                      // 전체 최대 커넥션 수 (기본값: 25)
            .setMaxConnPerRoute(100)                                   // 호스트(라우트)당 최대 커넥션 수 (기본값: 5)
            // ── 커넥션 레벨 설정 ──
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(3000))        // TCP 연결 타임아웃 (3초)
                    .setSocketTimeout(Timeout.ofMilliseconds(5000))         // 소켓 읽기 타임아웃 (5초)
                    .setTimeToLive(TimeValue.ofMinutes(5))                  // 커넥션 최대 수명 (5분)
                    .setValidateAfterInactivity(TimeValue.ofSeconds(10))    // 유휴 후 재사용 전 검증 (10초)
                    .build()
            )
            // ── SSL/TLS ──
            // .setTlsSocketStrategy(DefaultClientTlsStrategy(
            //     SSLContexts.custom()
            //         .loadTrustMaterial(TrustAllStrategy.INSTANCE)       // 개발용 (프로덕션 금지!)
            //         .build(),
            //     NoopHostnameVerifier.INSTANCE
            // ))
            // ── DNS 리졸버 ──
            // .setDnsResolver(SystemDefaultDnsResolver())               // 기본 시스템 DNS 리졸버
            .build()

        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            // ── 요청 레벨 설정 ──
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(1000))  // 풀에서 커넥션 획득 대기 (1초)
                    .setResponseTimeout(Timeout.ofMilliseconds(5000))           // 전체 응답 타임아웃 (5초)
                    .setRedirectsEnabled(true)                                  // 리다이렉트 허용
                    .setMaxRedirects(50)                                        // 최대 리다이렉트 횟수
                    .setContentCompressionEnabled(true)                         // gzip/deflate 압축 지원
                    .build()
            )
            // ── 커넥션 퇴거 정책 ──
            .evictExpiredConnections()                                  // 만료된 커넥션 자동 퇴거
            .evictIdleConnections(TimeValue.ofSeconds(30))             // 30초 이상 유휴 커넥션 퇴거
            // ── 재시도 전략 ──
            // .setRetryStrategy(DefaultHttpRequestRetryStrategy(
            //     3,                                                     // 최대 재시도 횟수
            //     TimeValue.ofSeconds(1)                                 // 재시도 간격
            // ))
            // ── Keep-Alive 전략 ──
            // .setKeepAliveStrategy { response, context ->
            //     // 서버의 Keep-Alive 헤더 값 사용, 없으면 30초
            //     val keepAliveHeader = response.getFirstHeader("Keep-Alive")
            //     if (keepAliveHeader != null) {
            //         // timeout=N 파싱
            //         TimeValue.ofSeconds(30)
            //     } else {
            //         TimeValue.ofSeconds(30)
            //     }
            // }
            // ── 리다이렉트 전략 ──
            // .setRedirectStrategy(DefaultRedirectStrategy())           // 기본 리다이렉트 전략
            // ── 프록시 ──
            // .setProxy(HttpHost("proxy.example.com", 8080))
            // .setRoutePlanner(DefaultProxyRoutePlanner(HttpHost("proxy.example.com", 8080)))
            // ── 쿠키 관리 ──
            // .setDefaultCookieStore(BasicCookieStore())
            // ── 인증 ──
            // .setDefaultCredentialsProvider(BasicCredentialsProvider().apply {
            //     setCredentials(
            //         AuthScope(null, -1),
            //         UsernamePasswordCredentials("user", "pass".toCharArray())
            //     )
            // })
            // ── 요청/응답 인터셉터 ──
            // .addRequestInterceptorFirst { request, entity, context ->
            //     request.addHeader("X-Request-Id", UUID.randomUUID().toString())
            // }
            // .addResponseInterceptorLast { response, entity, context ->
            //     println("[Apache] Status: ${response.code}")
            // }
            .build()
    }

    // ================================================================
    // 9. Ktor HttpClient
    //    - JetBrains (2018~)
    //    - Kotlin 코루틴 네이티브 HTTP 클라이언트
    //    - 멀티플랫폼 지원 (JVM, JS, Native)
    //    - 엔진 교체 가능: CIO, OkHttp, Apache, Java, Curl, Darwin(iOS)
    //    - 여기서는 CIO(Coroutine I/O) 엔진 사용
    //
    //    설정 가능 항목:
    //    ✅ connectTimeout         — endpoint.connectTimeout (밀리초)
    //    ✅ socketTimeout          — endpoint.socketTimeout (밀리초)
    //    ✅ connectAttempts        — endpoint.connectAttempts (연결 시도 횟수)
    //    ✅ 커넥션 풀 (전체)       — endpoint.maxConnectionsCount
    //    ✅ 커넥션 풀 (호스트당)    — endpoint.maxConnectionsPerRoute (CIO 엔진)
    //    ✅ Keep-Alive 시간       — endpoint.keepAliveTime (밀리초)
    //    ✅ SSL/TLS               — https { ... } (TLS 버전, 인증서)
    //    ✅ 프록시                 — engine { proxy = ProxyBuilder.http("...") }
    //    ✅ 타임아웃 플러그인       — HttpTimeout (requestTimeout, connectTimeout, socketTimeout)
    //    ✅ 재시도                 — HttpRequestRetry 플러그인 (횟수, 간격, 조건)
    //    ✅ 로깅                  — Logging 플러그인 (요청/응답 로깅)
    //    ✅ 압축(gzip)            — ContentEncoding 플러그인
    //    ✅ 직렬화                — ContentNegotiation (Jackson, kotlinx.serialization 등)
    //    ✅ 인증                  — Auth 플러그인 (Basic, Bearer, Digest)
    //    ✅ 쿠키                  — HttpCookies 플러그인
    //    ✅ WebSocket             — WebSockets 플러그인
    //    ❌ writeTimeout           — CIO 엔진에서 직접 미지원
    //    ❌ 유휴/만료 퇴거 주기    — CIO 엔진이 자동 관리
    //    ❌ 커넥션 최대 수명(TTL)  — 설정 불가 (엔진 내부 관리)
    //    ❌ HTTP/2 (CIO)          — CIO 엔진은 HTTP/1.1만 (OkHttp/Java 엔진에서 지원)
    //    ❌ DNS 커스텀             — CIO 엔진 미지원 (OkHttp 엔진에서 가능)
    //
    //    참고: Ktor Client는 Kotlin 코루틴과 가장 자연스럽게 통합되며,
    //          suspend 함수로 모든 HTTP 호출이 이루어진다.
    //          KMP(Kotlin Multiplatform) 프로젝트에서 유일한 선택지.
    // ================================================================
    @Bean
    fun ktorHttpClient(): io.ktor.client.HttpClient {
        return io.ktor.client.HttpClient(CIO) {
            // ── CIO 엔진 설정 ──
            engine {
                // ── 커넥션 풀 ──
                maxConnectionsCount = 200                              // 전체 최대 커넥션 수 (기본값: 1000)

                endpoint {
                    maxConnectionsPerRoute = 100                       // 호스트당 최대 커넥션 수 (기본값: 100)
                    keepAliveTime = 5000                                // Keep-Alive 유지 시간 (5초, 기본값: 5000ms)
                    connectTimeout = 3000                               // TCP 연결 타임아웃 (3초, 기본값: 5000ms)
                    socketTimeout = 5000                                // 소켓 읽기 타임아웃 (5초, 기본값: infinite)
                    connectAttempts = 3                                 // 연결 시도 횟수 (3회, 기본값: 1)
                    pipelineMaxSize = 20                                // HTTP 파이프라이닝 최대 크기 (기본값: 20)
                }

                // ── SSL/TLS ──
                https {
                    // serverName = "api.example.com"                   // SNI 서버명
                    // trustManager = ...                               // 커스텀 TrustManager
                }

                // ── 프록시 ──
                // proxy = ProxyBuilder.http("http://proxy.example.com:8080/")
            }

            // ── 타임아웃 플러그인 (엔진 설정보다 우선) ──
            // install(HttpTimeout) {
            //     requestTimeoutMillis = 10000                        // 전체 요청 타임아웃 (10초)
            //     connectTimeoutMillis = 3000                         // TCP 연결 타임아웃 (3초)
            //     socketTimeoutMillis = 5000                          // 소켓 타임아웃 (5초)
            // }

            // ── 재시도 플러그인 ──
            // install(HttpRequestRetry) {
            //     retryOnServerErrors(maxRetries = 3)                 // 5xx 에러 시 최대 3회 재시도
            //     retryOnException(maxRetries = 3)                    // 예외 발생 시 최대 3회 재시도
            //     exponentialDelay()                                  // 지수 백오프 (100ms → 200ms → 400ms)
            //     modifyRequest { request ->
            //         request.headers.append("X-Retry-Count", retryCount.toString())
            //     }
            // }

            // ── 로깅 플러그인 ──
            // install(Logging) {
            //     logger = Logger.DEFAULT                             // SLF4J 로거
            //     level = LogLevel.INFO                               // NONE, INFO, HEADERS, BODY, ALL
            // }

            // ── 압축(gzip) 플러그인 ──
            // install(ContentEncoding) {
            //     gzip()
            //     deflate()
            // }

            // ── 직렬화(JSON) 플러그인 ──
            // install(ContentNegotiation) {
            //     jackson {
            //         // Jackson ObjectMapper 설정
            //     }
            // }

            // ── 인증 플러그인 ──
            // install(Auth) {
            //     bearer {
            //         loadTokens { BearerTokens("access-token", "refresh-token") }
            //         refreshTokens { BearerTokens("new-access", "new-refresh") }
            //     }
            // }

            // ── 기본 요청 설정 ──
            // defaultRequest {
            //     header("Accept", "application/json")
            //     contentType(ContentType.Application.Json)
            // }
        }
    }
}
