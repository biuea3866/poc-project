package com.biuea.chat.application

import com.biuea.chat.domain.ChatDispatcher
import com.biuea.chat.domain.message.ChatMessage
import org.springframework.stereotype.Service

data class SendRoomMessageCommand(
    val senderId: Long,
    val roomId: Long,
    val content: String,
)

@Service
class SendRoomMessageUseCase(
    private val chatDispatcher: ChatDispatcher,
) {
    fun execute(command: SendRoomMessageCommand) {
        chatDispatcher.dispatch(
            ChatMessage.room(command.senderId, command.roomId, command.content),
        )
    }
}
