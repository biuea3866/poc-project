package com.closet.member.application

import com.closet.common.exception.BusinessException
import com.closet.member.application.point.PointService
import com.closet.member.domain.point.PointBalance
import com.closet.member.domain.point.PointHistory
import com.closet.member.domain.point.PointReferenceType
import com.closet.member.domain.point.PointTransactionType
import com.closet.member.domain.repository.PointBalanceRepository
import com.closet.member.domain.repository.PointHistoryRepository
import com.closet.member.presentation.dto.CancelPointRequest
import com.closet.member.presentation.dto.EarnPointRequest
import com.closet.member.presentation.dto.UsePointRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.ZonedDateTime
import java.util.Optional

class PointServiceTest : BehaviorSpec({

    val pointBalanceRepository = mockk<PointBalanceRepository>()
    val pointHistoryRepository = mockk<PointHistoryRepository>()
    val pointService =
        PointService(
            pointBalanceRepository = pointBalanceRepository,
            pointHistoryRepository = pointHistoryRepository,
        )

    Given("적립금 잔액 조회") {
        val balance = PointBalance.create(memberId = 1L)

        every { pointBalanceRepository.findByMemberId(1L) } returns Optional.of(balance)

        When("회원의 잔액을 조회하면") {
            val response = pointService.getBalance(1L)

            Then("잔액 정보가 반환된다") {
                response.memberId shouldBe 1L
                response.totalPoints shouldBe 0
                response.availablePoints shouldBe 0
            }
        }
    }

    Given("적립금 잔액이 없는 회원") {
        every { pointBalanceRepository.findByMemberId(99L) } returns Optional.empty()

        val balanceSlot = slot<PointBalance>()
        every { pointBalanceRepository.save(capture(balanceSlot)) } answers { balanceSlot.captured }

        When("잔액 조회 시 자동 생성") {
            val response = pointService.getBalance(99L)

            Then("초기 잔액 0으로 생성된다") {
                response.memberId shouldBe 99L
                response.totalPoints shouldBe 0
                response.availablePoints shouldBe 0
            }
        }
    }

    Given("적립금 적립") {
        val balance = PointBalance.create(memberId = 1L)

        every { pointBalanceRepository.findByMemberId(1L) } returns Optional.of(balance)
        every { pointBalanceRepository.save(any()) } answers { firstArg() }

        val historySlot = slot<PointHistory>()
        every { pointHistoryRepository.save(capture(historySlot)) } answers {
            historySlot.captured.apply { createdAt = ZonedDateTime.now() }
        }

        When("주문 확정 후 적립") {
            val request =
                EarnPointRequest(
                    memberId = 1L,
                    amount = 3000,
                    referenceType = PointReferenceType.ORDER,
                    referenceId = 100L,
                )

            val response = pointService.earn(request)

            Then("적립금이 증가하고 이력이 생성된다") {
                response.amount shouldBe 3000
                response.transactionType shouldBe PointTransactionType.EARN
                response.referenceType shouldBe PointReferenceType.ORDER
                response.referenceId shouldBe 100L
                balance.availablePoints shouldBe 3000
            }
        }
    }

    Given("적립금 사용") {
        val balance = PointBalance.create(memberId = 2L)
        balance.earn(5000) // 5000원 적립

        every { pointBalanceRepository.findByMemberId(2L) } returns Optional.of(balance)
        every { pointBalanceRepository.save(any()) } answers { firstArg() }

        val historySlot = slot<PointHistory>()
        every { pointHistoryRepository.save(capture(historySlot)) } answers {
            historySlot.captured.apply { createdAt = ZonedDateTime.now() }
        }

        When("2000원 사용") {
            val request =
                UsePointRequest(
                    memberId = 2L,
                    amount = 2000,
                    referenceType = PointReferenceType.ORDER,
                    referenceId = 200L,
                )

            val response = pointService.use(request)

            Then("적립금이 차감된다") {
                response.amount shouldBe -2000
                response.transactionType shouldBe PointTransactionType.USE
                balance.availablePoints shouldBe 3000
            }
        }

        When("잔액 초과 사용 시도") {
            val request =
                UsePointRequest(
                    memberId = 2L,
                    amount = 10000,
                )

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    pointService.use(request)
                }
            }
        }
    }

    Given("적립 취소") {
        val balance = PointBalance.create(memberId = 3L)
        balance.earn(5000)

        every { pointBalanceRepository.findByMemberId(3L) } returns Optional.of(balance)
        every { pointBalanceRepository.save(any()) } answers { firstArg() }

        val historySlot = slot<PointHistory>()
        every { pointHistoryRepository.save(capture(historySlot)) } answers {
            historySlot.captured.apply { createdAt = ZonedDateTime.now() }
        }

        When("적립 취소") {
            val request =
                CancelPointRequest(
                    memberId = 3L,
                    amount = 2000,
                    transactionType = PointTransactionType.CANCEL_EARN,
                )

            val response = pointService.cancel(request)

            Then("적립금이 차감된다") {
                response.transactionType shouldBe PointTransactionType.CANCEL_EARN
                balance.availablePoints shouldBe 3000
                balance.totalPoints shouldBe 3000
            }
        }
    }

    Given("사용 취소") {
        val balance = PointBalance.create(memberId = 4L)
        balance.earn(5000)
        balance.use(3000) // 잔액: 2000

        every { pointBalanceRepository.findByMemberId(4L) } returns Optional.of(balance)
        every { pointBalanceRepository.save(any()) } answers { firstArg() }

        val historySlot = slot<PointHistory>()
        every { pointHistoryRepository.save(capture(historySlot)) } answers {
            historySlot.captured.apply { createdAt = ZonedDateTime.now() }
        }

        When("사용 취소") {
            val request =
                CancelPointRequest(
                    memberId = 4L,
                    amount = 1000,
                    transactionType = PointTransactionType.CANCEL_USE,
                )

            val response = pointService.cancel(request)

            Then("적립금이 복구된다") {
                response.transactionType shouldBe PointTransactionType.CANCEL_USE
                balance.availablePoints shouldBe 3000
            }
        }
    }
})
