package com.biuea.chat.infrastructure.config

import com.biuea.chat.domain.ChatDispatcher
import com.biuea.chat.domain.connection.ConnectionRegistry
import com.biuea.chat.domain.message.MessageStore
import com.biuea.chat.domain.room.RoomRepository
import com.biuea.chat.domain.routing.MessageBroker
import com.biuea.chat.domain.routing.SessionLocator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatConfig {
    @Bean
    fun chatDispatcher(
        @Value("\${server.id}") serverId: String,
        connectionRegistry: ConnectionRegistry,
        sessionLocator: SessionLocator,
        messageBroker: MessageBroker,
        roomRepository: RoomRepository,
        messageStore: MessageStore,
    ): ChatDispatcher = ChatDispatcher(
        serverId = serverId,
        connectionRegistry = connectionRegistry,
        sessionLocator = sessionLocator,
        messageBroker = messageBroker,
        roomRepository = roomRepository,
        messageStore = messageStore,
    )
}
