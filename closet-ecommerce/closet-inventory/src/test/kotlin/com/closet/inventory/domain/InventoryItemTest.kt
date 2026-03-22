package com.closet.inventory.domain

import com.closet.common.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class InventoryItemTest : BehaviorSpec({

    Given("재고 예약 (reserve)") {
        When("가용 재고가 충분한 경우") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 100)

            val transaction = item.reserve(10, "ORDER-001")

            Then("가용 재고가 감소하고 예약 재고가 증가한다") {
                item.availableQuantity shouldBe 90
                item.reservedQuantity shouldBe 10
                item.totalQuantity shouldBe 100
            }

            Then("RESERVE 타입 트랜잭션이 생성된다") {
                transaction.type shouldBe TransactionType.RESERVE
                transaction.quantity shouldBe 10
                transaction.referenceId shouldBe "ORDER-001"
            }
        }

        When("가용 재고가 부족한 경우") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 5)

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    item.reserve(10, "ORDER-002")
                }
            }
        }

        When("수량이 0 이하인 경우") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 100)

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    item.reserve(0, "ORDER-003")
                }
            }
        }
    }

    Given("예약 해제 (release)") {
        When("예약된 재고가 충분한 경우") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 100)
            item.reserve(20, "ORDER-001")

            val transaction = item.release(10, "ORDER-001")

            Then("예약 재고가 감소하고 가용 재고가 증가한다") {
                item.availableQuantity shouldBe 90
                item.reservedQuantity shouldBe 10
                item.totalQuantity shouldBe 100
            }

            Then("RELEASE 타입 트랜잭션이 생성된다") {
                transaction.type shouldBe TransactionType.RELEASE
                transaction.quantity shouldBe 10
            }
        }

        When("예약된 재고보다 많은 수량을 해제하려는 경우") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 100)
            item.reserve(5, "ORDER-001")

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    item.release(10, "ORDER-001")
                }
            }
        }
    }

    Given("재고 차감 (deduct)") {
        When("예약된 재고에서 차감하는 경우") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 100)
            item.reserve(20, "ORDER-001")

            val transaction = item.deduct(20, "ORDER-001")

            Then("예약 재고와 총 재고가 감소한다") {
                item.totalQuantity shouldBe 80
                item.reservedQuantity shouldBe 0
                item.availableQuantity shouldBe 80
            }

            Then("OUTBOUND 타입 트랜잭션이 생성된다") {
                transaction.type shouldBe TransactionType.OUTBOUND
                transaction.quantity shouldBe 20
            }
        }
    }

    Given("입고 (restock)") {
        When("재고를 입고하는 경우") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 50)

            val transaction = item.restock(30, "정기 입고")

            Then("총 재고와 가용 재고가 증가한다") {
                item.totalQuantity shouldBe 80
                item.availableQuantity shouldBe 80
            }

            Then("INBOUND 타입 트랜잭션이 생성된다") {
                transaction.type shouldBe TransactionType.INBOUND
                transaction.quantity shouldBe 30
            }
        }
    }

    Given("안전재고 확인") {
        When("가용 재고가 안전재고 이하인 경우") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 10, safetyThreshold = 10)

            Then("isBelowSafetyThreshold()가 true를 반환한다") {
                item.isBelowSafetyThreshold() shouldBe true
            }
        }

        When("가용 재고가 안전재고 초과인 경우") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 100, safetyThreshold = 10)

            Then("isBelowSafetyThreshold()가 false를 반환한다") {
                item.isBelowSafetyThreshold() shouldBe false
            }
        }
    }

    Given("reserve + release 연속 동작") {
        When("전체 수량 예약 후 전체 해제") {
            val item = InventoryItem.create(productOptionId = 1L, totalQuantity = 50)
            item.reserve(50, "ORDER-001")
            item.release(50, "ORDER-001")

            Then("원래 상태로 복원된다") {
                item.totalQuantity shouldBe 50
                item.availableQuantity shouldBe 50
                item.reservedQuantity shouldBe 0
            }
        }
    }
})
