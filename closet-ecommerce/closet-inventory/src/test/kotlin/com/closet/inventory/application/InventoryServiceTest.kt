package com.closet.inventory.application

import com.closet.common.exception.BusinessException
import com.closet.common.outbox.OutboxEvent
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.inventory.domain.ChangeType
import com.closet.inventory.domain.Inventory
import com.closet.inventory.domain.InventoryHistory
import com.closet.inventory.domain.InventoryHistoryRepository
import com.closet.inventory.domain.InventoryRepository
import com.closet.inventory.domain.RestockNotificationRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class InventoryServiceTest : BehaviorSpec({

    val inventoryRepository = mockk<InventoryRepository>()
    val inventoryHistoryRepository = mockk<InventoryHistoryRepository>()
    val restockNotificationRepository = mockk<RestockNotificationRepository>()
    val inventoryLockService = mockk<InventoryLockService>()
    val outboxEventPublisher = mockk<OutboxEventPublisher>(relaxed = true)
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val inventoryService = InventoryService(
        inventoryRepository = inventoryRepository,
        inventoryHistoryRepository = inventoryHistoryRepository,
        restockNotificationRepository = restockNotificationRepository,
        inventoryLockService = inventoryLockService,
        outboxEventPublisher = outboxEventPublisher,
        objectMapper = objectMapper,
    )

    // 분산 락 모킹: 즉시 block 실행
    beforeSpec {
        every { inventoryLockService.withLock(any(), any<() -> Any?>()) } answers {
            val block = secondArg<() -> Any?>()
            block()
        }
    }

    Given("재고 생성") {
        val request = CreateInventoryRequest(
            productId = 1L,
            productOptionId = 100L,
            sku = "SKU-001",
            totalQuantity = 50,
            safetyThreshold = 10,
        )

        When("신규 재고 생성") {
            every { inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(100L) } returns null

            val inventorySlot = slot<Inventory>()
            every { inventoryRepository.save(capture(inventorySlot)) } answers { inventorySlot.captured }

            val result = inventoryService.createInventory(request)

            Then("재고가 올바르게 생성된다") {
                result.productOptionId shouldBe 100L
                result.sku shouldBe "SKU-001"
                result.totalQuantity shouldBe 50
                result.availableQuantity shouldBe 50
                result.reservedQuantity shouldBe 0
                result.safetyThreshold shouldBe 10
            }
        }

        When("이미 존재하는 productOptionId로 생성 시도") {
            val existing = Inventory.create(
                productId = 1L,
                productOptionId = 100L,
                sku = "SKU-001",
                totalQuantity = 50,
            )
            every { inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(100L) } returns existing

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    inventoryService.createInventory(request)
                }
            }
        }
    }

    Given("All-or-Nothing 재고 예약") {
        When("모든 SKU 재고 충분 시") {
            val inventory1 = Inventory.create(1L, 100L, "SKU-A", 50)
            val inventory2 = Inventory.create(1L, 200L, "SKU-B", 30)

            every { inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(100L) } returns inventory1
            every { inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(200L) } returns inventory2
            every { inventoryRepository.save(any()) } answers { firstArg() }

            val historySlot = slot<InventoryHistory>()
            every { inventoryHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }

            val items = listOf(
                ReserveItemRequest(productOptionId = 100L, quantity = 5),
                ReserveItemRequest(productOptionId = 200L, quantity = 3),
            )

            val result = inventoryService.reserveAll(orderId = 1L, items = items)

            Then("전체 예약 성공") {
                result.success shouldBe true
            }

            Then("SKU-A의 available이 감소한다") {
                inventory1.availableQuantity shouldBe 45
                inventory1.reservedQuantity shouldBe 5
            }

            Then("SKU-B의 available이 감소한다") {
                inventory2.availableQuantity shouldBe 27
                inventory2.reservedQuantity shouldBe 3
            }

            Then("이력이 기록된다") {
                verify(atLeast = 2) { inventoryHistoryRepository.save(any()) }
            }
        }

        When("두 번째 SKU 재고 부족 시 (All-or-Nothing)") {
            val inventory1 = Inventory.create(1L, 100L, "SKU-A", 50)
            val inventory2 = Inventory.create(1L, 200L, "SKU-B", 2) // 부족

            every { inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(100L) } returns inventory1
            every { inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(200L) } returns inventory2
            every { inventoryRepository.save(any()) } answers { firstArg() }
            every { inventoryHistoryRepository.save(any()) } answers { firstArg() }
            every { outboxEventPublisher.publish(any(), any(), any(), any(), any(), any()) } returns mockk<OutboxEvent>()

            val items = listOf(
                ReserveItemRequest(productOptionId = 100L, quantity = 5),
                ReserveItemRequest(productOptionId = 200L, quantity = 10), // 재고 부족
            )

            val result = inventoryService.reserveAll(orderId = 2L, items = items)

            Then("예약 실패") {
                result.success shouldBe false
            }

            Then("부족 SKU 정보가 포함된다") {
                result.insufficientItems shouldNotBe emptyList<InventoryResult.InsufficientItemInfo>()
                result.insufficientItems[0].sku shouldBe "SKU-B"
            }

            Then("SKU-A는 보상 RELEASE 되어 원복된다") {
                inventory1.availableQuantity shouldBe 50
                inventory1.reservedQuantity shouldBe 0
            }

            Then("insufficient 이벤트가 발행된다") {
                verify { outboxEventPublisher.publish(any(), any(), eq("INSUFFICIENT"), eq("event.closet.inventory"), any(), any()) }
            }
        }
    }

    Given("재고 차감") {
        When("deductAll 실행") {
            val inventory = Inventory.create(1L, 100L, "SKU-A", 50)
            inventory.reserve(10)

            every { inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(100L) } returns inventory
            every { inventoryRepository.save(any()) } answers { firstArg() }
            every { inventoryHistoryRepository.save(any()) } answers { firstArg() }

            val items = listOf(DeductItemRequest(productOptionId = 100L, quantity = 10))
            inventoryService.deductAll(orderId = 1L, items = items)

            Then("reserved가 0, total이 40이 된다") {
                inventory.reservedQuantity shouldBe 0
                inventory.totalQuantity shouldBe 40
                inventory.availableQuantity shouldBe 40
            }
        }
    }

    Given("재고 해제") {
        When("releaseAll 실행") {
            val inventory = Inventory.create(1L, 100L, "SKU-A", 50)
            inventory.reserve(10)

            every { inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(100L) } returns inventory
            every { inventoryRepository.save(any()) } answers { firstArg() }
            every { inventoryHistoryRepository.save(any()) } answers { firstArg() }

            val items = listOf(ReleaseItemRequest(productOptionId = 100L, quantity = 10))
            inventoryService.releaseAll(orderId = 1L, items = items, reason = "주문 취소")

            Then("reserved가 0, available이 50으로 원복된다") {
                inventory.reservedQuantity shouldBe 0
                inventory.availableQuantity shouldBe 50
                inventory.totalQuantity shouldBe 50
            }
        }
    }

    Given("입고") {
        When("정상 입고") {
            val inventory = Inventory.create(1L, 100L, "SKU-A", 50)

            every { inventoryRepository.findByIdAndDeletedAtIsNull(any()) } returns inventory
            every { inventoryRepository.save(any()) } answers { firstArg() }
            every { inventoryHistoryRepository.save(any()) } answers { firstArg() }

            val request = InboundRequest(quantity = 30)
            val result = inventoryService.inbound(inventory.id, request)

            Then("total과 available이 증가한다") {
                result.totalQuantity shouldBe 80
                result.availableQuantity shouldBe 80
            }
        }

        When("존재하지 않는 inventoryId") {
            every { inventoryRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    inventoryService.inbound(999L, InboundRequest(quantity = 10))
                }
            }
        }
    }

    Given("이력 기록 검증") {
        When("reserve 시 before/after 스냅샷이 기록된다") {
            val inventory = Inventory.create(1L, 100L, "SKU-A", 50)

            every { inventoryRepository.findByProductOptionIdAndDeletedAtIsNull(100L) } returns inventory
            every { inventoryRepository.save(any()) } answers { firstArg() }

            val historySlot = slot<InventoryHistory>()
            every { inventoryHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }

            val items = listOf(ReserveItemRequest(productOptionId = 100L, quantity = 10))
            inventoryService.reserveAll(orderId = 1L, items = items)

            Then("이력의 changeType이 RESERVE이다") {
                historySlot.captured.changeType shouldBe ChangeType.RESERVE
            }

            Then("이력의 before/after가 올바르다") {
                historySlot.captured.beforeAvailable shouldBe 50
                historySlot.captured.afterAvailable shouldBe 40
                historySlot.captured.beforeReserved shouldBe 0
                historySlot.captured.afterReserved shouldBe 10
            }
        }
    }
})
