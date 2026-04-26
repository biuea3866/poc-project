package com.example.payment

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var injector: FaultInjector

    @AfterEach
    fun reset() {
        injector.setDelay(0)
        injector.setFail(false)
    }

    @Test
    fun `정상 응답 - APPROVED 반환`() {
        val res = rest.postForEntity(
            "http://localhost:$port/payments",
            PaymentRequest(orderId = "O1", amount = 100),
            PaymentResponse::class.java,
        )
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals("APPROVED", res.body!!.status)
    }

    @Test
    fun `fault inject - fail=true 시 500 응답`() {
        injector.setFail(true)
        val res = rest.postForEntity(
            "http://localhost:$port/payments",
            PaymentRequest(orderId = "O1", amount = 100),
            String::class.java,
        )
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.statusCode)
    }

    @Test
    fun `fault inject - delay 적용시 호출 시간이 기준 이상`() {
        injector.setDelay(500)
        val started = System.currentTimeMillis()
        val res = rest.postForEntity(
            "http://localhost:$port/payments",
            PaymentRequest(orderId = "O1", amount = 100),
            PaymentResponse::class.java,
        )
        val elapsed = System.currentTimeMillis() - started
        assertEquals(HttpStatus.OK, res.statusCode)
        assertTrue(elapsed >= 500) { "elapsed=$elapsed must be >= 500ms" }
    }

    @Test
    fun `FaultInjector 단위 - fail flag 동작`() {
        val unit = FaultInjector(0, true)
        assertThrows<ResponseStatusException> { unit.maybeFail("payment") }
    }
}
