package com.closet.order.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.vo.Money
import com.closet.order.domain.event.OrderCancelledEvent
import com.closet.order.domain.event.OrderCreatedEvent
import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderItem
import com.closet.order.domain.order.OrderStatus
import com.closet.order.domain.order.OrderStatusHistory
import com.closet.order.presentation.dto.CreateOrderRequest
import com.closet.order.presentation.dto.OrderResponse
import com.closet.order.repository.OrderItemRepository
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.OrderStatusHistoryRepository
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        val items =
            request.items.map { itemReq ->
                OrderItem.create(
                    productId = itemReq.productId,
                    productOptionId = itemReq.productOptionId,
                    productName = itemReq.productName,
                    optionName = itemReq.optionName,
                    categoryId = itemReq.categoryId,
                    quantity = itemReq.quantity,
                    unitPrice = Money(itemReq.unitPrice),
                )
            }

        val order =
            Order.create(
                memberId = request.memberId,
                sellerId = request.sellerId,
                items = items,
                receiverName = request.receiverName,
                receiverPhone = request.receiverPhone,
                zipCode = request.zipCode,
                address = request.address,
                detailAddress = request.detailAddress,
                shippingFee = Money(request.shippingFee),
                discountAmount = Money(request.discountAmount),
            )

        order.place()
        val savedOrder = orderRepository.save(order)

        val savedItems =
            items.map { item ->
                orderItemRepository.save(
                    OrderItem.create(
                        orderId = savedOrder.id,
                        productId = item.productId,
                        productOptionId = item.productOptionId,
                        productName = item.productName,
                        optionName = item.optionName,
                        categoryId = item.categoryId,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                    ),
                )
            }

        orderStatusHistoryRepository.save(
            OrderStatusHistory.create(
                orderId = savedOrder.id,
                fromStatus = null,
                toStatus = savedOrder.status,
            ),
        )

        eventPublisher.publishEvent(
            OrderCreatedEvent(
                orderId = savedOrder.id,
                memberId = savedOrder.memberId,
                items =
                    savedItems.map {
                        OrderCreatedEvent.OrderItemInfo(
                            productOptionId = it.productOptionId,
                            quantity = it.quantity,
                        )
                    },
            ),
        )

        logger.info { "주문 생성 완료: orderId=${savedOrder.id}, orderNumber=${savedOrder.orderNumber}" }
        return OrderResponse.from(savedOrder, savedItems)
    }

    fun findById(id: Long): OrderResponse {
        val order =
            orderRepository.findByIdAndDeletedAtIsNull(id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "주문을 찾을 수 없습니다. id=$id")
        val items = orderItemRepository.findByOrderId(order.id)
        return OrderResponse.from(order, items)
    }

    fun findByMemberId(
        memberId: Long,
        pageable: Pageable,
    ): Page<OrderResponse> {
        return orderRepository.findByMemberIdAndDeletedAtIsNull(memberId, pageable)
            .map { order ->
                val items = orderItemRepository.findByOrderId(order.id)
                OrderResponse.from(order, items)
            }
    }

    @Transactional
    fun cancelOrder(
        id: Long,
        reason: String,
    ): OrderResponse {
        val order =
            orderRepository.findByIdAndDeletedAtIsNull(id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "주문을 찾을 수 없습니다. id=$id")

        val previousStatus = order.status
        order.cancel(reason)

        val items = orderItemRepository.findByOrderId(order.id)
        items.forEach { it.cancel() }

        orderStatusHistoryRepository.save(
            OrderStatusHistory.create(
                orderId = order.id,
                fromStatus = previousStatus,
                toStatus = order.status,
                reason = reason,
            ),
        )

        eventPublisher.publishEvent(
            OrderCancelledEvent(
                orderId = order.id,
                reason = reason,
                items =
                    items.map {
                        OrderCancelledEvent.OrderItemInfo(
                            productOptionId = it.productOptionId,
                            quantity = it.quantity,
                        )
                    },
            ),
        )

        logger.info { "주문 취소 완료: orderId=${order.id}, reason=$reason" }
        return OrderResponse.from(order, items)
    }

    @Transactional
    fun syncStatusFromShipping(
        orderId: Long,
        shippingStatus: String,
    ) {
        val order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
        if (order == null) {
            logger.warn { "주문을 찾을 수 없습니다. orderId=$orderId" }
            return
        }

        if (order.status.isTerminal()) {
            logger.info { "터미널 상태 주문은 무시합니다. orderId=$orderId, status=${order.status}" }
            return
        }

        val targetStatus = mapShippingStatusToOrderStatus(shippingStatus)
        if (targetStatus == null) {
            logger.warn { "알 수 없는 배송 상태: $shippingStatus" }
            return
        }

        if (!order.status.canTransitionTo(targetStatus)) {
            logger.warn { "잘못된 상태 전이 시도: ${order.status} -> $targetStatus, orderId=$orderId" }
            return
        }

        val previousStatus = order.status
        when (targetStatus) {
            OrderStatus.PREPARING -> order.prepare()
            OrderStatus.SHIPPED -> order.ship()
            OrderStatus.DELIVERED -> order.deliver()
            else -> {
                logger.warn { "처리할 수 없는 대상 상태: $targetStatus" }
                return
            }
        }

        orderStatusHistoryRepository.save(
            OrderStatusHistory.create(
                orderId = order.id,
                fromStatus = previousStatus,
                toStatus = order.status,
                changedBy = "shipping-service",
            ),
        )

        logger.info { "주문 상태 동기화 완료: orderId=$orderId, $previousStatus -> ${order.status}" }
    }

    @Transactional
    fun failByInventoryInsufficient(
        orderId: Long,
        reason: String?,
    ) {
        val order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
        if (order == null) {
            logger.warn { "주문을 찾을 수 없습니다. orderId=$orderId" }
            return
        }

        if (order.status.isTerminal()) {
            logger.info { "터미널 상태 주문은 무시합니다. orderId=$orderId, status=${order.status}" }
            return
        }

        if (order.status != OrderStatus.STOCK_RESERVED) {
            logger.warn { "STOCK_RESERVED 상태가 아닌 주문에 재고 부족 이벤트 수신: orderId=$orderId, status=${order.status}" }
            return
        }

        val previousStatus = order.status
        order.fail()

        orderStatusHistoryRepository.save(
            OrderStatusHistory.create(
                orderId = order.id,
                fromStatus = previousStatus,
                toStatus = order.status,
                reason = reason,
                changedBy = "inventory-service",
            ),
        )

        logger.info { "주문 FAILED 처리 완료: orderId=$orderId, reason=$reason" }
    }

    private fun mapShippingStatusToOrderStatus(shippingStatus: String): OrderStatus? {
        return when (shippingStatus) {
            "READY" -> OrderStatus.PREPARING
            "IN_TRANSIT" -> OrderStatus.SHIPPED
            "DELIVERED" -> OrderStatus.DELIVERED
            else -> null
        }
    }
}
