package com.closet.inventory.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.inventory.domain.InventoryItem
import com.closet.inventory.infrastructure.InventoryLockService
import com.closet.inventory.presentation.dto.InventoryResponse
import com.closet.inventory.repository.InventoryItemRepository
import com.closet.inventory.repository.InventoryTransactionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class InventoryService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryTransactionRepository: InventoryTransactionRepository,
    private val lockService: InventoryLockService,
) {

    /**
     * 재고 예약: 주문 시 가용 재고에서 예약 재고로 이동 (분산 락 적용)
     */
    @Transactional
    fun reserveStock(productOptionId: Long, quantity: Int, orderId: String): InventoryResponse {
        return lockService.withLock(productOptionId) {
            val item = findItemByProductOptionId(productOptionId)
            val transaction = item.reserve(quantity, orderId)

            inventoryItemRepository.save(item)
            inventoryTransactionRepository.save(transaction)

            logger.info { "재고 예약 완료: productOptionId=$productOptionId, quantity=$quantity, orderId=$orderId, available=${item.availableQuantity}" }

            if (item.isBelowSafetyThreshold()) {
                logger.warn { "안전재고 이하: productOptionId=$productOptionId, available=${item.availableQuantity}, threshold=${item.safetyThreshold}" }
            }

            InventoryResponse.from(item)
        }
    }

    /**
     * 예약 해제: 취소/반품 시 예약 재고를 가용 재고로 복원 (분산 락 적용)
     */
    @Transactional
    fun releaseStock(productOptionId: Long, quantity: Int, orderId: String): InventoryResponse {
        return lockService.withLock(productOptionId) {
            val item = findItemByProductOptionId(productOptionId)
            val transaction = item.release(quantity, orderId)

            inventoryItemRepository.save(item)
            inventoryTransactionRepository.save(transaction)

            logger.info { "재고 해제 완료: productOptionId=$productOptionId, quantity=$quantity, orderId=$orderId, available=${item.availableQuantity}" }

            InventoryResponse.from(item)
        }
    }

    /**
     * 재고 차감: 결제 확정 시 예약 재고에서 실물 차감 (분산 락 적용)
     */
    @Transactional
    fun deductStock(productOptionId: Long, quantity: Int, orderId: String): InventoryResponse {
        return lockService.withLock(productOptionId) {
            val item = findItemByProductOptionId(productOptionId)
            val transaction = item.deduct(quantity, orderId)

            inventoryItemRepository.save(item)
            inventoryTransactionRepository.save(transaction)

            logger.info { "재고 차감 완료: productOptionId=$productOptionId, quantity=$quantity, orderId=$orderId, total=${item.totalQuantity}" }

            InventoryResponse.from(item)
        }
    }

    /**
     * 입고: 실물 재고 증가 (분산 락 적용)
     */
    @Transactional
    fun restockItem(productOptionId: Long, quantity: Int): InventoryResponse {
        return lockService.withLock(productOptionId) {
            val item = inventoryItemRepository.findByProductOptionId(productOptionId)
                ?: run {
                    // 재고 항목이 없으면 신규 생성
                    val newItem = InventoryItem.create(
                        productOptionId = productOptionId,
                        totalQuantity = 0,
                    )
                    inventoryItemRepository.save(newItem)
                }

            val transaction = item.restock(quantity, "입고")

            inventoryItemRepository.save(item)
            inventoryTransactionRepository.save(transaction)

            logger.info { "입고 완료: productOptionId=$productOptionId, quantity=$quantity, total=${item.totalQuantity}, available=${item.availableQuantity}" }

            InventoryResponse.from(item)
        }
    }

    /**
     * 단건 재고 조회
     */
    fun getStock(productOptionId: Long): InventoryResponse {
        val item = findItemByProductOptionId(productOptionId)
        return InventoryResponse.from(item)
    }

    /**
     * 다건 재고 조회
     */
    fun bulkGetStock(productOptionIds: List<Long>): List<InventoryResponse> {
        val items = inventoryItemRepository.findByProductOptionIdIn(productOptionIds)
        return items.map { InventoryResponse.from(it) }
    }

    private fun findItemByProductOptionId(productOptionId: Long): InventoryItem {
        return inventoryItemRepository.findByProductOptionId(productOptionId)
            ?: throw BusinessException(
                ErrorCode.ENTITY_NOT_FOUND,
                "재고 항목을 찾을 수 없습니다. productOptionId=$productOptionId"
            )
    }
}
