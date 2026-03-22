package com.closet.seller.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.seller.domain.ApplicationStatus
import com.closet.seller.domain.SellerApplication
import com.closet.seller.domain.repository.SellerApplicationRepository
import com.closet.seller.presentation.dto.SellerApplicationResponse
import com.closet.seller.presentation.dto.SellerApplyRequest
import com.closet.seller.presentation.dto.SellerResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class SellerApplicationService(
    private val sellerApplicationRepository: SellerApplicationRepository,
    private val sellerService: SellerService,
) {
    /** 입점 신청 */
    @Transactional
    fun apply(request: SellerApplyRequest): SellerApplicationResponse {
        // 셀러 등록
        val seller = sellerService.register(
            email = request.email,
            name = request.name,
            businessName = request.businessName,
            businessNumber = request.businessNumber,
            representativeName = request.representativeName,
            phone = request.phone,
        )

        // 입점 신청 생성
        val application = SellerApplication(
            sellerId = seller.id,
            brandName = request.brandName,
            categoryIds = request.categoryIds,
            businessLicenseUrl = request.businessLicenseUrl,
            bankName = request.bankName,
            accountNumber = request.accountNumber,
            accountHolder = request.accountHolder,
        )

        val saved = sellerApplicationRepository.save(application)
        return SellerApplicationResponse.from(saved)
    }

    /** 심사 시작 */
    @Transactional
    fun startReview(applicationId: Long): SellerApplicationResponse {
        val application = getApplicationEntity(applicationId)
        application.startReview()
        return SellerApplicationResponse.from(application)
    }

    /** 승인 (셀러 활성화 포함) */
    @Transactional
    fun approve(applicationId: Long, commissionRate: BigDecimal): SellerResponse {
        val application = getApplicationEntity(applicationId)
        application.approve()

        // 셀러 활성화
        return sellerService.activate(application.sellerId, commissionRate)
    }

    /** 반려 */
    @Transactional
    fun reject(applicationId: Long, reason: String): SellerApplicationResponse {
        val application = getApplicationEntity(applicationId)
        application.reject(reason)
        return SellerApplicationResponse.from(application)
    }

    /** 상태별 신청 목록 조회 */
    fun findByStatus(status: ApplicationStatus): List<SellerApplicationResponse> {
        return sellerApplicationRepository.findAllByStatus(status)
            .map { SellerApplicationResponse.from(it) }
    }

    private fun getApplicationEntity(id: Long): SellerApplication {
        return sellerApplicationRepository.findById(id).orElseThrow {
            BusinessException(ErrorCode.ENTITY_NOT_FOUND, "입점 신청을 찾을 수 없습니다")
        }
    }
}
