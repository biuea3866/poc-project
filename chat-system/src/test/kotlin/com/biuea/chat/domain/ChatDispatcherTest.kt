package com.biuea.chat.domain

import com.biuea.chat.domain.message.ChatMessage
import com.biuea.chat.support.FakeConnectionRegistry
import com.biuea.chat.support.FakeMessageBroker
import com.biuea.chat.support.FakeRoomRepository
import com.biuea.chat.support.FakeSessionLocator
import com.biuea.chat.support.RecordingConnection
import com.biuea.chat.support.RecordingMessageStore
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private const val THIS_SERVER = "server-1"

private fun dispatcher(
    registry: FakeConnectionRegistry = FakeConnectionRegistry(),
    locator: FakeSessionLocator = FakeSessionLocator(),
    broker: FakeMessageBroker = FakeMessageBroker(),
    rooms: FakeRoomRepository = FakeRoomRepository(emptyMap()),
    store: RecordingMessageStore = RecordingMessageStore(),
) = ChatDispatcher(THIS_SERVER, registry, locator, broker, rooms, store)

class ChatDispatcherTest : FunSpec({

    test("서버 1대: 수신자가 이 서버에 붙어 있으면 로컬로 직접 전달한다") {
        val registry = FakeConnectionRegistry()
        val receiver = RecordingConnection(2).also { registry.bind(it) }
        val locator = FakeSessionLocator().apply { register(2, THIS_SERVER) }
        val broker = FakeMessageBroker()
        val store = RecordingMessageStore()
        val sut = dispatcher(registry = registry, locator = locator, broker = broker, store = store)

        sut.dispatch(ChatMessage.direct(senderId = 1, receiverId = 2, content = "hi"))

        receiver.received shouldHaveSize 1
        receiver.received.first().content shouldBe "hi"
        broker.forwarded shouldHaveSize 0
        store.saved shouldHaveSize 1
    }

    test("서버 다수: 수신자가 다른 서버면 그 서버로만 지향 전달한다 (브로드캐스트 아님)") {
        val locator = FakeSessionLocator().apply { register(2, "server-7") }
        val broker = FakeMessageBroker()
        val sut = dispatcher(locator = locator, broker = broker)

        sut.dispatch(ChatMessage.direct(senderId = 1, receiverId = 2, content = "hi"))

        broker.forwarded shouldHaveSize 1
        val (targetServer, envelope) = broker.forwarded.first()
        targetServer shouldBe "server-7"
        envelope.recipientIds shouldContainExactlyInAnyOrder listOf(2L)
    }

    test("수신자가 오프라인이면 저장만 하고 전달하지 않는다") {
        val broker = FakeMessageBroker()
        val store = RecordingMessageStore()
        val sut = dispatcher(broker = broker, store = store)

        sut.dispatch(ChatMessage.direct(senderId = 1, receiverId = 2, content = "hi"))

        store.saved shouldHaveSize 1
        broker.forwarded shouldHaveSize 0
    }

    test("그룹 3명: 발신자를 제외한 나머지 멤버에게 전달한다") {
        val registry = FakeConnectionRegistry()
        val b = RecordingConnection(2).also { registry.bind(it) }
        val c = RecordingConnection(3).also { registry.bind(it) }
        val locator = FakeSessionLocator().apply {
            register(2, THIS_SERVER)
            register(3, THIS_SERVER)
        }
        val rooms = FakeRoomRepository(mapOf(10L to setOf(1L, 2L, 3L)))
        val sut = dispatcher(registry = registry, locator = locator, rooms = rooms)

        sut.dispatch(ChatMessage.room(senderId = 1, roomId = 10, content = "hello room"))

        b.received shouldHaveSize 1
        c.received shouldHaveSize 1
    }

    test("그룹 대규모: 서버당 한 번만 전달한다 (서버 단위 팬아웃)") {
        val registry = FakeConnectionRegistry()
        val local = RecordingConnection(2).also { registry.bind(it) }
        val locator = FakeSessionLocator().apply {
            register(2, THIS_SERVER)   // 로컬
            register(3, "server-2")    // 원격
            register(4, "server-2")    // 원격 (같은 서버)
            register(5, "server-3")    // 원격
        }
        val rooms = FakeRoomRepository(mapOf(10L to setOf(1L, 2L, 3L, 4L, 5L)))
        val broker = FakeMessageBroker()
        val sut = dispatcher(registry = registry, locator = locator, rooms = rooms, broker = broker)

        sut.dispatch(ChatMessage.room(senderId = 1, roomId = 10, content = "broadcast"))

        // 로컬 수신자는 직접 전달
        local.received shouldHaveSize 1
        // 원격은 서버당 1건 = server-2 1건 + server-3 1건 (수신자 3건이 아니라 2건)
        broker.forwarded shouldHaveSize 2
        val byServer = broker.forwarded.toMap()
        byServer["server-2"]!!.recipientIds shouldContainExactlyInAnyOrder listOf(3L, 4L)
        byServer["server-3"]!!.recipientIds shouldContainExactlyInAnyOrder listOf(5L)
    }
})
