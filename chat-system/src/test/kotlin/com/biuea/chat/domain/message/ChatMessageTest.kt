package com.biuea.chat.domain.message

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.time.ZonedDateTime

class ChatMessageTest : FunSpec({

    test("direct 메시지는 messageId 와 occurredAt 을 내부에서 생성한다") {
        val message = ChatMessage.direct(senderId = 1, receiverId = 2, content = "hi")

        message.messageId.shouldNotBeBlank()
        message.senderId shouldBe 1
        message.target shouldBe MessageTarget.Direct(2)
        message.content shouldBe "hi"
        message.occurredAt shouldNotBe null
    }

    test("room 메시지는 target 이 Room 이다") {
        val message = ChatMessage.room(senderId = 1, roomId = 99, content = "hello")

        message.target shouldBe MessageTarget.Room(99)
    }

    test("빈 내용은 거부한다") {
        shouldThrow<IllegalArgumentException> {
            ChatMessage.direct(senderId = 1, receiverId = 2, content = "  ")
        }
    }

    test("senderId 가 0 이하면 거부한다") {
        shouldThrow<IllegalArgumentException> {
            ChatMessage.direct(senderId = 0, receiverId = 2, content = "hi")
        }
    }

    test("reconstitute 는 원본 식별자와 시간을 그대로 보존한다") {
        val at = ZonedDateTime.parse("2026-07-04T10:15:30+09:00")

        val message = ChatMessage.reconstitute(
            messageId = "fixed-id",
            senderId = 1,
            target = MessageTarget.Direct(2),
            content = "hi",
            occurredAt = at,
        )

        message.messageId shouldBe "fixed-id"
        message.occurredAt shouldBe at
    }
})
