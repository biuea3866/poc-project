package com.biuea.httpclient.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * 벤치마크용 Mock 엔드포인트.
 *
 * Mono.delay()를 사용하여 논블로킹으로 지연을 시뮬레이션한다.
 * 서버 스레드 풀이 고갈되지 않으므로 동시 요청을 무제한으로 처리할 수 있다.
 */
@RestController
class MockController {

    /**
     * 지정된 시간만큼 지연 후 응답을 반환한다.
     *
     * @param delayMs 지연 시간 (밀리초), 기본값 100ms
     */
    @GetMapping("/api/mock/delay")
    fun delay(@RequestParam(defaultValue = "100") delayMs: Long): Mono<MockResponse> {
        return Mono.delay(Duration.ofMillis(delayMs))
            .map {
                MockResponse(
                    message = "ok",
                    timestamp = System.currentTimeMillis(),
                    thread = Thread.currentThread().name
                )
            }
    }

    /**
     * 헬스 체크 엔드포인트.
     */
    @GetMapping("/api/mock/health")
    fun health(): Map<String, String> = mapOf("status" to "UP")

    data class MockResponse(
        val message: String,
        val timestamp: Long,
        val thread: String
    )
}
