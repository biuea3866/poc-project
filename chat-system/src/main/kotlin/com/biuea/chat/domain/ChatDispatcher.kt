package com.biuea.chat.domain

import com.biuea.chat.domain.connection.ConnectionRegistry
import com.biuea.chat.domain.message.ChatMessage
import com.biuea.chat.domain.message.MessageStore
import com.biuea.chat.domain.message.MessageTarget
import com.biuea.chat.domain.room.RoomRepository
import com.biuea.chat.domain.routing.DeliveryEnvelope
import com.biuea.chat.domain.routing.MessageBroker
import com.biuea.chat.domain.routing.RoomFanout
import com.biuea.chat.domain.routing.SessionLocator

/**
 * 메시지를 수신자에게 전달하는 라우팅 중심 도메인 서비스.
 *
 * - 서버 1대: 수신자가 이 서버에 붙어 있으면 로컬 전달.
 * - 서버 다수: 세션 레지스트리로 수신자의 서버를 찾아 지향 전달.
 * - 그룹: 방 멤버를 서버별로 묶어 서버당 한 번만 전달 (서버 단위 팬아웃).
 */
class ChatDispatcher(
    private val serverId: String,
    private val connectionRegistry: ConnectionRegistry,
    private val sessionLocator: SessionLocator,
    private val messageBroker: MessageBroker,
    private val roomRepository: RoomRepository,
    private val messageStore: MessageStore,
) {
    fun dispatch(message: ChatMessage) {
        messageStore.save(message)
        when (val target = message.target) {
            is MessageTarget.Direct -> dispatchDirect(target.receiverId, message)
            is MessageTarget.Room -> dispatchRoom(target.roomId, message)
        }
    }

    private fun dispatchDirect(receiverId: Long, message: ChatMessage) {
        val targetServer = sessionLocator.locate(receiverId) ?: return // 오프라인: 저장만
        routeTo(targetServer, listOf(receiverId), message)
    }

    private fun dispatchRoom(roomId: Long, message: ChatMessage) {
        val recipients = roomRepository.membersOf(roomId) - message.senderId
        RoomFanout.groupByServer(recipients, sessionLocator)
            .forEach { (targetServer, recipientIds) -> routeTo(targetServer, recipientIds, message) }
    }

    private fun routeTo(targetServer: String, recipientIds: List<Long>, message: ChatMessage) {
        if (targetServer == serverId) {
            recipientIds.forEach { connectionRegistry.find(it)?.send(message) }
        } else {
            messageBroker.forward(targetServer, DeliveryEnvelope(recipientIds, message))
        }
    }
}
