package com.closet.member.integration

import com.closet.common.exception.BusinessException
import com.closet.common.test.ClosetIntegrationTest
import com.closet.member.application.point.PointService
import com.closet.member.domain.point.PointReferenceType
import com.closet.member.domain.point.PointTransactionType
import com.closet.member.domain.repository.PointBalanceRepository
import com.closet.member.domain.repository.PointHistoryRepository
import com.closet.member.presentation.dto.CancelPointRequest
import com.closet.member.presentation.dto.EarnPointRequest
import com.closet.member.presentation.dto.UsePointRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@ActiveProfiles("test")
class PointServiceIntegrationTest(
    private val pointService: PointService,
    private val pointBalanceRepository: PointBalanceRepository,
    private val pointHistoryRepository: PointHistoryRepository,
    private val redisTemplate: StringRedisTemplate,
) : BehaviorSpec({

        beforeEach {
            pointHistoryRepository.deleteAll()
            pointBalanceRepository.deleteAll()
            // Redis 일일 한도 키 초기화
            redisTemplate.keys("review:daily_point:*").forEach { redisTemplate.delete(it) }
        }

        Given("적립금 적립 — 실제 DB에 저장") {
            val memberId = 1L

            When("3000원을 적립하면") {
                val request =
                    EarnPointRequest(
                        memberId = memberId,
                        amount = 3000,
                        referenceType = PointReferenceType.ORDER,
                        referenceId = 100L,
                    )
                val response = pointService.earn(request)

                Then("PointBalance가 DB에 저장되고 잔액이 증가한다") {
                    response.amount shouldBe 3000
                    response.transactionType shouldBe PointTransactionType.EARN
                    response.referenceType shouldBe PointReferenceType.ORDER
                    response.referenceId shouldBe 100L

                    val balance = pointBalanceRepository.findByMemberId(memberId).orElse(null)
                    balance shouldNotBe null
                    balance.availablePoints shouldBe 3000
                    balance.totalPoints shouldBe 3000
                }

                Then("PointHistory가 DB에 기록된다") {
                    val histories = pointHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                    histories shouldHaveSize 1
                    histories[0].amount shouldBe 3000
                    histories[0].transactionType shouldBe PointTransactionType.EARN
                    histories[0].balanceAfter shouldBe 3000
                    histories[0].createdAt shouldNotBe null
                }
            }
        }

        Given("적립금 사용 — 실제 DB에서 차감") {
            val memberId = 2L

            // 사전 조건: 5000원 적립
            pointService.earn(
                EarnPointRequest(
                    memberId = memberId,
                    amount = 5000,
                    referenceType = PointReferenceType.ORDER,
                    referenceId = 200L,
                ),
            )

            When("2000원을 사용하면") {
                val request =
                    UsePointRequest(
                        memberId = memberId,
                        amount = 2000,
                        referenceType = PointReferenceType.ORDER,
                        referenceId = 201L,
                    )
                val response = pointService.use(request)

                Then("잔액이 3000원으로 감소한다") {
                    response.amount shouldBe -2000
                    response.transactionType shouldBe PointTransactionType.USE
                    response.balanceAfter shouldBe 3000

                    val balance = pointBalanceRepository.findByMemberId(memberId).orElse(null)
                    balance.availablePoints shouldBe 3000
                }

                Then("사용 이력이 DB에 기록된다") {
                    val histories = pointHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                    histories shouldHaveSize 2 // EARN + USE
                    histories[0].transactionType shouldBe PointTransactionType.USE
                    histories[0].amount shouldBe -2000
                }
            }

            When("잔액을 초과하여 사용하면") {
                Then("BusinessException이 발생한다") {
                    shouldThrow<BusinessException> {
                        pointService.use(
                            UsePointRequest(
                                memberId = memberId,
                                amount = 99999,
                            ),
                        )
                    }
                }
            }
        }

        Given("적립 취소 — CANCEL_EARN") {
            val memberId = 3L

            // 사전 조건: 5000원 적립
            pointService.earn(
                EarnPointRequest(
                    memberId = memberId,
                    amount = 5000,
                ),
            )

            When("2000원 적립을 취소하면") {
                val request =
                    CancelPointRequest(
                        memberId = memberId,
                        amount = 2000,
                        transactionType = PointTransactionType.CANCEL_EARN,
                    )
                val response = pointService.cancel(request)

                Then("잔액이 3000원으로 감소하고 totalPoints도 감소한다") {
                    response.transactionType shouldBe PointTransactionType.CANCEL_EARN
                    response.amount shouldBe -2000
                    response.balanceAfter shouldBe 3000

                    val balance = pointBalanceRepository.findByMemberId(memberId).orElse(null)
                    balance.availablePoints shouldBe 3000
                    balance.totalPoints shouldBe 3000
                }

                Then("취소 이력이 DB에 기록된다") {
                    val histories = pointHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                    histories shouldHaveSize 2 // EARN + CANCEL_EARN
                    histories[0].transactionType shouldBe PointTransactionType.CANCEL_EARN
                }
            }
        }

        Given("사용 취소 — CANCEL_USE") {
            val memberId = 4L

            // 사전 조건: 5000원 적립 후 3000원 사용 (잔액: 2000)
            pointService.earn(EarnPointRequest(memberId = memberId, amount = 5000))
            pointService.use(UsePointRequest(memberId = memberId, amount = 3000))

            When("1000원 사용을 취소하면") {
                val request =
                    CancelPointRequest(
                        memberId = memberId,
                        amount = 1000,
                        transactionType = PointTransactionType.CANCEL_USE,
                    )
                val response = pointService.cancel(request)

                Then("잔액이 3000원으로 복구된다") {
                    response.transactionType shouldBe PointTransactionType.CANCEL_USE
                    response.amount shouldBe 1000
                    response.balanceAfter shouldBe 3000

                    val balance = pointBalanceRepository.findByMemberId(memberId).orElse(null)
                    balance.availablePoints shouldBe 3000
                }
            }
        }

        Given("리뷰 포인트 적립 — Redis 일일 한도 관리") {
            val memberId = 5L

            When("300포인트 리뷰 포인트를 적립하면") {
                val actual =
                    pointService.earnReviewPoint(
                        memberId = memberId,
                        reviewId = 500L,
                        amount = 300,
                    )

                Then("포인트가 적립되고 DB에 반영된다") {
                    actual shouldBe 300

                    val balance = pointBalanceRepository.findByMemberId(memberId).orElse(null)
                    balance shouldNotBe null
                    balance.availablePoints shouldBe 300

                    val histories = pointHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                    histories shouldHaveSize 1
                    histories[0].referenceType shouldBe PointReferenceType.REVIEW
                    histories[0].referenceId shouldBe 500L
                }

                Then("Redis에 일일 누적이 기록된다") {
                    val keys = redisTemplate.keys("review:daily_point:$memberId:*")
                    keys.size shouldBe 1
                }
            }
        }

        Given("리뷰 포인트 회수 — 리뷰 삭제 시") {
            val memberId = 6L

            // 사전 조건: 리뷰 300P 적립
            pointService.earnReviewPoint(memberId = memberId, reviewId = 600L, amount = 300)

            When("리뷰를 삭제하여 포인트를 회수하면") {
                pointService.revokeReviewPoint(memberId = memberId, reviewId = 600L, amount = 300)

                Then("잔액이 0으로 돌아간다") {
                    val balance = pointBalanceRepository.findByMemberId(memberId).orElse(null)
                    balance.availablePoints shouldBe 0
                }

                Then("회수 이력이 DB에 기록된다") {
                    val histories = pointHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                    histories shouldHaveSize 2 // EARN + CANCEL_EARN(회수)
                    histories[0].transactionType shouldBe PointTransactionType.CANCEL_EARN
                    histories[0].amount shouldBe -300
                }
            }
        }
    }) {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            ClosetIntegrationTest.overrideProperties(registry)
        }
    }
}
