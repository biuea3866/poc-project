package com.biuea.chat.domain.routing

/**
 * 세션 레지스트리: userId 가 어느 서버에 붙어 있는지 매핑한다 (서버 다수 단계의 지향 전달 근거).
 */
interface SessionLocator {
    fun register(userId: Long, serverId: String)
    fun locate(userId: Long): String?
    fun deregister(userId: Long)
}
