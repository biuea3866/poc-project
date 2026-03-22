package com.closet.product

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.product", "com.closet.common"])
class ClosetProductApplication

fun main(args: Array<String>) {
    runApplication<ClosetProductApplication>(*args)
}
