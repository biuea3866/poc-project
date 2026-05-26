package com.biuea.springai.tool

import com.biuea.springai.domain.Order
import com.biuea.springai.domain.OrderRepository
import com.biuea.springai.domain.Product
import com.biuea.springai.domain.ProductRepository
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

/**
 * CatalogTools 단위 테스트 — LLM 없이 인메모리 시드 데이터로 도구 본문 로직을 검증한다.
 *
 * 스코프 검사 + 감사 로그는 `GuardedToolCallback` 데코레이터가 처리하므로 본 테스트에서는 제외한다.
 * (해당 흐름은 [com.biuea.springai.security.GuardedToolCallbackTest] 에서 검증)
 */
class CatalogToolsTest {

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
        catalogTools = CatalogTools(productRepository, orderRepository, ToolInputValidator())
    }

    @Test
    fun `searchProducts 는 색상으로 필터링한다`() {
        val results = catalogTools.searchProducts(
            keyword = null, category = null, color = "블랙", maxPrice = null,
        )
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.color == "블랙" })
    }

    @Test
    fun `searchProducts 는 최대가격 이하 상품만 반환한다`() {
        val results = catalogTools.searchProducts(
            keyword = null, category = null, color = null, maxPrice = 30000,
        )
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.priceKrw <= 30000 })
    }

    @Test
    fun `checkInventory 는 보유 재고 수량을 반환한다`() {
        val result = catalogTools.checkInventory("P-1001", "S")
        assertEquals(12, result.quantity)
    }

    @Test
    fun `checkInventory 는 재고 0 인 사이즈를 품절로 안내한다`() {
        val result = catalogTools.checkInventory("P-1001", "L")
        assertEquals(0, result.quantity)
        assertTrue(result.message.contains("품절"))
    }

    @Test
    fun `checkInventory 는 존재하지 않는 상품을 안내한다`() {
        val result = catalogTools.checkInventory("P-9999", "M")
        assertTrue(result.message.contains("찾을 수 없"))
    }

    @Test
    fun `getOrderStatus 는 주문 상태를 반환한다`() {
        val result = catalogTools.getOrderStatus("ORD-1001")
        assertEquals("배송중", result.status)
        assertEquals("슬림 스트레이트 청바지", result.productName)
    }

    @Test
    fun `getOrderStatus 는 존재하지 않는 주문을 안내한다`() {
        val result = catalogTools.getOrderStatus("ORD-9999")
        assertTrue(result.status.contains("찾을 수 없"))
    }

    @Test
    fun `getProductDetails 는 사이즈별 재고와 메타정보를 반환한다`() {
        val details = catalogTools.getProductDetails("P-1001")
        assertEquals("옥스포드 코튼 셔츠", details.name)
        assertTrue(details.sizeStock.isNotEmpty())
        assertEquals(details.sizeStock.sumOf { it.quantity }, details.totalStock)
    }

    @Test
    fun `listCategories 는 등록된 카테고리와 색상 목록을 반환한다`() {
        val catalog = catalogTools.listCategories()
        assertTrue(catalog.categories.isNotEmpty())
        assertTrue(catalog.colors.isNotEmpty())
        assertEquals(catalog.categories.distinct(), catalog.categories)
    }

    @Test
    fun `listOrders 는 전체 주문을 반환한다`() {
        val orders = catalogTools.listOrders(null)
        assertEquals(5, orders.size)
    }

    @Test
    fun `listOrders 는 상태로 필터링한다`() {
        val delivered = catalogTools.listOrders("배송완료")
        assertEquals(1, delivered.size)
        assertEquals("ORD-1002", delivered.first().orderId)
    }

    @Test
    fun `listOrders 는 허용되지 않는 상태에 대해 IllegalArgumentException`() {
        val e = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            catalogTools.listOrders("폐기")
        }
        val message = e.message ?: ""
        assertTrue(message.contains("허용"))
    }

    @Test
    fun `trackShipment 는 배송 단계 이벤트를 시간순으로 반환한다`() {
        val track = catalogTools.trackShipment("ORD-1002")
        assertEquals("배송완료", track.currentStatus)
        val statuses = track.events.map { it.status }
        assertTrue(statuses.contains("결제완료"))
        assertTrue(statuses.contains("배송완료"))
        // 시간순 (오름차순) 검증
        val timestamps = track.events.map { it.timestamp }
        assertEquals(timestamps.sorted(), timestamps)
    }

    // ─── 쓰기 도구 ──────────────────────────────────────────────────────

    @Test
    fun `placeOrder 는 새 주문을 생성하고 재고를 차감한다`() {
        val beforeStock = catalogTools.checkInventory("P-1001", "S").quantity
        val placed = catalogTools.placeOrder("P-1001", "S", 2)
        assertEquals("P-1001", placed.productId)
        assertEquals(2, placed.quantity)
        assertEquals(beforeStock - 2, placed.remainingStock)
        assertEquals("결제완료", placed.status)
        // 새 주문이 목록에 추가됨
        val orders = catalogTools.listOrders(null)
        assertTrue(orders.any { it.orderId == placed.orderId })
        // 재고 차감 반영
        assertEquals(beforeStock - 2, catalogTools.checkInventory("P-1001", "S").quantity)
    }

    @Test
    fun `placeOrder 는 재고 부족 시 IllegalArgumentException`() {
        // P-1001 / S 는 시드 데이터로 12개 보유. 수량 13 으로 재고 부족을 일으킨다.
        val e = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            catalogTools.placeOrder("P-1001", "S", 13)
        }
        val message = e.message ?: ""
        assertTrue(message.contains("재고가 부족"))
    }

    @Test
    fun `placeOrder 는 수량이 0 이하면 IllegalArgumentException`() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            catalogTools.placeOrder("P-1001", "S", 0)
        }
    }

    @Test
    fun `cancelOrder 는 결제완료 주문을 취소 상태로 변경한다`() {
        val cancelled = catalogTools.cancelOrder("ORD-1003")
        assertEquals("취소", cancelled.status)
        val track = catalogTools.trackShipment("ORD-1003")
        assertEquals("취소", track.currentStatus)
        assertEquals("취소", track.events.last().status)
    }

    @Test
    fun `cancelOrder 는 배송완료 주문을 취소할 수 없다`() {
        val e = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            catalogTools.cancelOrder("ORD-1002")
        }
        val message = e.message ?: ""
        assertTrue(message.contains("배송완료"))
    }

    @Test
    fun `addTrackingEvent 는 이벤트를 추가하고 currentStatus 를 갱신한다`() {
        val before = catalogTools.trackShipment("ORD-1005").events.size
        val updated = catalogTools.addTrackingEvent("ORD-1005", "발송", "용인 풀필먼트 센터")
        assertEquals("발송", updated.currentStatus)
        assertEquals(before + 1, updated.events.size)
        assertEquals("발송", updated.events.last().status)
        assertEquals("용인 풀필먼트 센터", updated.events.last().location)
    }

    @Test
    fun `restockProduct 는 재고를 가감한다 (양수 입고)`() {
        val before = catalogTools.checkInventory("P-1001", "S").quantity
        val result = catalogTools.restockProduct("P-1001", "S", 5)
        assertEquals(before, result.before)
        assertEquals(before + 5, result.after)
        assertEquals(before + 5, catalogTools.checkInventory("P-1001", "S").quantity)
    }

    @Test
    fun `restockProduct 는 출고로 재고가 음수가 되면 IllegalArgumentException`() {
        val before = catalogTools.checkInventory("P-1001", "S").quantity
        val e = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            catalogTools.restockProduct("P-1001", "S", -(before + 1))
        }
        val message = e.message ?: ""
        assertTrue(message.contains("재고가 음수"))
    }

    @Test
    fun `restockProduct 는 delta 0 이면 IllegalArgumentException`() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            catalogTools.restockProduct("P-1001", "S", 0)
        }
    }
}
