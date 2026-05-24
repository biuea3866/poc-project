package com.biuea.springai.tool

import com.biuea.springai.domain.Order
import com.biuea.springai.domain.OrderRepository
import com.biuea.springai.domain.ProductRepository
import com.biuea.springai.domain.TrackingEvent
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 의류 이커머스 카탈로그 도구 — 같은 빈을 인앱 ChatClient(`.tools(...)`) 와 MCP 서버 양쪽이 공유.
 *
 * 스코프 매핑:
 *   - catalog:read → searchProducts, getProductDetails, listCategories, checkInventory
 *   - order:read   → listOrders, getOrderStatus, trackShipment
 *
 * 모든 도구는 ToolGuard.invoke() 로 감싸 스코프 검증·감사 로그·예외 분류를 일관 처리한다.
 */
@Component
class CatalogTools(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val toolInputValidator: ToolInputValidator,
    private val toolGuard: ToolGuard,
) {

    // ─── 상품 탐색 ─────────────────────────────────────────────────────

    @Tool(description = "의류 상품을 키워드·카테고리·색상·최대가격으로 검색해 목록을 반환한다. 조건이 비어있으면 전체에서 필터링한다.")
    fun searchProducts(
        @ToolParam(description = "검색 키워드 (상품명/설명/카테고리). 조건 없으면 빈 문자열", required = false)
        keyword: String?,
        @ToolParam(description = "카테고리 (예: 셔츠, 자켓, 청바지). 조건 없으면 빈 문자열", required = false)
        category: String?,
        @ToolParam(description = "색상 (예: 블랙, 화이트). 조건 없으면 빈 문자열", required = false)
        color: String?,
        @ToolParam(description = "최대 가격(원). 조건 없으면 0", required = false)
        maxPrice: Int?,
    ): List<ProductSummary> = toolGuard.invoke(
        tool = "searchProducts",
        scope = "catalog:read",
        args = mapOf("keyword" to keyword, "category" to category, "color" to color, "maxPrice" to maxPrice),
    ) {
        val safeKeyword = toolInputValidator.sanitizeFreeText(keyword)
        val safeCategory = toolInputValidator.sanitizeCategory(category)
        val safeColor = toolInputValidator.sanitizeColor(color)
        val safeMaxPrice = toolInputValidator.sanitizeMaxPrice(maxPrice)
        productRepository.search(
            keyword = safeKeyword,
            category = safeCategory,
            color = safeColor,
            maxPrice = safeMaxPrice,
        ).map { it.toSummary() }
    }

    @Tool(description = "단일 상품의 상세 정보를 반환한다. 사이즈별 재고, 소재, 시즌, 설명을 포함한다.")
    fun getProductDetails(
        @ToolParam(description = "상품 ID (예: P-1001)") productId: String,
    ): ProductDetails = toolGuard.invoke(
        tool = "getProductDetails",
        scope = "catalog:read",
        args = mapOf("productId" to productId),
    ) {
        val safeProductId = toolInputValidator.requireProductId(productId)
        val product = productRepository.findById(safeProductId)
            ?: throw NoSuchElementException("상품을 찾을 수 없습니다: $safeProductId")
        ProductDetails(
            id = product.id,
            name = product.name,
            category = product.category,
            color = product.color,
            material = product.material,
            fit = product.fit,
            season = product.season,
            priceKrw = product.priceKrw,
            description = product.description,
            sizeStock = product.sizes.map { SizeStock(size = it, quantity = product.stockOf(it)) },
            totalStock = product.totalStock(),
        )
    }

    @Tool(description = "카탈로그에 등록된 카테고리·색상 목록을 반환한다. 사용자가 어떤 옵션이 있는지 물을 때 사용한다.")
    fun listCategories(): CategoryColorCatalog = toolGuard.invoke(
        tool = "listCategories",
        scope = "catalog:read",
        args = emptyMap(),
    ) {
        CategoryColorCatalog(
            categories = productRepository.listCategories(),
            colors = productRepository.listColors(),
        )
    }

    @Tool(description = "특정 상품의 특정 사이즈 재고 수량을 조회한다.")
    fun checkInventory(
        @ToolParam(description = "상품 ID (예: P-1001)") productId: String,
        @ToolParam(description = "사이즈 (예: M, L, 30)") size: String,
    ): InventoryResult = toolGuard.invoke(
        tool = "checkInventory",
        scope = "catalog:read",
        args = mapOf("productId" to productId, "size" to size),
    ) {
        val safeProductId = toolInputValidator.requireProductId(productId)
        val safeSize = toolInputValidator.requireSize(size)
        val product = productRepository.findById(safeProductId)
            ?: return@invoke InventoryResult(safeProductId, safeSize, 0, "상품을 찾을 수 없습니다.")
        val quantity = product.stockOf(safeSize)
        val message = if (quantity > 0) "재고 ${quantity}개 보유" else "해당 사이즈는 품절입니다."
        InventoryResult(safeProductId, safeSize, quantity, message)
    }

    // ─── 주문 / 배송 ────────────────────────────────────────────────────

    @Tool(description = "주문 목록을 조회한다. status 가 비어있으면 전체, 지정 시 해당 상태만 반환한다. " +
        "허용 상태: 결제완료, 배송준비중, 배송중, 배송완료, 취소")
    fun listOrders(
        @ToolParam(description = "주문 상태 필터. 조건 없으면 빈 문자열", required = false)
        status: String?,
    ): List<OrderSummary> = toolGuard.invoke(
        tool = "listOrders",
        scope = "order:read",
        args = mapOf("status" to status),
    ) {
        val safeStatus = toolInputValidator.sanitizeOrderStatus(status)
        val rows = if (safeStatus == null) orderRepository.findAll() else orderRepository.findByStatus(safeStatus)
        rows.map {
            OrderSummary(
                orderId = it.orderId,
                productName = it.productName,
                size = it.size,
                quantity = it.quantity,
                status = it.status,
                orderedAt = it.orderedAt,
            )
        }
    }

    @Tool(description = "주문 번호로 주문 상태와 배송 정보를 조회한다.")
    fun getOrderStatus(
        @ToolParam(description = "주문 번호 (예: ORD-1001)") orderId: String,
    ): OrderStatusResult = toolGuard.invoke(
        tool = "getOrderStatus",
        scope = "order:read",
        args = mapOf("orderId" to orderId),
    ) {
        val safeOrderId = toolInputValidator.requireOrderId(orderId)
        val order = orderRepository.findById(safeOrderId)
            ?: return@invoke OrderStatusResult(safeOrderId, "주문을 찾을 수 없습니다.", null, null)
        OrderStatusResult(
            orderId = order.orderId,
            status = order.status,
            productName = order.productName,
            trackingNo = order.trackingNo,
        )
    }

    @Tool(description = "주문의 배송 단계별 이벤트(타임라인)를 시간순으로 반환한다. " +
        "결제완료 → 상품 준비중 → 발송 → 간선 운송 → 배송중 → 배송완료/취소 순.")
    fun trackShipment(
        @ToolParam(description = "주문 번호 (예: ORD-1001)") orderId: String,
    ): ShipmentTrack = toolGuard.invoke(
        tool = "trackShipment",
        scope = "order:read",
        args = mapOf("orderId" to orderId),
    ) {
        val safeOrderId = toolInputValidator.requireOrderId(orderId)
        val order = orderRepository.findById(safeOrderId)
            ?: throw NoSuchElementException("주문을 찾을 수 없습니다: $safeOrderId")
        ShipmentTrack(
            orderId = order.orderId,
            currentStatus = order.status,
            trackingNo = order.trackingNo,
            events = order.trackingEvents,
        )
    }

    // ─── 쓰기 도구 ──────────────────────────────────────────────────────

    @Tool(description = "사용자의 새 주문을 생성한다. 재고를 즉시 차감하고, 상태=결제완료 + 초기 trackingEvent 1건이 기록된다. " +
        "재고가 부족하면 IllegalArgumentException 을 던진다.")
    fun placeOrder(
        @ToolParam(description = "상품 ID (예: P-1001)") productId: String,
        @ToolParam(description = "사이즈 (예: M, L, 30)") size: String,
        @ToolParam(description = "주문 수량 (1~100)") quantity: Int,
    ): PlacedOrder = toolGuard.invoke(
        tool = "placeOrder",
        scope = "order:write",
        args = mapOf("productId" to productId, "size" to size, "quantity" to quantity),
    ) {
        val safeProductId = toolInputValidator.requireProductId(productId)
        val safeSize = toolInputValidator.requireSize(size)
        val safeQuantity = toolInputValidator.requirePositiveQuantity(quantity)

        val product = productRepository.findById(safeProductId)
            ?: throw NoSuchElementException("상품을 찾을 수 없습니다: $safeProductId")
        val onHand = product.stockOf(safeSize)
        require(onHand >= safeQuantity) {
            "재고가 부족합니다. 현재 ${onHand}개 (요청 ${safeQuantity}개)"
        }

        // 1) 재고 차감
        val newStock = product.stock.toMutableMap()
        newStock[safeSize] = onHand - safeQuantity
        productRepository.save(product.copy(stock = newStock))

        // 2) 주문 생성
        val now = Instant.now().toString()
        val newOrderId = orderRepository.nextOrderId()
        val newOrder = Order(
            orderId = newOrderId,
            productId = safeProductId,
            productName = product.name,
            size = safeSize,
            quantity = safeQuantity,
            status = "결제완료",
            orderedAt = now.substring(0, 10),
            trackingNo = null,
            trackingEvents = listOf(
                TrackingEvent(timestamp = now, status = "결제완료", location = "온라인 결제"),
            ),
        )
        orderRepository.save(newOrder)

        PlacedOrder(
            orderId = newOrderId,
            productId = safeProductId,
            productName = product.name,
            size = safeSize,
            quantity = safeQuantity,
            remainingStock = onHand - safeQuantity,
            status = "결제완료",
        )
    }

    @Tool(description = "주문을 취소한다. 이미 배송완료된 주문은 취소할 수 없다. " +
        "상태=취소 로 변경되고 취소 이벤트가 trackingEvents 끝에 추가된다.")
    fun cancelOrder(
        @ToolParam(description = "주문 번호 (예: ORD-1001)") orderId: String,
    ): CancelledOrder = toolGuard.invoke(
        tool = "cancelOrder",
        scope = "order:write",
        args = mapOf("orderId" to orderId),
    ) {
        val safeOrderId = toolInputValidator.requireOrderId(orderId)
        val order = orderRepository.findById(safeOrderId)
            ?: throw NoSuchElementException("주문을 찾을 수 없습니다: $safeOrderId")
        require(order.status != "배송완료") { "이미 배송완료된 주문은 취소할 수 없습니다." }
        require(order.status != "취소") { "이미 취소된 주문입니다." }

        val now = Instant.now().toString()
        val cancelled = order.copy(
            status = "취소",
            trackingEvents = order.trackingEvents + TrackingEvent(
                timestamp = now,
                status = "취소",
                location = "고객 요청",
            ),
        )
        orderRepository.save(cancelled)
        CancelledOrder(orderId = cancelled.orderId, status = cancelled.status, cancelledAt = now)
    }

    @Tool(description = "운영자가 주문의 배송 단계 이벤트를 추가한다. 동시에 주문의 currentStatus 도 새 status 로 갱신된다. " +
        "예: status='발송', location='성남 풀필먼트 센터'.")
    fun addTrackingEvent(
        @ToolParam(description = "주문 번호 (예: ORD-1001)") orderId: String,
        @ToolParam(description = "이벤트 상태 (예: 발송, 간선 운송, 배송중, 배송완료)") status: String,
        @ToolParam(description = "발생 위치 (예: 성남 풀필먼트 센터, 강남 영업소)") location: String,
    ): ShipmentTrack = toolGuard.invoke(
        tool = "addTrackingEvent",
        scope = "shipment:write",
        args = mapOf("orderId" to orderId, "status" to status, "location" to location),
    ) {
        val safeOrderId = toolInputValidator.requireOrderId(orderId)
        val safeStatus = toolInputValidator.requireEventStatus(status)
        val safeLocation = toolInputValidator.requireEventLocation(location)
        val order = orderRepository.findById(safeOrderId)
            ?: throw NoSuchElementException("주문을 찾을 수 없습니다: $safeOrderId")

        val now = Instant.now().toString()
        val event = TrackingEvent(timestamp = now, status = safeStatus, location = safeLocation)
        val updated = order.copy(
            status = safeStatus,
            trackingEvents = order.trackingEvents + event,
        )
        orderRepository.save(updated)
        ShipmentTrack(
            orderId = updated.orderId,
            currentStatus = updated.status,
            trackingNo = updated.trackingNo,
            events = updated.trackingEvents,
        )
    }

    @Tool(description = "관리자가 상품의 특정 사이즈 재고를 가감한다. delta 양수=입고, 음수=출고. " +
        "차감 시 보유 재고가 음수가 되면 IllegalArgumentException 을 던진다.")
    fun restockProduct(
        @ToolParam(description = "상품 ID (예: P-1001)") productId: String,
        @ToolParam(description = "사이즈 (예: M, L, 30)") size: String,
        @ToolParam(description = "변경량 (양수=입고, 음수=출고). 범위 -100 ~ +1000.") delta: Int,
    ): RestockResult = toolGuard.invoke(
        tool = "restockProduct",
        scope = "catalog:write",
        args = mapOf("productId" to productId, "size" to size, "delta" to delta),
    ) {
        val safeProductId = toolInputValidator.requireProductId(productId)
        val safeSize = toolInputValidator.requireSize(size)
        val safeDelta = toolInputValidator.requireRestockDelta(delta)

        val product = productRepository.findById(safeProductId)
            ?: throw NoSuchElementException("상품을 찾을 수 없습니다: $safeProductId")
        val before = product.stockOf(safeSize)
        val after = before + safeDelta
        require(after >= 0) { "차감 후 재고가 음수가 됩니다. 현재 ${before}개" }

        val newStock = product.stock.toMutableMap()
        newStock[safeSize] = after
        productRepository.save(product.copy(stock = newStock))

        RestockResult(productId = safeProductId, size = safeSize, before = before, after = after, delta = safeDelta)
    }
}

