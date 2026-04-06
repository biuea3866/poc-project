package com.closet.promotion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.promotion", "com.closet.common"])
class ClosetPromotionApplication

fun main(args: Array<String>) {
    runApplication<ClosetPromotionApplication>(*args)
}
