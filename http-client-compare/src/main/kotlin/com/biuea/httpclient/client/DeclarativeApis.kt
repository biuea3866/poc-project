package com.biuea.httpclient.client

import feign.Param
import feign.RequestLine
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import reactor.core.publisher.Mono
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// ================================================================
// 선언적(Declarative) HTTP 클라이언트 인터페이스 모음
//
// 각 라이브러리별 어노테이션 스타일 비교:
//
// | 라이브러리       | 어노테이션           | 파라미터          | 반환 타입          |
// |-----------------|---------------------|------------------|--------------------|
// | OpenFeign       | @RequestLine        | @Param           | 일반 객체 (동기)    |
// | Retrofit        | @GET, @POST 등      | @Query           | Call<T> (동기/비동기)|
// | HTTP Interface  | @GetExchange 등     | @RequestParam    | T (동기) / Mono (비동기)|
// ================================================================

// ================================================================
// 1. OpenFeign API
//    - @RequestLine("METHOD /path") 어노테이션 사용
//    - @Param으로 쿼리 파라미터 바인딩
//    - 반환 타입이 일반 객체이므로 동기(blocking) 호출
//    - Spring Cloud Feign과 달리 raw Feign은 Spring MVC 어노테이션 미사용
// ================================================================
interface FeignMockApi {

    /**
     * 지연 Mock 엔드포인트 호출.
     *
     * @param delayMs 서버 지연 시간 (밀리초)
     * @return 응답 문자열 (JSON)
     */
    @RequestLine("GET /api/mock/delay?delayMs={delayMs}")
    fun getWithDelay(@Param("delayMs") delayMs: Long): String

    /**
     * 헬스 체크 엔드포인트.
     */
    @RequestLine("GET /api/mock/health")
    fun health(): String
}

// ================================================================
// 2. Retrofit API
//    - @GET, @POST, @PUT, @DELETE 등 HTTP 메서드 어노테이션
//    - @Query로 쿼리 파라미터 바인딩
//    - @Path로 경로 변수 바인딩
//    - @Body로 요청 본문 바인딩
//    - 반환 타입 Call<T>: execute() = 동기, enqueue() = 비동기 콜백
//    - suspend 함수로 선언하면 코루틴 지원 (Call<T> 불필요)
// ================================================================
interface RetrofitMockApi {

    /**
     * 지연 Mock 엔드포인트 호출 (동기).
     *
     * Call<String>.execute()로 동기 호출,
     * Call<String>.enqueue()로 비동기 콜백 호출.
     */
    @GET("/api/mock/delay")
    fun getWithDelay(@Query("delayMs") delayMs: Long): Call<String>

    /**
     * 헬스 체크 엔드포인트.
     */
    @GET("/api/mock/health")
    fun health(): Call<String>
}

// ================================================================
// 3. Spring HTTP Interface (Blocking)
//    - @GetExchange, @PostExchange 등 사용
//    - @RequestParam, @PathVariable, @RequestBody로 파라미터 바인딩
//    - RestClient 백엔드 → 반환 타입이 일반 객체 → 동기 호출
//    - Spring이 인터페이스 프록시를 자동 생성 (HttpServiceProxyFactory)
// ================================================================
interface BlockingHttpInterfaceApi {

    /**
     * 지연 Mock 엔드포인트 (동기).
     *
     * RestClient 백엔드이므로 호출 스레드가 응답까지 블로킹된다.
     */
    @GetExchange("/api/mock/delay")
    fun getWithDelay(@RequestParam delayMs: Long): String

    /**
     * 헬스 체크 엔드포인트.
     */
    @GetExchange("/api/mock/health")
    fun health(): String
}

// ================================================================
// 4. Spring HTTP Interface (Reactive)
//    - 동일한 어노테이션 (@GetExchange 등)
//    - WebClient 백엔드 → 반환 타입이 Mono/Flux → 비동기 논블로킹 호출
//    - 구독(subscribe) 시점에 실제 HTTP 호출이 발생
//    - 동일 인터페이스를 RestClient/WebClient 양쪽 백엔드로 사용할 수 있으나,
//      반환 타입이 다르므로 별도 인터페이스로 분리하는 것이 명확하다.
// ================================================================
interface ReactiveHttpInterfaceApi {

    /**
     * 지연 Mock 엔드포인트 (비동기).
     *
     * WebClient 백엔드이므로 논블로킹으로 동작한다.
     * Mono<String>을 구독해야 실제 요청이 발생.
     */
    @GetExchange("/api/mock/delay")
    fun getWithDelay(@RequestParam delayMs: Long): Mono<String>

    /**
     * 헬스 체크 엔드포인트.
     */
    @GetExchange("/api/mock/health")
    fun health(): Mono<String>
}
