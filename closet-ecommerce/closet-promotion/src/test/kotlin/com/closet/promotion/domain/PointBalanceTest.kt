package com.closet.promotion.domain

import com.closet.common.exception.BusinessException
import com.closet.promotion.domain.point.PointBalance
import com.closet.promotion.domain.point.PointTransactionType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PointBalanceTest : BehaviorSpec({

    Given("적립금 잔액") {
        When("회원 잔액 생성") {
            val balance = PointBalance.create(memberId = 1L)

            Then("초기 잔액은 0이다") {
                balance.memberId shouldBe 1L
                balance.totalPoints shouldBe 0
                balance.availablePoints shouldBe 0
            }
        }
    }

    Given("적립금 적립") {
        val balance = PointBalance.create(memberId = 1L)

        When("1000원 적립") {
            val history = balance.earn(1000)

            Then("잔액이 1000원 증가한다") {
                balance.totalPoints shouldBe 1000
                balance.availablePoints shouldBe 1000
            }

            Then("적립 이력이 생성된다") {
                history.amount shouldBe 1000
                history.balanceAfter shouldBe 1000
                history.transactionType shouldBe PointTransactionType.EARN
            }
        }

        When("추가로 500원 적립") {
            val history = balance.earn(500)

            Then("잔액이 누적된다") {
                balance.totalPoints shouldBe 1500
                balance.availablePoints shouldBe 1500
                history.balanceAfter shouldBe 1500
            }
        }

        When("0원 적립 시도") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    balance.earn(0)
                }
            }
        }

        When("음수 적립 시도") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    balance.earn(-100)
                }
            }
        }
    }

    Given("적립금 사용") {
        val balance = PointBalance.create(memberId = 1L)
        balance.earn(3000) // 3000원 적립

        When("1000원 사용") {
            val history = balance.use(1000)

            Then("사용 가능 잔액이 감소한다") {
                balance.availablePoints shouldBe 2000
                balance.totalPoints shouldBe 3000 // totalPoints는 변하지 않음
            }

            Then("사용 이력이 생성된다") {
                history.amount shouldBe -1000
                history.balanceAfter shouldBe 2000
                history.transactionType shouldBe PointTransactionType.USE
            }
        }

        When("잔액 초과 사용 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    balance.use(5000)
                }
            }
        }
    }

    Given("적립 취소") {
        val balance = PointBalance.create(memberId = 1L)
        balance.earn(3000)

        When("1000원 적립 취소") {
            val history = balance.cancelEarn(1000)

            Then("적립금이 감소한다") {
                balance.totalPoints shouldBe 2000
                balance.availablePoints shouldBe 2000
            }

            Then("취소 이력이 생성된다") {
                history.amount shouldBe -1000
                history.transactionType shouldBe PointTransactionType.CANCEL_EARN
            }
        }

        When("잔액 초과 취소 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    balance.cancelEarn(5000)
                }
            }
        }
    }

    Given("사용 취소") {
        val balance = PointBalance.create(memberId = 1L)
        balance.earn(3000)
        balance.use(2000) // 잔액: 1000

        When("1000원 사용 취소") {
            val history = balance.cancelUse(1000)

            Then("사용 가능 잔액이 복구된다") {
                balance.availablePoints shouldBe 2000
            }

            Then("사용 취소 이력이 생성된다") {
                history.amount shouldBe 1000
                history.transactionType shouldBe PointTransactionType.CANCEL_USE
            }
        }
    }
})