private fun com.biuea.springai.domain.Product.toSummary(): ProductSummary = ProductSummary(
    id = id,
    name = name,
    category = category,
    color = color,
    priceKrw = priceKrw,
    totalStock = totalStock(),
)

// ─── 응답 DTO ──────────────────────────────────────────────────────────

data class ProductSummary(
    val id: String,
    val name: String,
    val category: String,
    val color: String,
    val priceKrw: Int,
    val totalStock: Int,
)

data class ProductDetails(
    val id: String,
    val name: String,
    val category: String,
    val color: String,
    val material: String,
    val fit: String,
    val season: String,
    val priceKrw: Int,
    val description: String,
    val sizeStock: List<SizeStock>,
    val totalStock: Int,
)

data class SizeStock(
    val size: String,
    val quantity: Int,
)

data class CategoryColorCatalog(
    val categories: List<String>,
    val colors: List<String>,
)

data class InventoryResult(
    val productId: String,
    val size: String,
    val quantity: Int,
    val message: String,
)

data class OrderSummary(
    val orderId: String,
    val productName: String,
    val size: String,
    val quantity: Int,
    val status: String,
    val orderedAt: String,
)

data class OrderStatusResult(
    val orderId: String,
    val status: String,
    val productName: String?,
    val trackingNo: String?,
)

data class ShipmentTrack(
    val orderId: String,
    val currentStatus: String,
    val trackingNo: String?,
    val events: List<TrackingEvent>,
)

data class PlacedOrder(
    val orderId: String,
    val productId: String,
    val productName: String,
    val size: String,
    val quantity: Int,
    val remainingStock: Int,
    val status: String,
)

data class CancelledOrder(
    val orderId: String,
    val status: String,
    val cancelledAt: String,
)

data class RestockResult(
    val productId: String,
    val size: String,
    val before: Int,
    val after: Int,
    val delta: Int,
)
