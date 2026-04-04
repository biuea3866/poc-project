package com.closet.shipping.application

import com.closet.common.exception.BusinessException
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.shipping.application.carrier.CarrierAdapterFactory
import com.closet.shipping.domain.Shipment
import com.closet.shipping.domain.ShipmentRepository
import com.closet.shipping.domain.ShippingTrackingLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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

    val shippingService = ShippingService(
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

            val response = shippingService.prepareShipment(
                PrepareShipmentRequest(
                    orderId = 1L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "홍길동",
                    receiverPhone = "010-1234-5678",
                    zipCode = "06000",
                    address = "서울시 강남구",
                    detailAddress = "101동 201호",
                )
            )

            Then("배송 정보 저장 성공") {
                response.orderId shouldBe 1L
                response.status shouldBe "READY"
                response.receiverName shouldBe "홍길동"
            }
        }

        When("이미 배송 정보가 존재하는 경우") {
            val existingShipment = Shipment.create(
                orderId = 2L, sellerId = 1L, memberId = 1L,
                receiverName = "기존고객", receiverPhone = "010-0000-0000",
                zipCode = "06000", address = "서울", detailAddress = "101호",
            )
            every { shipmentRepository.findByOrderId(2L) } returns Optional.of(existingShipment)

            val response = shippingService.prepareShipment(
                PrepareShipmentRequest(
                    orderId = 2L, sellerId = 1L, memberId = 1L,
                    receiverName = "새고객", receiverPhone = "010-1111-1111",
                    zipCode = "06000", address = "부산", detailAddress = "201호",
                )
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
                        RegisterShipmentRequest(orderId = 999L, carrier = "CJ")
                    )
                }
            }
        }
    }
})
