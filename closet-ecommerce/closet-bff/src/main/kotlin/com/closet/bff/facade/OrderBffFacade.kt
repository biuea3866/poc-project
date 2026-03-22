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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class OrderBffFacade(
    private val orderClient: OrderServiceClient,
    private val paymentClient: PaymentServiceClient,
    private val memberClient: MemberServiceClient,
) {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun getOrderDetail(orderId: Long): OrderDetailBffResponse {
        val orderFuture = CompletableFuture.supplyAsync(
            { orderClient.getOrder(orderId) },
            executor,
        )
        val paymentFuture = CompletableFuture.supplyAsync(
            { runCatching { paymentClient.getPaymentByOrderId(orderId) }.getOrNull() },
            executor,
        )

        CompletableFuture.allOf(orderFuture, paymentFuture).join()

        return OrderDetailBffResponse(
            order = orderFuture.get().data!!,
            payment = paymentFuture.get()?.data,
            shipment = null, // Phase 2
        )
    }

    fun getCheckout(memberId: Long): CheckoutBffResponse {
        val cartFuture = CompletableFuture.supplyAsync(
            { orderClient.getCart(memberId) },
            executor,
        )
        val addressesFuture = CompletableFuture.supplyAsync(
            { memberClient.getAddresses(memberId) },
            executor,
        )

        CompletableFuture.allOf(cartFuture, addressesFuture).join()

        val cart = cartFuture.get().data!!
        val addresses = addressesFuture.get().data ?: emptyList()

        return CheckoutBffResponse(
            cart = cart,
            addresses = addresses,
            defaultAddress = addresses.find { it.isDefault },
            availableCoupons = null, // Phase 3
        )
    }

    fun placeOrder(memberId: Long, request: CreateOrderBffRequest): OrderDetailBffResponse {
        val order = orderClient.createOrder(memberId, request).data!!
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
        val payment = paymentClient.confirmPayment(paymentRequest).data!!
        val order = orderClient.getOrder(request.orderId).data!!
        return OrderDetailBffResponse(
            order = order,
            payment = payment,
            shipment = null,
        )
    }

    fun cancelOrder(orderId: Long, reason: String): OrderDetailBffResponse {
        val order = orderClient.cancelOrder(orderId, mapOf("reason" to reason)).data!!
        val payment = runCatching { paymentClient.getPaymentByOrderId(orderId) }.getOrNull()?.data
        return OrderDetailBffResponse(
            order = order,
            payment = payment,
            shipment = null,
        )
    }
}
