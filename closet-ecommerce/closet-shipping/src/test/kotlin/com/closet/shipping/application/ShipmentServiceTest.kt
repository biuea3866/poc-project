package com.closet.shipping.application

import com.closet.common.exception.BusinessException
import com.closet.shipping.domain.Carrier
import com.closet.shipping.domain.Shipment
import com.closet.shipping.domain.ShipmentStatus
import com.closet.shipping.domain.ShipmentStatusHistory
import com.closet.shipping.presentation.dto.CreateShipmentRequest
import com.closet.shipping.repository.ShipmentRepository
import com.closet.shipping.repository.ShipmentStatusHistoryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional

class ShipmentServiceTest : BehaviorSpec({

    val shipmentRepository = mockk<ShipmentRepository>()
    val shipmentStatusHistoryRepository = mockk<ShipmentStatusHistoryRepository>()

    val shipmentService = ShipmentService(
        shipmentRepository = shipmentRepository,
        shipmentStatusHistoryRepository = shipmentStatusHistoryRepository,
    )

    Given("배송 생성") {
        val request = CreateShipmentRequest(
            orderId = 1L,
            sellerId = 10L,
            receiverName = "홍길동",
            receiverPhone = "010-1234-5678",
            address = "서울시 강남구 역삼동 123-4",
        )

        val shipmentSlot = slot<Shipment>()
        val historySlot = slot<ShipmentStatusHistory>()

        every { shipmentRepository.save(capture(shipmentSlot)) } answers { shipmentSlot.captured }
        every { shipmentStatusHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }

        When("정상 배송 생성 요청") {
            val result = shipmentService.createShipment(request)

            Then("PENDING 상태로 생성된다") {
                result.status shouldBe ShipmentStatus.PENDING
            }

            Then("수령인 정보가 올바르게 설정된다") {
                result.receiverName shouldBe "홍길동"
                result.receiverPhone shouldBe "010-1234-5678"
                result.address shouldBe "서울시 강남구 역삼동 123-4"
            }

            Then("상태 이력이 저장된다") {
                verify(atLeast = 1) { shipmentStatusHistoryRepository.save(any()) }
            }
        }
    }

    Given("송장 등록") {
        val shipment = Shipment.create(
            orderId = 1L,
            sellerId = 10L,
            receiverName = "홍길동",
            receiverPhone = "010-1234-5678",
            address = "서울시 강남구 역삼동 123-4",
        )

        every { shipmentRepository.findById(any()) } returns Optional.of(shipment)
        every { shipmentStatusHistoryRepository.save(any()) } answers { firstArg() }

        When("PENDING 상태에서 송장 등록") {
            val result = shipmentService.registerTracking(1L, Carrier.CJ, "1234567890")

            Then("READY 상태로 변경된다") {
                result.status shouldBe ShipmentStatus.READY
            }

            Then("택배사와 송장번호가 설정된다") {
                result.carrier shouldBe Carrier.CJ
                result.trackingNumber shouldBe "1234567890"
            }
        }
    }

    Given("배송 상태 변경") {
        When("존재하지 않는 배송 ID로 상태 변경 시도") {
            every { shipmentRepository.findById(999L) } returns Optional.empty()

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    shipmentService.updateStatus(999L, ShipmentStatus.PICKED_UP)
                }
            }
        }
    }
})
