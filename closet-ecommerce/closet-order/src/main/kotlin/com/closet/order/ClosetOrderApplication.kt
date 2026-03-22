package com.closet.order

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.order", "com.closet.common"])
class ClosetOrderApplication

fun main(args: Array<String>) {
    runApplication<ClosetOrderApplication>(*args)
}
