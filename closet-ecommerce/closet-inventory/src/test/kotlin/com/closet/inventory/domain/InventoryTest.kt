package com.closet.inventory.domain

import com.closet.common.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class InventoryTest : BehaviorSpec({

    Given("재고가 total=100, available=100, reserved=0 인 상태") {
        fun createInventory() =
            Inventory.create(
                productId = 1L,
                productOptionId = 100L,
                sku = "SKU-001",
                totalQuantity = 100,
                safetyThreshold = 10,
            )

        When("reserve(10) 실행") {
            val inventory = createInventory()
            inventory.reserve(10)

            Then("available이 90, reserved가 10이 된다") {
                inventory.totalQuantity shouldBe 100
                inventory.availableQuantity shouldBe 90
                inventory.reservedQuantity shouldBe 10
            }
        }

        When("reserve(100) 실행 (전량 예약)") {
            val inventory = createInventory()
            inventory.reserve(100)

            Then("available이 0, reserved가 100이 된다") {
                inventory.totalQuantity shouldBe 100
                inventory.availableQuantity shouldBe 0
                inventory.reservedQuantity shouldBe 100
            }
        }

        When("reserve(101) 실행 (재고 부족)") {
            val inventory = createInventory()

            Then("InsufficientStockException이 발생한다") {
                shouldThrow<InsufficientStockException> {
                    inventory.reserve(101)
                }
            }

            Then("재고 수량이 변경되지 않는다") {
                try {
                    inventory.reserve(101)
                } catch (_: InsufficientStockException) {
                }
                inventory.totalQuantity shouldBe 100
                inventory.availableQuantity shouldBe 100
                inventory.reservedQuantity shouldBe 0
            }
        }
    }

    Given("재고가 total=100, available=90, reserved=10 인 상태") {
        fun createReservedInventory(): Inventory {
            val inventory =
                Inventory.create(
                    productId = 1L,
                    productOptionId = 100L,
                    sku = "SKU-001",
                    totalQuantity = 100,
                )
            inventory.reserve(10)
            return inventory
        }

        When("deduct(10) 실행") {
            val inventory = createReservedInventory()
            inventory.deduct(10)

            Then("reserved가 0이 되고 total이 90이 된다") {
                inventory.totalQuantity shouldBe 90
                inventory.availableQuantity shouldBe 90
                inventory.reservedQuantity shouldBe 0
            }
        }

        When("deduct(5) 실행 (부분 차감)") {
            val inventory = createReservedInventory()
            inventory.deduct(5)

            Then("reserved가 5, total이 95가 된다") {
                inventory.totalQuantity shouldBe 95
                inventory.availableQuantity shouldBe 90
                inventory.reservedQuantity shouldBe 5
            }
        }

        When("deduct(11) 실행 (reserved 초과)") {
            val inventory = createReservedInventory()

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    inventory.deduct(11)
                }
            }
        }
    }

    Given("재고가 total=100, available=90, reserved=10 이고 release 시") {
        fun createReservedInventory(): Inventory {
            val inventory =
                Inventory.create(
                    productId = 1L,
                    productOptionId = 100L,
                    sku = "SKU-001",
                    totalQuantity = 100,
                )
            inventory.reserve(10)
            return inventory
        }

        When("release(10) 실행") {
            val inventory = createReservedInventory()
            inventory.release(10)

            Then("reserved가 0, available이 100이 된다 (원복)") {
                inventory.totalQuantity shouldBe 100
                inventory.availableQuantity shouldBe 100
                inventory.reservedQuantity shouldBe 0
            }
        }

        When("release(5) 실행 (부분 해제)") {
            val inventory = createReservedInventory()
            inventory.release(5)

            Then("reserved가 5, available이 95가 된다") {
                inventory.totalQuantity shouldBe 100
                inventory.availableQuantity shouldBe 95
                inventory.reservedQuantity shouldBe 5
            }
        }

        When("release(11) 실행 (reserved 초과)") {
            val inventory = createReservedInventory()

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    inventory.release(11)
                }
            }
        }
    }

    Given("재고 입고") {
        When("inbound(50) 실행") {
            val inventory =
                Inventory.create(
                    productId = 1L,
                    productOptionId = 100L,
                    sku = "SKU-001",
                    totalQuantity = 100,
                )
            val previousAvailable = inventory.inbound(50)

            Then("total이 150, available이 150이 된다") {
                inventory.totalQuantity shouldBe 150
                inventory.availableQuantity shouldBe 150
                inventory.reservedQuantity shouldBe 0
            }

            Then("이전 available이 100이다") {
                previousAvailable shouldBe 100
            }
        }

        When("available이 0인 상태에서 inbound(10) 실행") {
            val inventory =
                Inventory.create(
                    productId = 1L,
                    productOptionId = 100L,
                    sku = "SKU-001",
                    totalQuantity = 10,
                )
            inventory.reserve(10) // available = 0
            val previousAvailable = inventory.inbound(10)

            Then("total이 20, available이 10이 된다") {
                inventory.totalQuantity shouldBe 20
                inventory.availableQuantity shouldBe 10
                inventory.reservedQuantity shouldBe 10
            }

            Then("이전 available이 0이다 (재입고 알림 트리거 조건)") {
                previousAvailable shouldBe 0
            }
        }
    }

    Given("반품 양품 복구") {
        When("returnRestore(5) 실행") {
            val inventory =
                Inventory.create(
                    productId = 1L,
                    productOptionId = 100L,
                    sku = "SKU-001",
                    totalQuantity = 90,
                )
            val previousAvailable = inventory.returnRestore(5)

            Then("total이 95, available이 95가 된다") {
                inventory.totalQuantity shouldBe 95
                inventory.availableQuantity shouldBe 95
                inventory.reservedQuantity shouldBe 0
            }

            Then("이전 available이 90이다") {
                previousAvailable shouldBe 90
            }
        }
    }

    Given("안전재고 확인") {
        When("available이 safetyThreshold 이하") {
            val inventory =
                Inventory.create(
                    productId = 1L,
                    productOptionId = 100L,
                    sku = "SKU-001",
                    totalQuantity = 10,
                    safetyThreshold = 10,
                )

            Then("isBelowSafetyThreshold가 true") {
                inventory.isBelowSafetyThreshold() shouldBe true
            }
        }

        When("available이 safetyThreshold 초과") {
            val inventory =
                Inventory.create(
                    productId = 1L,
                    productOptionId = 100L,
                    sku = "SKU-001",
                    totalQuantity = 100,
                    safetyThreshold = 10,
                )

            Then("isBelowSafetyThreshold가 false") {
                inventory.isBelowSafetyThreshold() shouldBe false
            }
        }
    }

    Given("품절 확인") {
        When("available이 0") {
            val inventory =
                Inventory.create(
                    productId = 1L,
                    productOptionId = 100L,
                    sku = "SKU-001",
                    totalQuantity = 10,
                )
            inventory.reserve(10)

            Then("isOutOfStock이 true") {
                inventory.isOutOfStock() shouldBe true
            }
        }
    }

    Given("불변 조건") {
        When("reserve 후 항상 total == available + reserved") {
            val inventory =
                Inventory.create(
                    productId = 1L,
                    productOptionId = 100L,
                    sku = "SKU-001",
                    totalQuantity = 100,
                )

            inventory.reserve(30)
            Then("total == available + reserved") {
                inventory.totalQuantity shouldBe (inventory.availableQuantity + inventory.reservedQuantity)
            }

            inventory.release(10)
            Then("release 후에도 total == available + reserved") {
                inventory.totalQuantity shouldBe (inventory.availableQuantity + inventory.reservedQuantity)
            }

            inventory.deduct(10)
            Then("deduct 후에도 total == available + reserved") {
                inventory.totalQuantity shouldBe (inventory.availableQuantity + inventory.reservedQuantity)
            }

            inventory.inbound(50)
            Then("inbound 후에도 total == available + reserved") {
                inventory.totalQuantity shouldBe (inventory.availableQuantity + inventory.reservedQuantity)
            }
        }
    }

    Given("음수 수량 방어") {
        val inventory =
            Inventory.create(
                productId = 1L,
                productOptionId = 100L,
                sku = "SKU-001",
                totalQuantity = 100,
            )

        When("reserve(0)") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    inventory.reserve(0)
                }
            }
        }

        When("reserve(-1)") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    inventory.reserve(-1)
                }
            }
        }

        When("deduct(0)") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    inventory.deduct(0)
                }
            }
        }

        When("inbound(0)") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    inventory.inbound(0)
                }
            }
        }
    }
})
