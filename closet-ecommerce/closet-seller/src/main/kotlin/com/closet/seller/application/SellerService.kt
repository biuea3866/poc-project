package com.closet.seller.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.seller.domain.Seller
import com.closet.seller.domain.repository.SellerRepository
import com.closet.seller.presentation.dto.SellerResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class SellerService(
    private val sellerRepository: SellerRepository,
) {
    /** 셀러 등록 */
    @Transactional
    fun register(
        email: String,
        name: String,
        businessName: String,
        businessNumber: String,
        representativeName: String,
        phone: String,
    ): Seller {
        if (sellerRepository.existsByEmail(email)) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 등록된 이메일입니다")
        }
        if (sellerRepository.existsByBusinessNumber(businessNumber)) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 등록된 사업자등록번호입니다")
        }

        val seller = Seller.register(
            email = email,
            name = name,
            businessName = businessName,
            businessNumber = businessNumber,
            representativeName = representativeName,
            phone = phone,
        )
        return sellerRepository.save(seller)
    }

    /** 셀러 조회 */
    fun findById(id: Long): SellerResponse {
        val seller = sellerRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "셀러를 찾을 수 없습니다")
        return SellerResponse.from(seller)
    }

    /** 셀러 목록 조회 */
    fun findAll(): List<SellerResponse> {
        return sellerRepository.findAllByDeletedAtIsNull().map { SellerResponse.from(it) }
    }

    /** 셀러 활성화 */
    @Transactional
    fun activate(id: Long, commissionRate: BigDecimal): SellerResponse {
        val seller = getSellerEntity(id)
        seller.activate(commissionRate)
        return SellerResponse.from(seller)
    }

    /** 셀러 정지 */
    @Transactional
    fun suspend(id: Long, reason: String): SellerResponse {
        val seller = getSellerEntity(id)
        seller.suspend(reason)
        return SellerResponse.from(seller)
    }

    /** 셀러 탈퇴 */
    @Transactional
    fun withdraw(id: Long) {
        val seller = getSellerEntity(id)
        seller.withdraw()
    }

    internal fun getSellerEntity(id: Long): Seller {
        return sellerRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "셀러를 찾을 수 없습니다")
    }
}
