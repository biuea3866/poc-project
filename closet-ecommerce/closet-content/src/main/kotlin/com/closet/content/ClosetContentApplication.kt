package com.closet.content

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.content", "com.closet.common"])
class ClosetContentApplication

fun main(args: Array<String>) {
    runApplication<ClosetContentApplication>(*args)
}
