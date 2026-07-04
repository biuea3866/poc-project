package com.biuea.chat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChatSystemApplication

fun main(args: Array<String>) {
    runApplication<ChatSystemApplication>(*args)
}
