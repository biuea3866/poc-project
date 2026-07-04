package com.biuea.chat.infrastructure.memory

import com.biuea.chat.domain.message.ChatMessage
import com.biuea.chat.domain.message.MessageStore
import java.util.concurrent.CopyOnWriteArrayList
import org.springframework.stereotype.Component

@Component
class InMemoryMessageStore : MessageStore {
    private val saved = CopyOnWriteArrayList<ChatMessage>()

    override fun save(message: ChatMessage) {
        saved.add(message)
    }

    fun findAll(): List<ChatMessage> = saved.toList()
}
