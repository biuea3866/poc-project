package com.closet.seller

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.seller.application.SellerApplicationService
import com.closet.seller.application.SellerService
import com.closet.seller.application.SellerSettlementAccountService
import com.closet.seller.domain.ApplicationStatus
import com.closet.seller.domain.Seller
import com.closet.seller.domain.SellerApplication
import com.closet.seller.domain.SellerSettlementAccount
import com.closet.seller.domain.SellerStatus
import com.closet.seller.domain.repository.SellerApplicationRepository
import com.closet.seller.domain.repository.SellerRepository
import com.closet.seller.domain.repository.SellerSettlementAccountRepository
import com.closet.seller.presentation.dto.RegisterSettlementAccountRequest
import com.closet.seller.presentation.dto.SellerApplyRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class SellerServiceTest : BehaviorSpec({
    val sellerRepository = mockk<SellerRepository>()
    val sellerApplicationRepository = mockk<SellerApplicationRepository>()
    val settlementAccountRepository = mockk<SellerSettlementAccountRepository>()

    val sellerService = SellerService(sellerRepository)
    val sellerApplicationService = SellerApplicationService(sellerApplicationRepository, sellerService)
    val settlementAccountService = SellerSettlementAccountService(settlementAccountRepository, sellerService)

    Given("입점 신청이 주어졌을 때") {
        val request = SellerApplyRequest(
            email = "seller@brand.com",
            name = "김셀러",
            businessName = "브랜드컴퍼니",
            businessNumber = "123-45-67890",
            representativeName = "김대표",
            phone = "010-1234-5678",
            brandName = "패션브랜드",
            categoryIds = "1,2,3",
            businessLicenseUrl = "https://s3.amazonaws.com/license.pdf",
            bankName = "국민은행",
            accountNumber = "123-456-789012",
            accountHolder = "김대표",
        )

        When("신규 이메일과 사업자등록번호이면") {
            every { sellerRepository.existsByEmail(request.email) } returns false
            every { sellerRepository.existsByBusinessNumber(request.businessNumber) } returns false

            val sellerSlot = slot<Seller>()
            every { sellerRepository.save(capture(sellerSlot)) } answers {
                sellerSlot.captured.apply {
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }
            }

            val applicationSlot = slot<SellerApplication>()
            every { sellerApplicationRepository.save(capture(applicationSlot)) } answers {
                applicationSlot.captured
            }

            val result = sellerApplicationService.apply(request)

            Then("입점 신청이 SUBMITTED 상태로 생성된다") {
                result.brandName shouldBe request.brandName
                result.status shouldBe ApplicationStatus.SUBMITTED
                result.bankName shouldBe request.bankName
                result.accountNumber shouldBe request.accountNumber
            }
        }

        When("이미 등록된 이메일이면") {
            every { sellerRepository.existsByEmail(request.email) } returns true

            Then("DUPLICATE_ENTITY 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    sellerApplicationService.apply(request)
                }
                exception.errorCode shouldBe ErrorCode.DUPLICATE_ENTITY
            }
        }
    }

    Given("입점 신청 심사 플로우") {
        val seller = Seller.register(
            email = "seller@brand.com",
            name = "김셀러",
            businessName = "브랜드컴퍼니",
            businessNumber = "123-45-67890",
            representativeName = "김대표",
            phone = "010-1234-5678",
        ).apply {
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        val application = SellerApplication(
            sellerId = seller.id,
            brandName = "패션브랜드",
            categoryIds = "1,2,3",
            businessLicenseUrl = "https://s3.amazonaws.com/license.pdf",
            bankName = "국민은행",
            accountNumber = "123-456-789012",
            accountHolder = "김대표",
        )

        When("심사를 시작하면") {
            every { sellerApplicationRepository.findById(any()) } returns Optional.of(application)

            val result = sellerApplicationService.startReview(application.id)

            Then("상태가 REVIEWING으로 변경된다") {
                result.status shouldBe ApplicationStatus.REVIEWING
            }
        }

        When("승인하면") {
            val reviewingApplication = SellerApplication(
                sellerId = seller.id,
                brandName = "패션브랜드",
                categoryIds = "1,2,3",
                businessLicenseUrl = "https://s3.amazonaws.com/license.pdf",
                bankName = "국민은행",
                accountNumber = "123-456-789012",
                accountHolder = "김대표",
                status = ApplicationStatus.REVIEWING,
            )

            every { sellerApplicationRepository.findById(any()) } returns Optional.of(reviewingApplication)
            every { sellerRepository.findByIdAndDeletedAtIsNull(any()) } returns seller

            val commissionRate = BigDecimal("10.00")
            val result = sellerApplicationService.approve(reviewingApplication.id, commissionRate)

            Then("셀러가 ACTIVE 상태가 되고 수수료율이 설정된다") {
                result.status shouldBe SellerStatus.ACTIVE
                result.commissionRate shouldBe commissionRate
            }

            Then("신청 상태가 APPROVED로 변경된다") {
                reviewingApplication.status shouldBe ApplicationStatus.APPROVED
                reviewingApplication.reviewedAt shouldNotBe null
            }
        }

        When("반려하면") {
            val reviewingApplication = SellerApplication(
                sellerId = seller.id,
                brandName = "패션브랜드",
                categoryIds = "1,2,3",
                businessLicenseUrl = "https://s3.amazonaws.com/license.pdf",
                bankName = "국민은행",
                accountNumber = "123-456-789012",
                accountHolder = "김대표",
                status = ApplicationStatus.REVIEWING,
            )

            every { sellerApplicationRepository.findById(any()) } returns Optional.of(reviewingApplication)

            val reason = "사업자등록증이 유효하지 않습니다"
            val result = sellerApplicationService.reject(reviewingApplication.id, reason)

            Then("상태가 REJECTED이고 반려 사유가 설정된다") {
                result.status shouldBe ApplicationStatus.REJECTED
                result.rejectReason shouldBe reason
                result.reviewedAt shouldNotBe null
            }
        }
    }

    Given("셀러 상태 전이 규칙") {
        When("PENDING 셀러를 ACTIVE로 전환하면") {
            val seller = Seller.register(
                email = "test@brand.com",
                name = "테스트",
                businessName = "테스트컴퍼니",
                businessNumber = "111-22-33333",
                representativeName = "김테스트",
                phone = "010-0000-0000",
            )

            seller.activate(BigDecimal("15.00"))

            Then("정상적으로 ACTIVE 상태가 된다") {
                seller.status shouldBe SellerStatus.ACTIVE
                seller.commissionRate shouldBe BigDecimal("15.00")
            }
        }

        When("ACTIVE 셀러를 정지시키면") {
            val seller = Seller.register(
                email = "test2@brand.com",
                name = "테스트2",
                businessName = "테스트컴퍼니2",
                businessNumber = "222-33-44444",
                representativeName = "이테스트",
                phone = "010-1111-1111",
            )
            seller.activate(BigDecimal("10.00"))
            seller.suspend("정책 위반")

            Then("SUSPENDED 상태가 된다") {
                seller.status shouldBe SellerStatus.SUSPENDED
            }
        }

        When("WITHDRAWN 셀러의 상태를 변경하려고 하면") {
            val seller = Seller.register(
                email = "test3@brand.com",
                name = "테스트3",
                businessName = "테스트컴퍼니3",
                businessNumber = "333-44-55555",
                representativeName = "박테스트",
                phone = "010-2222-2222",
            )
            seller.activate(BigDecimal("10.00"))
            seller.withdraw()

            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    seller.activate(BigDecimal("10.00"))
                }
            }
        }
    }

    Given("정산 계좌 등록") {
        val request = RegisterSettlementAccountRequest(
            bankName = "신한은행",
            accountNumber = "110-123-456789",
            accountHolder = "김셀러",
        )

        val seller = Seller.register(
            email = "account@brand.com",
            name = "계좌테스트",
            businessName = "계좌컴퍼니",
            businessNumber = "444-55-66666",
            representativeName = "최테스트",
            phone = "010-3333-3333",
        ).apply {
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        When("정산 계좌가 없는 셀러에게 등록하면") {
            every { sellerRepository.findByIdAndDeletedAtIsNull(any()) } returns seller
            every { settlementAccountRepository.findBySellerId(any()) } returns null

            val accountSlot = slot<SellerSettlementAccount>()
            every { settlementAccountRepository.save(capture(accountSlot)) } answers {
                accountSlot.captured
            }

            val result = settlementAccountService.register(seller.id, request)

            Then("정산 계좌가 생성된다") {
                result.bankName shouldBe request.bankName
                result.accountNumber shouldBe request.accountNumber
                result.accountHolder shouldBe request.accountHolder
                result.isVerified shouldBe false
            }
        }

        When("이미 정산 계좌가 있으면") {
            every { sellerRepository.findByIdAndDeletedAtIsNull(any()) } returns seller
            every { settlementAccountRepository.findBySellerId(any()) } returns SellerSettlementAccount(
                sellerId = seller.id,
                bankName = "기존은행",
                accountNumber = "000-000-000000",
                accountHolder = "기존예금주",
            )

            Then("DUPLICATE_ENTITY 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    settlementAccountService.register(seller.id, request)
                }
                exception.errorCode shouldBe ErrorCode.DUPLICATE_ENTITY
            }
        }
    }
})
