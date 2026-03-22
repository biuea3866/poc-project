package com.closet.settlement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.settlement", "com.closet.common"])
class ClosetSettlementApplication

fun main(args: Array<String>) {
    runApplication<ClosetSettlementApplication>(*args)
}
