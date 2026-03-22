package com.closet.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.bff", "com.closet.common"])
class ClosetBffApplication

fun main(args: Array<String>) {
    runApplication<ClosetBffApplication>(*args)
}
