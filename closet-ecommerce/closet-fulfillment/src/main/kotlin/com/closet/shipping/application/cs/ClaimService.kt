package com.closet.shipping.application.cs

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.shipping.domain.cs.claim.ClaimRequest
import com.closet.shipping.domain.cs.claim.ClaimRequestRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class ClaimService(
    private val claimRequestRepository: ClaimRequestRepository,
) {
    @Transactional
    fun createClaim(request: CreateClaimCommand): ClaimResponse {
        val claim =
            ClaimRequest.create(
                orderId = request.orderId,
                orderItemId = request.orderItemId,
                memberId = request.memberId,
                claimType = request.claimType,
                reasonCategory = request.reasonCategory,
                reasonDetail = request.reasonDetail,
            )

        val saved = claimRequestRepository.save(claim)
        logger.info { "클레임 접수: id=${saved.id}, orderId=${saved.orderId}, type=${saved.claimType}" }
        return ClaimResponse.from(saved)
    }

    fun getClaim(claimId: Long): ClaimResponse {
        val claim = findClaimById(claimId)
        return ClaimResponse.from(claim)
    }

    fun getClaimsByMember(memberId: Long): List<ClaimResponse> {
        return claimRequestRepository.findByMemberId(memberId)
            .map { ClaimResponse.from(it) }
    }

    @Transactional
    fun approveClaim(
        claimId: Long,
        refundAmount: BigDecimal,
    ): ClaimResponse {
        val claim = findClaimById(claimId)
        claim.approve(refundAmount)

        logger.info { "클레임 승인: id=$claimId, refundAmount=$refundAmount" }
        return ClaimResponse.from(claim)
    }

    @Transactional
    fun rejectClaim(claimId: Long): ClaimResponse {
        val claim = findClaimById(claimId)
        claim.reject()

        logger.info { "클레임 거부: id=$claimId" }
        return ClaimResponse.from(claim)
    }

    @Transactional
    fun completeClaim(claimId: Long): ClaimResponse {
        val claim = findClaimById(claimId)
        claim.complete()

        logger.info { "클레임 완료: id=$claimId" }
        return ClaimResponse.from(claim)
    }

    private fun findClaimById(claimId: Long): ClaimRequest {
        return claimRequestRepository.findById(claimId)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "클레임을 찾을 수 없습니다. id=$claimId") }
    }
}
