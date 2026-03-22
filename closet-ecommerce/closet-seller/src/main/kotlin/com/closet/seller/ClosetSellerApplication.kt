package com.closet.seller

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.seller", "com.closet.common"])
class ClosetSellerApplication

fun main(args: Array<String>) {
    runApplication<ClosetSellerApplication>(*args)
}
