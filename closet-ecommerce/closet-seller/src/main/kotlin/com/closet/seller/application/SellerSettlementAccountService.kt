package com.closet.seller.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.seller.domain.SellerSettlementAccount
import com.closet.seller.domain.repository.SellerSettlementAccountRepository
import com.closet.seller.presentation.dto.RegisterSettlementAccountRequest
import com.closet.seller.presentation.dto.SettlementAccountResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SellerSettlementAccountService(
    private val settlementAccountRepository: SellerSettlementAccountRepository,
    private val sellerService: SellerService,
) {
    /** 정산 계좌 등록 */
    @Transactional
    fun register(sellerId: Long, request: RegisterSettlementAccountRequest): SettlementAccountResponse {
        // 셀러 존재 확인
        sellerService.findById(sellerId)

        // 기존 계좌가 있으면 중복 등록 방지
        if (settlementAccountRepository.findBySellerId(sellerId) != null) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 정산 계좌가 등록되어 있습니다")
        }

        val account = SellerSettlementAccount(
            sellerId = sellerId,
            bankName = request.bankName,
            accountNumber = request.accountNumber,
            accountHolder = request.accountHolder,
        )

        val saved = settlementAccountRepository.save(account)
        return SettlementAccountResponse.from(saved)
    }

    /** 계좌 인증 */
    @Transactional
    fun verify(accountId: Long): SettlementAccountResponse {
        val account = settlementAccountRepository.findById(accountId).orElseThrow {
            BusinessException(ErrorCode.ENTITY_NOT_FOUND, "정산 계좌를 찾을 수 없습니다")
        }
        account.verify()
        return SettlementAccountResponse.from(account)
    }

    /** 셀러별 정산 계좌 조회 */
    fun findBySellerId(sellerId: Long): SettlementAccountResponse {
        val account = settlementAccountRepository.findBySellerId(sellerId)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "정산 계좌를 찾을 수 없습니다")
        return SettlementAccountResponse.from(account)
    }
}
