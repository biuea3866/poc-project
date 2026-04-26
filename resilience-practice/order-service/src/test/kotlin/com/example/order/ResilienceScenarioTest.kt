package com.example.order

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResilienceScenarioTest {

    companion object {
        private val payment = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }
        private val inventory = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }

        @AfterAll
        @JvmStatic
        fun stopMocks() {
            payment.stop()
            inventory.stop()
        }

        @DynamicPropertySource
        @JvmStatic
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("downstream.payment.base-url") { "http://localhost:${payment.port()}" }
            registry.add("downstream.inventory.base-url") { "http://localhost:${inventory.port()}" }
        }
    }

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var cbRegistry: CircuitBreakerRegistry

    private fun stubInventoryOk() {
        inventory.stubFor(
            post(urlEqualTo("/inventory/reserve"))
                .willReturn(
                    aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"orderId":"x","sku":"SKU","quantity":1,"status":"RESERVED"}""")
                )
        )
    }

    private fun stubPaymentOk() {
        payment.stubFor(
            post(urlEqualTo("/payments"))
                .willReturn(
                    aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"paymentId":"P1","orderId":"x","amount":100,"status":"APPROVED"}""")
                )
        )
    }

    private fun stubPaymentSlow(delayMillis: Int) {
        payment.stubFor(
            post(urlEqualTo("/payments"))
                .willReturn(
                    aResponse().withStatus(200)
                        .withFixedDelay(delayMillis)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"paymentId":"P1","orderId":"x","amount":100,"status":"APPROVED"}""")
                )
        )
    }

    private fun stubPaymentDown() {
        payment.stubFor(
            post(urlEqualTo("/payments"))
                .willReturn(aResponse().withStatus(500))
        )
    }

    private fun resetCircuitBreakers() {
        cbRegistry.allCircuitBreakers.forEach { it.reset() }
    }

    private fun placeOrder(): org.springframework.http.ResponseEntity<String> =
        rest.postForEntity(
            "http://localhost:$port/orders",
            mapOf("sku" to "SKU", "quantity" to 1, "amount" to 100),
            String::class.java,
        )

    @Test
    fun `정상 흐름 - 주문 성공`() {
        payment.resetAll(); inventory.resetAll(); resetCircuitBreakers()
        stubPaymentOk(); stubInventoryOk()

        val res = placeOrder()
        res.statusCode shouldBe HttpStatus.OK
        res.body!! shouldContain "APPROVED"
    }

    @Test
    fun `타임아웃 - payment 응답 5초 지연시 fallback PENDING`() {
        payment.resetAll(); inventory.resetAll(); resetCircuitBreakers()
        stubPaymentSlow(5_000); stubInventoryOk()

        val res = placeOrder()
        res.statusCode shouldBe HttpStatus.OK
        res.body!! shouldContain "PENDING"
        res.body!! shouldContain "FALLBACK"
    }

    @Test
    fun `서킷 OPEN - payment 연속 실패시 OPEN 상태로 전이되고 즉시 fallback`() {
        payment.resetAll(); inventory.resetAll(); resetCircuitBreakers()
        stubPaymentDown(); stubInventoryOk()

        repeat(10) { placeOrder() }

        val cb = cbRegistry.circuitBreaker("paymentCb")
        cb.state shouldBe CircuitBreaker.State.OPEN

        shouldNotThrowAny {
            val res = placeOrder()
            res.statusCode shouldBe HttpStatus.OK
            res.body!! shouldContain "PENDING"
        }
    }

    @Test
    fun `RateLimiter - 초당 10건 초과시 429 발생`() {
        payment.resetAll(); inventory.resetAll(); resetCircuitBreakers()
        stubPaymentOk(); stubInventoryOk()

        val pool = Executors.newFixedThreadPool(20)
        val statuses = (1..30).map {
            pool.submit<Int> { placeOrder().statusCode.value() }
        }.map { it.get(5, TimeUnit.SECONDS) }
        pool.shutdown()

        val tooMany = statuses.count { it == 429 }
        check(tooMany > 0) { "expected at least one 429 but got: $statuses" }
    }
}
