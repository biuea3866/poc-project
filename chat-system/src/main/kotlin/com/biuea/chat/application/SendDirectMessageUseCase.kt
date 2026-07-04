package com.biuea.chat.application

import com.biuea.chat.domain.ChatDispatcher
import com.biuea.chat.domain.message.ChatMessage
import org.springframework.stereotype.Service

data class SendDirectMessageCommand(
    val senderId: Long,
    val receiverId: Long,
    val content: String,
)

@Service
class SendDirectMessageUseCase(
    private val chatDispatcher: ChatDispatcher,
) {
    fun execute(command: SendDirectMessageCommand) {
        chatDispatcher.dispatch(
            ChatMessage.direct(command.senderId, command.receiverId, command.content),
        )
    }
}
