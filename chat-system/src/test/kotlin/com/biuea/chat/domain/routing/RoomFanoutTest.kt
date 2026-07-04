package com.biuea.chat.domain.routing

import com.biuea.chat.support.FakeSessionLocator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe

class RoomFanoutTest : FunSpec({

    test("수신자를 접속 서버별로 묶는다") {
        val locator = FakeSessionLocator().apply {
            register(2, "server-1")
            register(3, "server-2")
            register(4, "server-2")
        }

        val grouped = RoomFanout.groupByServer(listOf(2, 3, 4), locator)

        grouped shouldHaveSize 2
        grouped["server-1"]!! shouldContainExactlyInAnyOrder listOf(2)
        grouped["server-2"]!! shouldContainExactlyInAnyOrder listOf(3, 4)
    }

    test("오프라인 수신자는 제외한다") {
        val locator = FakeSessionLocator().apply {
            register(2, "server-1")
            // 3 은 등록하지 않음 = 오프라인
        }

        val grouped = RoomFanout.groupByServer(listOf(2, 3), locator)

        grouped shouldHaveSize 1
        grouped["server-1"]!! shouldContainExactlyInAnyOrder listOf(2)
    }

    test("1000명이 3개 서버에 흩어져 있으면 서버 수만큼(3개)으로 묶인다") {
        val locator = FakeSessionLocator()
        val recipients = (1L..1000L).toList()
        recipients.forEach { userId -> locator.register(userId, "server-${userId % 3}") }

        val grouped = RoomFanout.groupByServer(recipients, locator)

        // 팬아웃 증폭 완화: 1000건 전달이 아니라 서버 3개로 묶인다
        grouped shouldHaveSize 3
        grouped.values.sumOf { it.size } shouldBe 1000
    }

    test("수신자가 없으면 빈 결과다") {
        RoomFanout.groupByServer(emptyList(), FakeSessionLocator()) shouldBe emptyMap()
    }
})
