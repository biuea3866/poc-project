package com.biuea.chat.domain.message

/**
 * 메시지의 수신 대상. 일대일(Direct)과 그룹(Room)을 구분한다.
 */
sealed interface MessageTarget {
    data class Direct(val receiverId: Long) : MessageTarget
    data class Room(val roomId: Long) : MessageTarget
}
