package com.closet.shipping.application

import com.closet.common.exception.BusinessException
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.shipping.application.carrier.CarrierAdapter
import com.closet.shipping.application.carrier.CarrierAdapterFactory
import com.closet.shipping.domain.Shipment
import com.closet.shipping.domain.ShipmentRepository
import com.closet.shipping.domain.ShippingStatus
import com.closet.shipping.domain.ShippingTrackingLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.Optional

class ShippingServiceTest : BehaviorSpec({

    val shipmentRepository = mockk<ShipmentRepository>(relaxed = true)
    val trackingLogRepository = mockk<ShippingTrackingLogRepository>(relaxed = true)
    val carrierAdapterFactory = mockk<CarrierAdapterFactory>()
    val outboxEventPublisher = mockk<OutboxEventPublisher>(relaxed = true)
    val objectMapper = ObjectMapper()
    val redisTemplate = mockk<StringRedisTemplate>()
    val valueOps = mockk<ValueOperations<String, String>>(relaxed = true)

    every { redisTemplate.opsForValue() } returns valueOps

    val shippingService =
        ShippingService(
            shipmentRepository,
            trackingLogRepository,
            carrierAdapterFactory,
            outboxEventPublisher,
            objectMapper,
            redisTemplate,
        )

    Given("배송 준비 정보 사전 저장") {

        When("신규 주문에 대해 prepareShipment 호출") {
            every { shipmentRepository.findByOrderId(1L) } returns Optional.empty()
            every { shipmentRepository.save(any()) } answers { firstArg() }

            val response =
                shippingService.prepareShipment(
                    PrepareShipmentRequest(
                        orderId = 1L,
                        sellerId = 1L,
                        memberId = 1L,
                        receiverName = "홍길동",
                        receiverPhone = "010-1234-5678",
                        zipCode = "06000",
                        address = "서울시 강남구",
                        detailAddress = "101동 201호",
                    ),
                )

            Then("배송 정보 저장 성공") {
                response.orderId shouldBe 1L
                response.status shouldBe "READY"
                response.receiverName shouldBe "홍길동"
            }
        }

        When("이미 배송 정보가 존재하는 경우") {
            val existingShipment =
                Shipment.create(
                    orderId = 2L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "기존고객",
                    receiverPhone = "010-0000-0000",
                    zipCode = "06000",
                    address = "서울",
                    detailAddress = "101호",
                )
            every { shipmentRepository.findByOrderId(2L) } returns Optional.of(existingShipment)

            val response =
                shippingService.prepareShipment(
                    PrepareShipmentRequest(
                        orderId = 2L,
                        sellerId = 1L,
                        memberId = 1L,
                        receiverName = "새고객",
                        receiverPhone = "010-1111-1111",
                        zipCode = "06000",
                        address = "부산",
                        detailAddress = "201호",
                    ),
                )

            Then("기존 배송 정보 반환") {
                response.orderId shouldBe 2L
                response.receiverName shouldBe "기존고객"
            }
        }
    }

    Given("송장 등록") {

        When("존재하지 않는 주문으로 송장 등록") {
            every { shipmentRepository.findByOrderId(999L) } returns Optional.empty()

            Then("BusinessException 발생") {
                shouldThrow<BusinessException> {
                    shippingService.registerShipment(
                        RegisterShipmentRequest(orderId = 999L, carrier = "CJ"),
                    )
                }
            }
        }

        When("이미 송장이 등록된 주문으로 중복 등록 시도") {
            val shipment =
                Shipment.create(
                    orderId = 10L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "홍길동",
                    receiverPhone = "010-1234-5678",
                    zipCode = "06000",
                    address = "서울",
                    detailAddress = "101호",
                )
            shipment.registerTracking("CJ", "CJ1234567890")
            every { shipmentRepository.findByOrderId(10L) } returns Optional.of(shipment)

            Then("BusinessException 발생 (중복 등록 에러)") {
                shouldThrow<BusinessException> {
                    shippingService.registerShipment(
                        RegisterShipmentRequest(orderId = 10L, carrier = "CJ", trackingNumber = "CJ9999999999"),
                    )
                }
            }
        }

        When("정상 수동 송장 등록") {
            val shipment =
                Shipment.create(
                    orderId = 20L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "김철수",
                    receiverPhone = "010-9876-5432",
                    zipCode = "06000",
                    address = "서울시 서초구",
                    detailAddress = "서초동",
                )
            every { shipmentRepository.findByOrderId(20L) } returns Optional.of(shipment)
            every { shipmentRepository.save(any()) } answers { firstArg() }

            val mockAdapter = mockk<CarrierAdapter>()
            every { mockAdapter.validateTrackingNumber("CJ1234567890") } returns true
            every { carrierAdapterFactory.getAdapter("CJ") } returns mockAdapter

            val response =
                shippingService.registerShipment(
                    RegisterShipmentRequest(orderId = 20L, carrier = "CJ", trackingNumber = "CJ1234567890"),
                )

            Then("송장 등록 성공") {
                response.carrier shouldBe "CJ"
                response.trackingNumber shouldBe "CJ1234567890"
            }

            Then("ShippingStatusChanged 이벤트 발행됨") {
                verify(atLeast = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "Shipment",
                        aggregateId = any(),
                        eventType = "ShippingStatusChanged",
                        topic = "event.closet.shipping",
                        partitionKey = any(),
                        payload = any(),
                    )
                }
            }

            Then("OrderShippingStarted 이벤트 발행됨 (ORDER 토픽)") {
                verify(atLeast = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "Shipment",
                        aggregateId = any(),
                        eventType = "OrderShippingStarted",
                        topic = "event.closet.order",
                        partitionKey = any(),
                        payload = any(),
                    )
                }
            }
        }

        When("유효하지 않은 송장번호 형식") {
            val shipment =
                Shipment.create(
                    orderId = 30L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "박영희",
                    receiverPhone = "010-1111-2222",
                    zipCode = "06000",
                    address = "서울",
                    detailAddress = "101호",
                )
            every { shipmentRepository.findByOrderId(30L) } returns Optional.of(shipment)

            val mockAdapter = mockk<CarrierAdapter>()
            every { mockAdapter.validateTrackingNumber("INVALID") } returns false
            every { carrierAdapterFactory.getAdapter("CJ") } returns mockAdapter

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    shippingService.registerShipment(
                        RegisterShipmentRequest(orderId = 30L, carrier = "CJ", trackingNumber = "INVALID"),
                    )
                }
            }
        }
    }

    Given("수동 구매확정") {

        When("DELIVERED 상태에서 구매확정") {
            val shipment =
                Shipment.create(
                    orderId = 40L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "홍길동",
                    receiverPhone = "010-1234-5678",
                    zipCode = "06000",
                    address = "서울",
                    detailAddress = "101호",
                )
            shipment.updateStatus(ShippingStatus.IN_TRANSIT)
            shipment.updateStatus(ShippingStatus.DELIVERED)
            every { shipmentRepository.findByOrderId(40L) } returns Optional.of(shipment)

            val response = shippingService.confirmOrder(40L)

            Then("성공") {
                response.orderId shouldBe 40L
            }

            Then("OrderConfirmed 이벤트 발행됨") {
                verify(atLeast = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "Shipment",
                        aggregateId = any(),
                        eventType = "OrderConfirmed",
                        topic = "event.closet.order",
                        partitionKey = "40",
                        payload = any(),
                    )
                }
            }
        }

        When("DELIVERED가 아닌 상태에서 구매확정") {
            val shipment =
                Shipment.create(
                    orderId = 41L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "홍길동",
                    receiverPhone = "010-1234-5678",
                    zipCode = "06000",
                    address = "서울",
                    detailAddress = "101호",
                )
            every { shipmentRepository.findByOrderId(41L) } returns Optional.of(shipment)

            Then("BusinessException 발생") {
                shouldThrow<BusinessException> {
                    shippingService.confirmOrder(41L)
                }
            }
        }
    }

    Given("자동 구매확정") {

        val returnService = mockk<ReturnService>()
        val exchangeService = mockk<ExchangeService>()

        When("배송 완료 7일 경과, 반품/교환 없는 건") {
            val shipment =
                Shipment.create(
                    orderId = 50L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "홍길동",
                    receiverPhone = "010-1234-5678",
                    zipCode = "06000",
                    address = "서울",
                    detailAddress = "101호",
                )
            shipment.updateStatus(ShippingStatus.IN_TRANSIT)
            shipment.updateStatus(ShippingStatus.DELIVERED)

            every { shipmentRepository.findByStatusAndDeliveredAtLessThanEqual(any(), any()) } returns listOf(shipment)
            every { returnService.hasActiveReturnRequest(50L) } returns false
            every { exchangeService.hasActiveExchangeRequest(50L) } returns false

            shippingService.autoConfirmOrders(returnService, exchangeService)

            Then("OrderConfirmed 이벤트 발행됨") {
                verify(atLeast = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "Shipment",
                        aggregateId = any(),
                        eventType = "OrderConfirmed",
                        topic = "event.closet.order",
                        partitionKey = "50",
                        payload = any(),
                    )
                }
            }
        }

        When("반품 진행 중인 건") {
            val shipment =
                Shipment.create(
                    orderId = 51L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "홍길동",
                    receiverPhone = "010-1234-5678",
                    zipCode = "06000",
                    address = "서울",
                    detailAddress = "101호",
                )
            shipment.updateStatus(ShippingStatus.IN_TRANSIT)
            shipment.updateStatus(ShippingStatus.DELIVERED)

            every { shipmentRepository.findByStatusAndDeliveredAtLessThanEqual(any(), any()) } returns listOf(shipment)
            every { returnService.hasActiveReturnRequest(51L) } returns true
            every { exchangeService.hasActiveExchangeRequest(51L) } returns false

            shippingService.autoConfirmOrders(returnService, exchangeService)

            Then("OrderConfirmed 이벤트 발행되지 않음") {
                verify(exactly = 0) {
                    outboxEventPublisher.publish(
                        aggregateType = "Shipment",
                        aggregateId = any(),
                        eventType = "OrderConfirmed",
                        topic = "event.closet.order",
                        partitionKey = "51",
                        payload = any(),
                    )
                }
            }
        }
    }
})
