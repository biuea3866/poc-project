package com.biuea.chat.infrastructure.memory

import com.biuea.chat.domain.room.RoomRepository
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component

@Component
class InMemoryRoomRepository : RoomRepository {
    private val rooms = ConcurrentHashMap<Long, MutableSet<Long>>()

    override fun membersOf(roomId: Long): Set<Long> = rooms[roomId]?.toSet() ?: emptySet()

    fun join(roomId: Long, userId: Long) {
        rooms.computeIfAbsent(roomId) { ConcurrentHashMap.newKeySet() }.add(userId)
    }
}
