package com.closet.inventory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.inventory", "com.closet.common"])
class ClosetInventoryApplication

fun main(args: Array<String>) {
    runApplication<ClosetInventoryApplication>(*args)
}
