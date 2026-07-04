package com.biuea.chat.domain.room

/**
 * 방 멤버십. roomId 로 그 방에 속한 사용자 집합을 조회한다.
 */
interface RoomRepository {
    fun membersOf(roomId: Long): Set<Long>
}
