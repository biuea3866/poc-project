package com.closet.payment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.payment", "com.closet.common"])
class ClosetPaymentApplication

fun main(args: Array<String>) {
    runApplication<ClosetPaymentApplication>(*args)
}
