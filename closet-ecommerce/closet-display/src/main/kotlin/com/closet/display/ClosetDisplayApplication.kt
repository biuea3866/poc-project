package com.closet.display

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.display", "com.closet.common"])
class ClosetDisplayApplication

fun main(args: Array<String>) {
    runApplication<ClosetDisplayApplication>(*args)
}
