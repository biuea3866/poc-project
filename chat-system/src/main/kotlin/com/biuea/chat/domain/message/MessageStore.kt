package com.biuea.chat.domain.message

/**
 * 메시지 영속화. 오프라인 수신자 대비와 히스토리 조회를 위해 항상 저장한다.
 */
interface MessageStore {
    fun save(message: ChatMessage)
}
