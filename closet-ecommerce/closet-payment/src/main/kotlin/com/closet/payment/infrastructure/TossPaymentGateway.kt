package com.closet.payment.infrastructure

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

/**
 * Toss Payments PG 어댑터.
 *
 * Mock API: /toss/v1/payments (closet-external-api)
 * Production: https://api.tosspayments.com/v1/payments
 */
@Component
class TossPaymentGateway(
    private val restTemplate: RestTemplate,
    @Value("\${payment.gateway.toss.api-url:http://localhost:9090}")
    private val apiUrl: String,
    @Value("\${payment.gateway.toss.secret-key:test_sk_key}")
    private val secretKey: String,
) : PaymentGateway {
    override fun getPaymentType(): PaymentType = PaymentType.TOSS

    override fun approve(request: PaymentApproveRequest): PaymentApproveResponse {
        val url = "$apiUrl/toss/v1/payments/confirm"
        val body =
            mapOf(
                "paymentKey" to request.paymentKey,
                "orderId" to request.orderId.toString(),
                "amount" to request.amount,
            )

        logger.info { "[Toss] 결제 승인 요청: paymentKey=${request.paymentKey}, orderId=${request.orderId}" }

        val response = callApi(url, body)
        val data = extractData(response, "결제 승인")

        return PaymentApproveResponse(
            approved = true,
            paymentKey = data["paymentKey"]?.toString() ?: request.paymentKey,
            approvedAt = data["approvedAt"]?.toString() ?: "",
        )
    }

    override fun cancel(request: PaymentCancelRequest): PaymentCancelResponse {
        val url = "$apiUrl/toss/v1/payments/${request.paymentKey}/cancel"
        val body = mapOf("cancelReason" to request.reason)

        logger.info { "[Toss] 결제 취소 요청: paymentKey=${request.paymentKey}" }

        val response = callApi(url, body)
        val data = extractData(response, "결제 취소")

        return PaymentCancelResponse(
            cancelled = true,
            cancelledAt = data["cancelledAt"]?.toString() ?: "",
        )
    }

    override fun refund(request: PaymentRefundRequest): PaymentRefundResponse {
        val url = "$apiUrl/toss/v1/payments/${request.paymentKey}/cancel"
        val body =
            mapOf(
                "cancelReason" to (request.reason ?: "부분 환불"),
                "cancelAmount" to request.cancelAmount,
            )

        logger.info { "[Toss] 부분 환불 요청: paymentKey=${request.paymentKey}, amount=${request.cancelAmount}" }

        val response = callApi(url, body)
        val data = extractData(response, "부분 환불")

        return PaymentRefundResponse(
            refunded = true,
            refundAmount = request.cancelAmount,
            refundedAt = data["cancelledAt"]?.toString() ?: "",
        )
    }

    private fun callApi(
        url: String,
        body: Map<String, Any?>,
    ): Map<*, *>? {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBasicAuth(secretKey, "")
            }
        val entity = HttpEntity(body, headers)

        return try {
            restTemplate.postForObject(url, entity, Map::class.java)
        } catch (e: Exception) {
            logger.error(e) { "[Toss] API 호출 실패: url=$url" }
            throw BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Toss Payments API 호출 실패: ${e.message}")
        }
    }

    private fun extractData(
        response: Map<*, *>?,
        operation: String,
    ): Map<*, *> {
        return response?.get("data") as? Map<*, *>
            ?: response
            ?: throw BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Toss $operation 응답 파싱 실패")
    }
}
