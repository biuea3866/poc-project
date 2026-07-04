package com.biuea.chat.support

import com.biuea.chat.domain.connection.ChatConnection
import com.biuea.chat.domain.connection.ConnectionRegistry
import com.biuea.chat.domain.message.ChatMessage
import com.biuea.chat.domain.message.MessageStore
import com.biuea.chat.domain.room.RoomRepository
import com.biuea.chat.domain.routing.DeliveryEnvelope
import com.biuea.chat.domain.routing.MessageBroker
import com.biuea.chat.domain.routing.SessionLocator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class RecordingConnection(override val userId: Long) : ChatConnection {
    val received = CopyOnWriteArrayList<ChatMessage>()
    override fun send(message: ChatMessage) {
        received.add(message)
    }
}

class FakeConnectionRegistry : ConnectionRegistry {
    private val connections = ConcurrentHashMap<Long, ChatConnection>()
    override fun bind(connection: ChatConnection) {
        connections[connection.userId] = connection
    }
    override fun unbind(userId: Long) {
        connections.remove(userId)
    }
    override fun find(userId: Long): ChatConnection? = connections[userId]
}

class FakeSessionLocator : SessionLocator {
    private val locations = mutableMapOf<Long, String>()
    override fun register(userId: Long, serverId: String) {
        locations[userId] = serverId
    }
    override fun locate(userId: Long): String? = locations[userId]
    override fun deregister(userId: Long) {
        locations.remove(userId)
    }
}

class FakeMessageBroker : MessageBroker {
    val forwarded = mutableListOf<Pair<String, DeliveryEnvelope>>()
    override fun forward(serverId: String, envelope: DeliveryEnvelope) {
        forwarded.add(serverId to envelope)
    }
}

class FakeRoomRepository(private val members: Map<Long, Set<Long>>) : RoomRepository {
    override fun membersOf(roomId: Long): Set<Long> = members[roomId] ?: emptySet()
}

class RecordingMessageStore : MessageStore {
    val saved = mutableListOf<ChatMessage>()
    override fun save(message: ChatMessage) {
        saved.add(message)
    }
}
