package com.closet.inventory.application

import com.closet.common.event.ClosetTopics
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.inventory.domain.ChangeType
import com.closet.inventory.domain.InsufficientStockException
import com.closet.inventory.domain.Inventory
import com.closet.inventory.domain.InventoryHistory
import com.closet.inventory.domain.InventoryHistoryRepository
import com.closet.inventory.domain.InventoryRepository
import com.closet.inventory.domain.RestockNotificationRepository
import com.closet.inventory.domain.RestockNotificationStatus
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val inventoryHistoryRepository: InventoryHistoryRepository,
    private val restockNotificationRepository: RestockNotificationRepository,
    private val inventoryLockService: InventoryLockService,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    /**
     * 재고 생성.
     */
    @Transactional
    fun createInventory(request: CreateInventoryRequest): InventoryResponse {
        val existing = inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(request.productOptionId)
        if (existing != null) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 재고가 존재합니다. productOptionId=${request.productOptionId}")
        }

        val inventory =
            Inventory.create(
                productId = request.productId,
                productOptionId = request.productOptionId,
                sku = request.sku,
                totalQuantity = request.totalQuantity,
                safetyThreshold = request.safetyThreshold,
            )

        val saved = inventoryRepository.save(inventory)
        logger.info { "재고 생성 완료: id=${saved.id}, sku=${saved.sku}, total=${saved.totalQuantity}" }
        return InventoryResponse.from(saved)
    }

    /**
     * 재고 조회 (by ID).
     */
    fun findById(id: Long): InventoryResponse {
        val inventory = getInventoryOrThrow(id)
        return InventoryResponse.from(inventory)
    }

    /**
     * 재고 조회 (by productOptionId).
     */
    fun findByProductOptionId(productOptionId: Long): InventoryResponse {
        val inventory =
            inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(productOptionId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "재고를 찾을 수 없습니다. productOptionId=$productOptionId")
        return InventoryResponse.from(inventory)
    }

    /**
     * 상품별 재고 목록 조회.
     */
    fun findByProductId(productId: Long): List<InventoryResponse> {
        return inventoryRepository.findByProductIdAndDeletedAtIsNull(productId)
            .map { InventoryResponse.from(it) }
    }

    /**
     * All-or-Nothing 재고 예약 (주문 생성 시).
     * 여러 SKU 주문 시 하나라도 부족하면 전체 RELEASE.
     */
    @Transactional
    fun reserveAll(
        orderId: Long,
        items: List<ReserveItemRequest>,
    ): InventoryResult {
        val reserved = mutableListOf<Pair<Long, Int>>() // productOptionId, quantity

        try {
            for (item in items) {
                inventoryLockService.withLock(item.productOptionId) {
                    val inventory =
                        inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(item.productOptionId)
                            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "재고를 찾을 수 없습니다. productOptionId=${item.productOptionId}")

                    val beforeTotal = inventory.totalQuantity
                    val beforeAvailable = inventory.availableQuantity
                    val beforeReserved = inventory.reservedQuantity

                    inventory.reserve(item.quantity)
                    inventoryRepository.save(inventory)

                    inventoryHistoryRepository.save(
                        InventoryHistory.create(
                            inventoryId = inventory.id,
                            changeType = ChangeType.RESERVE,
                            quantity = item.quantity,
                            beforeTotal = beforeTotal,
                            afterTotal = inventory.totalQuantity,
                            beforeAvailable = beforeAvailable,
                            afterAvailable = inventory.availableQuantity,
                            beforeReserved = beforeReserved,
                            afterReserved = inventory.reservedQuantity,
                            referenceId = orderId.toString(),
                            referenceType = "ORDER",
                        ),
                    )

                    reserved.add(item.productOptionId to item.quantity)

                    // 안전재고 이하 이벤트
                    if (inventory.isBelowSafetyThreshold()) {
                        publishLowStockEvent(inventory)
                    }

                    // 품절 이벤트
                    if (inventory.isOutOfStock()) {
                        publishOutOfStockEvent(inventory)
                    }
                }
            }

            logger.info { "재고 예약 완료: orderId=$orderId, items=${items.size}" }
            return InventoryResult.success()
        } catch (e: InsufficientStockException) {
            logger.warn { "재고 부족 발생: orderId=$orderId, sku=${e.sku}, requested=${e.requested}, available=${e.available}" }

            // 이미 예약한 건 전체 RELEASE (보상 트랜잭션)
            releaseReserved(reserved, orderId)

            // insufficient 이벤트 발행
            publishInsufficientEvent(orderId, e)

            return InventoryResult.insufficient(
                listOf(
                    InventoryResult.InsufficientItemInfo(
                        productOptionId = e.productOptionId,
                        sku = e.sku,
                        requested = e.requested,
                        available = e.available,
                    ),
                ),
            )
        }
    }

    /**
     * 재고 차감 (결제 완료 시).
     */
    @Transactional
    fun deductAll(
        orderId: Long,
        items: List<DeductItemRequest>,
    ) {
        for (item in items) {
            inventoryLockService.withLock(item.productOptionId) {
                val inventory =
                    inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(item.productOptionId)
                        ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "재고를 찾을 수 없습니다. productOptionId=${item.productOptionId}")

                val beforeTotal = inventory.totalQuantity
                val beforeAvailable = inventory.availableQuantity
                val beforeReserved = inventory.reservedQuantity

                inventory.deduct(item.quantity)
                inventoryRepository.save(inventory)

                inventoryHistoryRepository.save(
                    InventoryHistory.create(
                        inventoryId = inventory.id,
                        changeType = ChangeType.DEDUCT,
                        quantity = item.quantity,
                        beforeTotal = beforeTotal,
                        afterTotal = inventory.totalQuantity,
                        beforeAvailable = beforeAvailable,
                        afterAvailable = inventory.availableQuantity,
                        beforeReserved = beforeReserved,
                        afterReserved = inventory.reservedQuantity,
                        referenceId = orderId.toString(),
                        referenceType = "ORDER",
                    ),
                )
            }
        }

        logger.info { "재고 차감 완료: orderId=$orderId, items=${items.size}" }
    }

    /**
     * 재고 해제 (주문 취소 시).
     */
    @Transactional
    fun releaseAll(
        orderId: Long,
        items: List<ReleaseItemRequest>,
        reason: String? = null,
    ) {
        for (item in items) {
            inventoryLockService.withLock(item.productOptionId) {
                val inventory =
                    inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(item.productOptionId)
                        ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "재고를 찾을 수 없습니다. productOptionId=${item.productOptionId}")

                val beforeTotal = inventory.totalQuantity
                val beforeAvailable = inventory.availableQuantity
                val beforeReserved = inventory.reservedQuantity

                inventory.release(item.quantity)
                inventoryRepository.save(inventory)

                inventoryHistoryRepository.save(
                    InventoryHistory.create(
                        inventoryId = inventory.id,
                        changeType = ChangeType.RELEASE,
                        quantity = item.quantity,
                        beforeTotal = beforeTotal,
                        afterTotal = inventory.totalQuantity,
                        beforeAvailable = beforeAvailable,
                        afterAvailable = inventory.availableQuantity,
                        beforeReserved = beforeReserved,
                        afterReserved = inventory.reservedQuantity,
                        referenceId = orderId.toString(),
                        referenceType = "ORDER",
                        reason = reason,
                    ),
                )
            }
        }

        logger.info { "재고 해제 완료: orderId=$orderId, items=${items.size}" }
    }

    /**
     * 입고.
     */
    @Transactional
    fun inbound(
        id: Long,
        request: InboundRequest,
    ): InventoryResponse {
        return inventoryLockService.withLock(getInventoryOrThrow(id).productOptionId) {
            val inventory = getInventoryOrThrow(id)

            val beforeTotal = inventory.totalQuantity
            val beforeAvailable = inventory.availableQuantity
            val beforeReserved = inventory.reservedQuantity

            val previousAvailable = inventory.inbound(request.quantity)
            inventoryRepository.save(inventory)

            inventoryHistoryRepository.save(
                InventoryHistory.create(
                    inventoryId = inventory.id,
                    changeType = ChangeType.INBOUND,
                    quantity = request.quantity,
                    beforeTotal = beforeTotal,
                    afterTotal = inventory.totalQuantity,
                    beforeAvailable = beforeAvailable,
                    afterAvailable = inventory.availableQuantity,
                    beforeReserved = beforeReserved,
                    afterReserved = inventory.reservedQuantity,
                    reason = request.reason,
                ),
            )

            // available 0 -> 양수 전환 시 재입고 알림 이벤트
            if (previousAvailable == 0 && inventory.availableQuantity > 0) {
                triggerRestockNotification(inventory)
            }

            logger.info { "입고 완료: inventoryId=${inventory.id}, sku=${inventory.sku}, quantity=${request.quantity}" }
            InventoryResponse.from(inventory)
        }
    }

    /**
     * 반품 양품 복구.
     */
    @Transactional
    fun returnRestore(
        productOptionId: Long,
        quantity: Int,
        orderId: Long,
    ) {
        inventoryLockService.withLock(productOptionId) {
            val inventory =
                inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(productOptionId)
                    ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "재고를 찾을 수 없습니다. productOptionId=$productOptionId")

            val beforeTotal = inventory.totalQuantity
            val beforeAvailable = inventory.availableQuantity
            val beforeReserved = inventory.reservedQuantity

            val previousAvailable = inventory.returnRestore(quantity)
            inventoryRepository.save(inventory)

            inventoryHistoryRepository.save(
                InventoryHistory.create(
                    inventoryId = inventory.id,
                    changeType = ChangeType.RETURN_RESTORE,
                    quantity = quantity,
                    beforeTotal = beforeTotal,
                    afterTotal = inventory.totalQuantity,
                    beforeAvailable = beforeAvailable,
                    afterAvailable = inventory.availableQuantity,
                    beforeReserved = beforeReserved,
                    afterReserved = inventory.reservedQuantity,
                    referenceId = orderId.toString(),
                    referenceType = "RETURN",
                ),
            )

            // available 0 -> 양수 전환 시 재입고 알림 이벤트
            if (previousAvailable == 0 && inventory.availableQuantity > 0) {
                triggerRestockNotification(inventory)
            }

            logger.info { "반품 양품 복구 완료: productOptionId=$productOptionId, quantity=$quantity" }
        }
    }

    private fun getInventoryOrThrow(id: Long): Inventory {
        return inventoryRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "재고를 찾을 수 없습니다. id=$id")
    }

    /**
     * 보상 RELEASE: All-or-Nothing 실패 시 이미 예약한 건 원복.
     */
    private fun releaseReserved(
        reserved: List<Pair<Long, Int>>,
        orderId: Long,
    ) {
        for ((productOptionId, quantity) in reserved) {
            try {
                inventoryLockService.withLock(productOptionId) {
                    val inventory = inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(productOptionId) ?: return@withLock

                    val beforeTotal = inventory.totalQuantity
                    val beforeAvailable = inventory.availableQuantity
                    val beforeReserved = inventory.reservedQuantity

                    inventory.release(quantity)
                    inventoryRepository.save(inventory)

                    inventoryHistoryRepository.save(
                        InventoryHistory.create(
                            inventoryId = inventory.id,
                            changeType = ChangeType.RELEASE,
                            quantity = quantity,
                            beforeTotal = beforeTotal,
                            afterTotal = inventory.totalQuantity,
                            beforeAvailable = beforeAvailable,
                            afterAvailable = inventory.availableQuantity,
                            beforeReserved = beforeReserved,
                            afterReserved = inventory.reservedQuantity,
                            referenceId = orderId.toString(),
                            referenceType = "ORDER",
                            reason = "All-or-Nothing 보상 RELEASE",
                        ),
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "보상 RELEASE 실패: productOptionId=$productOptionId, orderId=$orderId" }
            }
        }
    }

    private fun triggerRestockNotification(inventory: Inventory) {
        val waitingNotifications =
            restockNotificationRepository.findByProductOptionIdAndStatus(
                productOptionId = inventory.productOptionId,
                status = RestockNotificationStatus.WAITING,
            )

        if (waitingNotifications.isEmpty()) return

        val memberIds = waitingNotifications.map { it.memberId }

        waitingNotifications.forEach { it.markNotified() }

        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "productOptionId" to inventory.productOptionId,
                    "sku" to inventory.sku,
                    "availableQuantity" to inventory.availableQuantity,
                    "memberIds" to memberIds,
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "Inventory",
            aggregateId = inventory.id.toString(),
            eventType = "RESTOCK_NOTIFICATION",
            topic = ClosetTopics.INVENTORY,
            partitionKey = inventory.productOptionId.toString(),
            payload = payload,
        )

        logger.info { "재입고 알림 이벤트 발행: productOptionId=${inventory.productOptionId}, memberIds=$memberIds" }
    }

    private fun publishLowStockEvent(inventory: Inventory) {
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "productOptionId" to inventory.productOptionId,
                    "sku" to inventory.sku,
                    "availableQuantity" to inventory.availableQuantity,
                    "safetyThreshold" to inventory.safetyThreshold,
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "Inventory",
            aggregateId = inventory.id.toString(),
            eventType = "LOW_STOCK",
            topic = ClosetTopics.INVENTORY,
            partitionKey = inventory.productOptionId.toString(),
            payload = payload,
        )

        logger.info { "안전재고 이하 이벤트 발행: sku=${inventory.sku}, available=${inventory.availableQuantity}" }
    }

    private fun publishOutOfStockEvent(inventory: Inventory) {
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "productOptionId" to inventory.productOptionId,
                    "sku" to inventory.sku,
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "Inventory",
            aggregateId = inventory.id.toString(),
            eventType = "OUT_OF_STOCK",
            topic = ClosetTopics.INVENTORY,
            partitionKey = inventory.productOptionId.toString(),
            payload = payload,
        )

        logger.info { "품절 이벤트 발행: sku=${inventory.sku}" }
    }

    private fun publishInsufficientEvent(
        orderId: Long,
        e: InsufficientStockException,
    ) {
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "eventId" to "insufficient-$orderId-${System.currentTimeMillis()}",
                    "orderId" to orderId,
                    "reason" to "재고 부족: sku=${e.sku}, requested=${e.requested}, available=${e.available}",
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "Inventory",
            aggregateId = orderId.toString(),
            eventType = "INSUFFICIENT",
            topic = ClosetTopics.INVENTORY,
            partitionKey = orderId.toString(),
            payload = payload,
        )

        logger.info { "재고 부족 이벤트 발행: orderId=$orderId, sku=${e.sku}" }
    }
}
