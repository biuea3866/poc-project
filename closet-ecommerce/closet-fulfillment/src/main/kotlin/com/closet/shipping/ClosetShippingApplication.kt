package com.closet.shipping

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(scanBasePackages = ["com.closet.shipping", "com.closet.common"])
class ClosetShippingApplication

fun main(args: Array<String>) {
    runApplication<ClosetShippingApplication>(*args)
}
