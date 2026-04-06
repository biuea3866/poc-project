package com.closet.bff.facade

import com.closet.bff.client.ShippingServiceClient
import com.closet.bff.dto.ExchangeRequestResponse
import com.closet.bff.dto.ReturnRequestBffResponse
import com.closet.bff.dto.ShipmentBffResponse
import com.closet.bff.dto.TrackingLogBffResponse
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

/**
 * 배송 BFF Facade (CP-30).
 *
 * shipping-service(8088)의 배송/반품/교환 정보를 집계한다.
 */
@Service
class ShippingBffFacade(
    private val shippingClient: ShippingServiceClient,
) {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    data class ShippingDetailBffResponse(
        val shipment: ShipmentBffResponse?,
        val trackingLogs: List<TrackingLogBffResponse>,
        val returns: List<ReturnRequestBffResponse>,
        val exchanges: List<ExchangeRequestResponse>,
    )

    /**
     * 주문별 배송 상세 (배송 + 추적 + 반품 + 교환 집계).
     */
    fun getShippingDetail(orderId: Long): ShippingDetailBffResponse {
        val shipmentFuture =
            CompletableFuture.supplyAsync(
                { runCatching { shippingClient.getShipmentByOrderId(orderId) }.getOrNull() },
                executor,
            )
        val returnsFuture =
            CompletableFuture.supplyAsync(
                { runCatching { shippingClient.getReturnsByOrderId(orderId) }.getOrNull() },
                executor,
            )
        val exchangesFuture =
            CompletableFuture.supplyAsync(
                { runCatching { shippingClient.getExchangesByOrderId(orderId) }.getOrNull() },
                executor,
            )

        CompletableFuture.allOf(shipmentFuture, returnsFuture, exchangesFuture).join()

        val shipment = shipmentFuture.get()?.data
        val trackingLogs =
            if (shipment != null) {
                runCatching { shippingClient.getTrackingLogs(shipment.id) }.getOrNull()?.data ?: emptyList()
            } else {
                emptyList()
            }

        return ShippingDetailBffResponse(
            shipment = shipment,
            trackingLogs = trackingLogs,
            returns = returnsFuture.get()?.data ?: emptyList(),
            exchanges = exchangesFuture.get()?.data ?: emptyList(),
        )
    }
}
