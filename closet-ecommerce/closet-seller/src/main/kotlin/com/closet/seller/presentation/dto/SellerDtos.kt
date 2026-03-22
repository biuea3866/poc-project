package com.closet.seller.presentation.dto

import com.closet.seller.domain.ApplicationStatus
import com.closet.seller.domain.Seller
import com.closet.seller.domain.SellerApplication
import com.closet.seller.domain.SellerSettlementAccount
import com.closet.seller.domain.SellerStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

/** 입점 신청 요청 */
data class SellerApplyRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "이메일 형식이 올바르지 않습니다")
    val email: String,

    @field:NotBlank(message = "담당자 이름은 필수입니다")
    val name: String,

    @field:NotBlank(message = "사업체명은 필수입니다")
    val businessName: String,

    @field:NotBlank(message = "사업자등록번호는 필수입니다")
    val businessNumber: String,

    @field:NotBlank(message = "대표자 이름은 필수입니다")
    val representativeName: String,

    @field:NotBlank(message = "연락처는 필수입니다")
    val phone: String,

    @field:NotBlank(message = "브랜드명은 필수입니다")
    val brandName: String,

    val categoryIds: String? = null,

    @field:NotBlank(message = "사업자등록증 URL은 필수입니다")
    val businessLicenseUrl: String,

    @field:NotBlank(message = "은행명은 필수입니다")
    val bankName: String,

    @field:NotBlank(message = "계좌번호는 필수입니다")
    val accountNumber: String,

    @field:NotBlank(message = "예금주는 필수입니다")
    val accountHolder: String,
)

/** 입점 승인 요청 */
data class ApproveApplicationRequest(
    @field:NotNull(message = "수수료율은 필수입니다")
    @field:DecimalMin(value = "0.01", message = "수수료율은 0보다 커야 합니다")
    val commissionRate: BigDecimal,
)

/** 입점 반려 요청 */
data class RejectApplicationRequest(
    @field:NotBlank(message = "반려 사유는 필수입니다")
    val reason: String,
)

/** 정산 계좌 등록 요청 */
data class RegisterSettlementAccountRequest(
    @field:NotBlank(message = "은행명은 필수입니다")
    val bankName: String,

    @field:NotBlank(message = "계좌번호는 필수입니다")
    val accountNumber: String,

    @field:NotBlank(message = "예금주는 필수입니다")
    val accountHolder: String,
)

/** 셀러 응답 */
data class SellerResponse(
    val id: Long,
    val email: String,
    val name: String,
    val businessName: String,
    val businessNumber: String,
    val representativeName: String,
    val phone: String,
    val status: SellerStatus,
    val commissionRate: BigDecimal?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(seller: Seller): SellerResponse = SellerResponse(
            id = seller.id,
            email = seller.email,
            name = seller.name,
            businessName = seller.businessName,
            businessNumber = seller.businessNumber,
            representativeName = seller.representativeName,
            phone = seller.phone,
            status = seller.status,
            commissionRate = seller.commissionRate,
            createdAt = seller.createdAt,
        )
    }
}

/** 입점 신청 응답 */
data class SellerApplicationResponse(
    val id: Long,
    val sellerId: Long,
    val brandName: String,
    val categoryIds: String?,
    val businessLicenseUrl: String,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val status: ApplicationStatus,
    val rejectReason: String?,
    val submittedAt: LocalDateTime,
    val reviewedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(application: SellerApplication): SellerApplicationResponse = SellerApplicationResponse(
            id = application.id,
            sellerId = application.sellerId,
            brandName = application.brandName,
            categoryIds = application.categoryIds,
            businessLicenseUrl = application.businessLicenseUrl,
            bankName = application.bankName,
            accountNumber = application.accountNumber,
            accountHolder = application.accountHolder,
            status = application.status,
            rejectReason = application.rejectReason,
            submittedAt = application.submittedAt,
            reviewedAt = application.reviewedAt,
            createdAt = application.createdAt,
        )
    }
}

/** 정산 계좌 응답 */
data class SettlementAccountResponse(
    val id: Long,
    val sellerId: Long,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val isVerified: Boolean,
    val verifiedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(account: SellerSettlementAccount): SettlementAccountResponse = SettlementAccountResponse(
            id = account.id,
            sellerId = account.sellerId,
            bankName = account.bankName,
            accountNumber = account.accountNumber,
            accountHolder = account.accountHolder,
            isVerified = account.isVerified,
            verifiedAt = account.verifiedAt,
            createdAt = account.createdAt,
        )
    }
}
