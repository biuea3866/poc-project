package com.biuea.chat.domain.connection

/**
 * 이 서버에 붙은 연결을 userId 로 관리한다 (서버 1대 단계의 인메모리 세션 맵).
 */
interface ConnectionRegistry {
    fun bind(connection: ChatConnection)
    fun unbind(userId: Long)
    fun find(userId: Long): ChatConnection?
}
