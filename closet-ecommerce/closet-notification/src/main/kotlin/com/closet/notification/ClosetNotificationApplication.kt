package com.closet.notification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.notification", "com.closet.common"])
class ClosetNotificationApplication

fun main(args: Array<String>) {
    runApplication<ClosetNotificationApplication>(*args)
}
