package com.example.order

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "ratelimit.redis.limit-per-second=5",
        "ratelimit.redis.window-seconds=1",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisRateLimiterTest {

    companion object {
        private val payment = WireMockServer(wireMockConfig().dynamicPort()).also {
            it.start()
            it.stubFor(
                post(urlEqualTo("/payments")).willReturn(
                    aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"paymentId":"P","orderId":"x","amount":100,"status":"APPROVED"}""")
                )
            )
        }

        private val inventory = WireMockServer(wireMockConfig().dynamicPort()).also {
            it.start()
            it.stubFor(
                post(urlEqualTo("/inventory/reserve")).willReturn(
                    aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"orderId":"x","sku":"SKU","quantity":1,"status":"RESERVED"}""")
                )
            )
        }

        private val redis = RedisContainer(DockerImageName.parse("redis:7.4-alpine")).also { it.start() }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            payment.stop()
            inventory.stop()
            redis.stop()
        }

        @DynamicPropertySource
        @JvmStatic
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("downstream.payment.base-url") { "http://localhost:${payment.port()}" }
            registry.add("downstream.inventory.base-url") { "http://localhost:${inventory.port()}" }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
        }
    }

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var rest: TestRestTemplate

    @Test
    fun `Redis 글로벌 RateLimiter - 동시 30건 호출시 limit 초과분은 429`() {
        val totalRequests = 30
        val workers = 30
        val latch = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(workers)

        // 모든 스레드가 latch를 기다리다 동시에 발사하도록 한다.
        // 그렇지 않으면 호출이 1초 윈도우 너머로 분산되어 limit이 자연스럽게 충족된다.
        val futures = (1..totalRequests).map {
            pool.submit<Int> {
                latch.await()
                rest.postForEntity(
                    "http://localhost:$port/orders/global",
                    mapOf("sku" to "SKU", "quantity" to 1, "amount" to 100),
                    String::class.java,
                ).statusCode.value()
            }
        }
        latch.countDown()
        val statuses = futures.map { it.get(15, TimeUnit.SECONDS) }
        pool.shutdown()

        val tooMany = statuses.count { it == 429 }
        assertTrue(tooMany > 0) { "expected at least one 429 but got: $statuses" }
    }
}
