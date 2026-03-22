package com.closet.shipping.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.shipping.domain.ReturnRequest
import com.closet.shipping.presentation.dto.CreateReturnRequest
import com.closet.shipping.presentation.dto.ReturnRequestResponse
import com.closet.shipping.repository.ReturnRequestRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class ReturnRequestService(
    private val returnRequestRepository: ReturnRequestRepository,
) {

    @Transactional
    fun requestReturn(request: CreateReturnRequest): ReturnRequestResponse {
        val returnRequest = ReturnRequest.create(
            orderId = request.orderId,
            orderItemId = request.orderItemId,
            type = request.type,
            reasonType = request.reasonType,
            reasonDetail = request.reasonDetail,
            shippingFeeBearer = request.shippingFeeBearer,
            returnShippingFee = request.returnShippingFee,
        )

        val saved = returnRequestRepository.save(returnRequest)

        logger.info { "반품 요청 생성: returnRequestId=${saved.id}, orderId=${saved.orderId}, type=${saved.type.name}" }
        return ReturnRequestResponse.from(saved)
    }

    @Transactional
    fun approveReturn(id: Long): ReturnRequestResponse {
        val returnRequest = findReturnRequestById(id)

        returnRequest.approve()

        logger.info { "반품 승인: returnRequestId=${returnRequest.id}" }
        return ReturnRequestResponse.from(returnRequest)
    }

    @Transactional
    fun rejectReturn(id: Long): ReturnRequestResponse {
        val returnRequest = findReturnRequestById(id)

        returnRequest.reject()

        logger.info { "반품 거절: returnRequestId=${returnRequest.id}" }
        return ReturnRequestResponse.from(returnRequest)
    }

    private fun findReturnRequestById(id: Long): ReturnRequest {
        return returnRequestRepository.findById(id).orElseThrow {
            BusinessException(ErrorCode.ENTITY_NOT_FOUND, "반품 요청을 찾을 수 없습니다. id=$id")
        }
    }
}
