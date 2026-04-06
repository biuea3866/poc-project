package com.closet.inventory.application.facade

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.inventory.application.InventoryResult
import com.closet.inventory.application.InventoryService
import com.closet.inventory.application.ReleaseItemRequest
import com.closet.inventory.application.ReserveItemRequest
import com.closet.inventory.consumer.event.OrderCancelledEvent
import com.closet.inventory.consumer.event.OrderCreatedEvent
import com.closet.inventory.consumer.event.ReturnApprovedEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class InventoryFacadeTest : BehaviorSpec({

    val inventoryService = mockk<InventoryService>(relaxed = true)
    val idempotencyChecker = mockk<IdempotencyChecker>()

    val facade =
        InventoryFacade(
            inventoryService = inventoryService,
            idempotencyChecker = idempotencyChecker,
        )

    // IdempotencyChecker 모킹: 항상 block 실행
    beforeSpec {
        every { idempotencyChecker.process(any(), any(), any(), any<() -> Any?>()) } answers {
            val block = lastArg<() -> Any?>()
            block()
        }
    }

    Given("OrderCreatedEvent 수신") {
        val event =
            OrderCreatedEvent(
                orderId = 1L,
                memberId = 100L,
                items =
                    listOf(
                        OrderCreatedEvent.OrderItemInfo(productOptionId = 10L, quantity = 3),
                        OrderCreatedEvent.OrderItemInfo(productOptionId = 20L, quantity = 2),
                    ),
            )

        When("handleOrderCreated 호출") {
            every { inventoryService.reserveAll(any(), any()) } returns InventoryResult.success()

            facade.handleOrderCreated(event)

            Then("InventoryService.reserveAll이 호출된다") {
                val itemsSlot = slot<List<ReserveItemRequest>>()
                verify {
                    inventoryService.reserveAll(eq(1L), capture(itemsSlot))
                }
                itemsSlot.captured.size shouldBe 2
                itemsSlot.captured[0].productOptionId shouldBe 10L
                itemsSlot.captured[0].quantity shouldBe 3
                itemsSlot.captured[1].productOptionId shouldBe 20L
                itemsSlot.captured[1].quantity shouldBe 2
            }

            Then("멱등성 체크가 event.closet.order 토픽으로 수행된다") {
                verify {
                    idempotencyChecker.process(
                        eq("order-created-1"),
                        eq("event.closet.order"),
                        eq("inventory-service"),
                        any<() -> Any?>(),
                    )
                }
            }
        }
    }

    Given("OrderCancelledEvent 수신") {
        val event =
            OrderCancelledEvent(
                orderId = 2L,
                reason = "고객 취소",
                items =
                    listOf(
                        OrderCancelledEvent.OrderItemInfo(productOptionId = 10L, quantity = 3),
                    ),
            )

        When("handleOrderCancelled 호출") {
            every { idempotencyChecker.process(any(), any(), any(), any<() -> Any?>()) } answers {
                val block = lastArg<() -> Any?>()
                block()
            }

            facade.handleOrderCancelled(event)

            Then("InventoryService.releaseAll이 호출된다") {
                val itemsSlot = slot<List<ReleaseItemRequest>>()
                verify {
                    inventoryService.releaseAll(eq(2L), capture(itemsSlot), eq("고객 취소"))
                }
                itemsSlot.captured.size shouldBe 1
                itemsSlot.captured[0].productOptionId shouldBe 10L
                itemsSlot.captured[0].quantity shouldBe 3
            }
        }
    }

    Given("ReturnApprovedEvent 수신") {
        val event =
            ReturnApprovedEvent(
                orderId = 3L,
                items =
                    listOf(
                        ReturnApprovedEvent.ReturnItemInfo(productOptionId = 30L, quantity = 1),
                    ),
            )

        When("handleReturnApproved 호출") {
            every { idempotencyChecker.process(any(), any(), any(), any<() -> Any?>()) } answers {
                val block = lastArg<() -> Any?>()
                block()
            }

            facade.handleReturnApproved(event)

            Then("InventoryService.returnRestore가 호출된다") {
                verify {
                    inventoryService.returnRestore(
                        productOptionId = eq(30L),
                        quantity = eq(1),
                        orderId = eq(3L),
                    )
                }
            }

            Then("멱등성 체크가 event.closet.shipping 토픽으로 수행된다") {
                verify {
                    idempotencyChecker.process(
                        eq("return-approved-3"),
                        eq("event.closet.shipping"),
                        eq("inventory-service"),
                        any<() -> Any?>(),
                    )
                }
            }
        }
    }
})
