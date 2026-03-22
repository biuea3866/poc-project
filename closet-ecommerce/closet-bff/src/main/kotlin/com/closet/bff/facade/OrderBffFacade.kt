package com.closet.bff.facade

import com.closet.bff.client.MemberServiceClient
import com.closet.bff.client.OrderServiceClient
import com.closet.bff.client.PaymentServiceClient
import com.closet.bff.dto.CheckoutBffResponse
import com.closet.bff.dto.ConfirmPaymentBffRequest
import com.closet.bff.dto.ConfirmPaymentRequest
import com.closet.bff.dto.CreateOrderBffRequest
import com.closet.bff.dto.OrderDetailBffResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class OrderBffFacade(
    private val orderClient: OrderServiceClient,
    private val paymentClient: PaymentServiceClient,
    private val memberClient: MemberServiceClient,
) {
    fun getOrderDetail(orderId: Long): OrderDetailBffResponse {
        val order = orderClient.getOrder(orderId).block()!!
        val payment = paymentClient.getPaymentByOrderId(orderId).block()

        return OrderDetailBffResponse(
            order = order,
            payment = payment,
            shipment = null, // Phase 2
        )
    }

    fun getCheckout(memberId: Long): CheckoutBffResponse {
        // Parallel calls using Mono.zip
        val cartMono = orderClient.getCart(memberId)
        val addressesMono = memberClient.getAddresses(memberId)

        val result = Mono.zip(cartMono, addressesMono).block()!!
        val cart = result.t1
        val addresses = result.t2

        return CheckoutBffResponse(
            cart = cart,
            addresses = addresses,
            defaultAddress = addresses.find { it.isDefault },
            availableCoupons = null, // Phase 3
        )
    }

    fun placeOrder(memberId: Long, request: CreateOrderBffRequest): OrderDetailBffResponse {
        val order = orderClient.createOrder(memberId, request).block()!!
        return OrderDetailBffResponse(
            order = order,
            payment = null,
            shipment = null,
        )
    }

    fun confirmPayment(request: ConfirmPaymentBffRequest): OrderDetailBffResponse {
        val paymentRequest = ConfirmPaymentRequest(
            paymentKey = request.paymentKey,
            orderId = request.orderId,
            amount = request.amount,
        )
        val payment = paymentClient.confirmPayment(paymentRequest).block()!!
        val order = orderClient.getOrder(request.orderId).block()!!
        return OrderDetailBffResponse(
            order = order,
            payment = payment,
            shipment = null,
        )
    }

    fun cancelOrder(orderId: Long, reason: String): OrderDetailBffResponse {
        val order = orderClient.cancelOrder(orderId, reason).block()!!
        val payment = paymentClient.getPaymentByOrderId(orderId).block()
        return OrderDetailBffResponse(
            order = order,
            payment = payment,
            shipment = null,
        )
    }
}
