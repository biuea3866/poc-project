package com.example.inventory

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@RestController
@RequestMapping("/inventory")
class InventoryController(
    private val faultInjector: InventoryFaultInjector,
) {
    @PostMapping("/reserve")
    fun reserve(@RequestBody req: ReserveRequest): ReserveResponse {
        faultInjector.maybeFail("inventory")
        return ReserveResponse(
            orderId = req.orderId,
            sku = req.sku,
            quantity = req.quantity,
            status = "RESERVED",
        )
    }
}

data class ReserveRequest(val orderId: String, val sku: String, val quantity: Int)
data class ReserveResponse(val orderId: String, val sku: String, val quantity: Int, val status: String)

@Component
class InventoryFaultInjector(
    @Value("\${fault.delay-millis:0}") initialDelay: Long,
    @Value("\${fault.fail:false}") initialFail: Boolean,
) {
    private val delayMillis = AtomicLong(initialDelay)
    private val failFlag = AtomicBoolean(initialFail)

    fun maybeFail(label: String) {
        val delay = delayMillis.get()
        if (delay > 0) Thread.sleep(delay)
        if (failFlag.get()) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "$label injected failure")
        }
    }

    fun setDelay(ms: Long) = delayMillis.set(ms)
    fun setFail(fail: Boolean) = failFlag.set(fail)
    fun snapshot() = mapOf("delayMillis" to delayMillis.get(), "fail" to failFlag.get())
}

@RestController
@RequestMapping("/admin/fault")
class InventoryFaultAdminController(private val injector: InventoryFaultInjector) {
    @PostMapping("/delay")
    fun setDelay(@RequestParam ms: Long) = injector.also { it.setDelay(ms) }.snapshot()

    @PostMapping("/fail")
    fun setFail(@RequestParam fail: Boolean) = injector.also { it.setFail(fail) }.snapshot()
}
