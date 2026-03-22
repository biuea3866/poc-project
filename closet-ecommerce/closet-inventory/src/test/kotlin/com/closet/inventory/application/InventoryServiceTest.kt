package com.closet.inventory.application

import com.closet.common.exception.BusinessException
import com.closet.inventory.domain.InventoryItem
import com.closet.inventory.domain.InventoryTransaction
import com.closet.inventory.domain.TransactionType
import com.closet.inventory.infrastructure.InventoryLockService
import com.closet.inventory.repository.InventoryItemRepository
import com.closet.inventory.repository.InventoryTransactionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class InventoryServiceTest : BehaviorSpec({

    val inventoryItemRepository = mockk<InventoryItemRepository>()
    val inventoryTransactionRepository = mockk<InventoryTransactionRepository>()
    val lockService = mockk<InventoryLockService>()

    val inventoryService = InventoryService(
        inventoryItemRepository = inventoryItemRepository,
        inventoryTransactionRepository = inventoryTransactionRepository,
        lockService = lockService,
    )

    // lockService.withLock 모킹: 락 획득을 바이패스하고 action을 즉시 실행
    beforeSpec {
        every { lockService.withLock<Any>(any(), any()) } answers {
            val action = secondArg<() -> Any>()
            action()
        }
    }

    Given("재고 예약 (reserveStock)") {
        val item = InventoryItem.create(productOptionId = 1000L, totalQuantity = 100)

        every { inventoryItemRepository.findByProductOptionId(1000L) } returns item
        every { inventoryItemRepository.save(any()) } answers { firstArg() }
        every { inventoryTransactionRepository.save(any()) } answers { firstArg() }

        When("충분한 재고로 예약 요청") {
            val result = inventoryService.reserveStock(
                productOptionId = 1000L,
                quantity = 10,
                orderId = "ORDER-001",
            )

            Then("가용 재고가 감소하고 예약 재고가 증가한다") {
                result.availableQuantity shouldBe 90
                result.reservedQuantity shouldBe 10
                result.totalQuantity shouldBe 100
            }

            Then("분산 락이 사용된다") {
                verify { lockService.withLock<Any>(1000L, any()) }
            }

            Then("트랜잭션이 저장된다") {
                verify { inventoryTransactionRepository.save(any()) }
            }
        }
    }

    Given("재고 부족 시 예약 실패") {
        val item = InventoryItem.create(productOptionId = 2000L, totalQuantity = 5)

        every { inventoryItemRepository.findByProductOptionId(2000L) } returns item

        When("재고보다 많은 수량 예약 요청") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    inventoryService.reserveStock(
                        productOptionId = 2000L,
                        quantity = 10,
                        orderId = "ORDER-002",
                    )
                }
            }
        }
    }

    Given("예약 해제 (releaseStock)") {
        val item = InventoryItem.create(productOptionId = 3000L, totalQuantity = 100)
        item.reserve(20, "ORDER-003")

        every { inventoryItemRepository.findByProductOptionId(3000L) } returns item
        every { inventoryItemRepository.save(any()) } answers { firstArg() }
        every { inventoryTransactionRepository.save(any()) } answers { firstArg() }

        When("예약된 재고 해제 요청") {
            val result = inventoryService.releaseStock(
                productOptionId = 3000L,
                quantity = 20,
                orderId = "ORDER-003",
            )

            Then("예약 재고가 0이 되고 가용 재고가 복원된다") {
                result.reservedQuantity shouldBe 0
                result.availableQuantity shouldBe 100
            }
        }
    }

    Given("재고 차감 (deductStock)") {
        val item = InventoryItem.create(productOptionId = 4000L, totalQuantity = 100)
        item.reserve(30, "ORDER-004")

        every { inventoryItemRepository.findByProductOptionId(4000L) } returns item
        every { inventoryItemRepository.save(any()) } answers { firstArg() }
        every { inventoryTransactionRepository.save(any()) } answers { firstArg() }

        When("결제 확정에 의한 재고 차감 요청") {
            val result = inventoryService.deductStock(
                productOptionId = 4000L,
                quantity = 30,
                orderId = "ORDER-004",
            )

            Then("총 재고가 감소하고 예약 재고가 해소된다") {
                result.totalQuantity shouldBe 70
                result.reservedQuantity shouldBe 0
                result.availableQuantity shouldBe 70
            }
        }
    }

    Given("입고 (restockItem)") {
        val item = InventoryItem.create(productOptionId = 5000L, totalQuantity = 30)

        every { inventoryItemRepository.findByProductOptionId(5000L) } returns item
        every { inventoryItemRepository.save(any()) } answers { firstArg() }
        every { inventoryTransactionRepository.save(any()) } answers { firstArg() }

        When("입고 요청") {
            val result = inventoryService.restockItem(
                productOptionId = 5000L,
                quantity = 20,
            )

            Then("총 재고와 가용 재고가 증가한다") {
                result.totalQuantity shouldBe 50
                result.availableQuantity shouldBe 50
            }
        }
    }

    Given("존재하지 않는 재고 조회") {
        every { inventoryItemRepository.findByProductOptionId(9999L) } returns null

        When("존재하지 않는 productOptionId로 조회") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    inventoryService.getStock(9999L)
                }
            }
        }
    }

    Given("다건 재고 조회 (bulkGetStock)") {
        val items = listOf(
            InventoryItem.create(productOptionId = 100L, totalQuantity = 50),
            InventoryItem.create(productOptionId = 200L, totalQuantity = 30),
            InventoryItem.create(productOptionId = 300L, totalQuantity = 0),
        )

        every { inventoryItemRepository.findByProductOptionIdIn(listOf(100L, 200L, 300L)) } returns items

        When("여러 productOptionId로 조회") {
            val result = inventoryService.bulkGetStock(listOf(100L, 200L, 300L))

            Then("조회된 항목 수가 일치한다") {
                result.size shouldBe 3
            }

            Then("각 항목의 재고가 올바르다") {
                result[0].availableQuantity shouldBe 50
                result[1].availableQuantity shouldBe 30
                result[2].availableQuantity shouldBe 0
            }
        }
    }
})
