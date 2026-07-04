package com.biuea.chat.infrastructure.redis

import com.biuea.chat.domain.room.RoomRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * 방 멤버십을 Redis Set 으로 공유한다. 인스턴스마다 로컬로 들고 있지 않으므로,
 * 어느 인스턴스에서 발신하든 같은 멤버 목록을 본다 (멀티 인스턴스 그룹 채팅의 전제).
 */
@Component
class RedisRoomRepository(
    private val redisTemplate: StringRedisTemplate,
) : RoomRepository {
    override fun membersOf(roomId: Long): Set<Long> =
        redisTemplate.opsForSet().members(key(roomId))
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    fun join(roomId: Long, userId: Long) {
        redisTemplate.opsForSet().add(key(roomId), userId.toString())
    }

    fun leave(roomId: Long, userId: Long) {
        redisTemplate.opsForSet().remove(key(roomId), userId.toString())
    }

    private fun key(roomId: Long) = "room:$roomId:members"
}
