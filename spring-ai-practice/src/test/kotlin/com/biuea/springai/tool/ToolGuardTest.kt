package com.biuea.springai.tool

import com.biuea.springai.audit.ToolAuditLogger
import com.biuea.springai.domain.Order
import com.biuea.springai.domain.OrderRepository
import com.biuea.springai.domain.Product
import com.biuea.springai.domain.ProductRepository
import com.biuea.springai.security.ScopeGuard
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

/**
 * ToolGuard / ScopeGuard 단위 테스트.
 *
 * 도구 호출이 LLM 의 동적 호출이든 직접 호출이든, 도구 진입부에서 명시 검증되는
 * 스코프 + 입력 검증 + 감사 로그 흐름을 검증한다.
 */
class ToolGuardTest {

    private lateinit var catalogTools: CatalogTools

    @BeforeEach
    fun setUp() {
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        val productRepository = ProductRepository().apply {
            saveAll(mapper.readValue<List<Product>>(ClassPathResource("data/products.json").inputStream))
        }
        val orderRepository = OrderRepository().apply {
            saveAll(mapper.readValue<List<Order>>(ClassPathResource("data/orders.json").inputStream))
        }
        val toolGuard = ToolGuard(ScopeGuard(), ToolAuditLogger(mapper))
        catalogTools = CatalogTools(productRepository, orderRepository, ToolInputValidator(), toolGuard)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun authenticate(vararg scopes: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            "test-user", null,
            scopes.map { SimpleGrantedAuthority("SCOPE_$it") },
        )
    }

    private fun messageOf(e: Throwable): String = e.message ?: ""

    @Test
    fun `G-01 인증 객체가 없으면 searchProducts 호출 시 AccessDeniedException`() {
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.searchProducts(null, null, "블랙", null)
        }
        assertTrue(messageOf(e).contains("authentication required"))
    }

    @Test
    fun `G-02 catalog 스코프 없이 searchProducts 호출 시 AccessDeniedException`() {
        authenticate("order:read")
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.searchProducts(null, null, "블랙", null)
        }
        assertTrue(messageOf(e).contains("catalog:read"))
    }

    @Test
    fun `G-03 catalog 스코프 보유 시 searchProducts 정상 동작`() {
        authenticate("catalog:read")
        val results = catalogTools.searchProducts(null, null, "블랙", null)
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `G-04 order 스코프 없이 getOrderStatus 호출 시 AccessDeniedException`() {
        authenticate("catalog:read")
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.getOrderStatus("ORD-1001")
        }
        assertTrue(messageOf(e).contains("order:read"))
    }

    @Test
    fun `G-05 catalog 스코프 보유 시 checkInventory 입력 검증 위반은 IllegalArgumentException`() {
        authenticate("catalog:read")
        val e = assertThrows(IllegalArgumentException::class.java) {
            catalogTools.checkInventory("DROP_TABLE", "M")
        }
        assertTrue(messageOf(e).contains("productId"))
    }

    @Test
    fun `G-06 양쪽 스코프 보유 시 모든 도구 정상 동작`() {
        authenticate("catalog:read", "order:read")
        assertTrue(catalogTools.searchProducts(null, null, null, 30_000).isNotEmpty())
        assertEquals(12, catalogTools.checkInventory("P-1001", "S").quantity)
        assertEquals("배송중", catalogTools.getOrderStatus("ORD-1001").status)
    }

    @Test
    fun `G-07 catalog 스코프 없이 getProductDetails 호출 시 거부`() {
        authenticate("order:read")
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.getProductDetails("P-1001")
        }
        assertTrue(messageOf(e).contains("catalog:read"))
    }

    @Test
    fun `G-08 catalog 스코프 없이 listCategories 호출 시 거부`() {
        authenticate("order:read")
        assertThrows(AccessDeniedException::class.java) { catalogTools.listCategories() }
    }

    @Test
    fun `G-09 order 스코프 없이 listOrders 호출 시 거부`() {
        authenticate("catalog:read")
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.listOrders(null)
        }
        assertTrue(messageOf(e).contains("order:read"))
    }

    @Test
    fun `G-10 order 스코프 없이 trackShipment 호출 시 거부`() {
        authenticate("catalog:read")
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.trackShipment("ORD-1001")
        }
        assertTrue(messageOf(e).contains("order:read"))
    }

    @Test
    fun `G-11 order_write 스코프 없이 placeOrder 호출 시 거부`() {
        authenticate("catalog:read", "order:read")
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.placeOrder("P-1001", "S", 1)
        }
        assertTrue(messageOf(e).contains("order:write"))
    }

    @Test
    fun `G-12 order_write 스코프 없이 cancelOrder 호출 시 거부`() {
        authenticate("catalog:read", "order:read")
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.cancelOrder("ORD-1003")
        }
        assertTrue(messageOf(e).contains("order:write"))
    }

    @Test
    fun `G-13 shipment_write 스코프 없이 addTrackingEvent 호출 시 거부`() {
        authenticate("catalog:read", "order:read", "order:write")
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.addTrackingEvent("ORD-1005", "발송", "용인 풀필먼트 센터")
        }
        assertTrue(messageOf(e).contains("shipment:write"))
    }

    @Test
    fun `G-14 catalog_write 스코프 없이 restockProduct 호출 시 거부`() {
        authenticate("catalog:read", "order:read", "order:write", "shipment:write")
        val e = assertThrows(AccessDeniedException::class.java) {
            catalogTools.restockProduct("P-1001", "S", 5)
        }
        assertTrue(messageOf(e).contains("catalog:write"))
    }

    @Test
    fun `G-15 ops 권한 (전체 스코프) 시 쓰기 도구 모두 정상 동작`() {
        authenticate(
            "catalog:read", "catalog:write",
            "order:read", "order:write", "shipment:write",
        )
        val placed = catalogTools.placeOrder("P-1001", "S", 1)
        val cancelled = catalogTools.cancelOrder(placed.orderId)
        assertEquals("취소", cancelled.status)
        val updated = catalogTools.addTrackingEvent("ORD-1005", "발송", "용인")
        assertEquals("발송", updated.currentStatus)
        val restock = catalogTools.restockProduct("P-1001", "S", 3)
        assertEquals(restock.before + 3, restock.after)
    }
}
