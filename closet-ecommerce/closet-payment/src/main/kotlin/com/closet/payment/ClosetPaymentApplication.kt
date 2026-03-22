package com.closet.payment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.payment", "com.closet.common"])
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = ["com.closet.payment.domain"])
class ClosetPaymentApplication

fun main(args: Array<String>) {
    runApplication<ClosetPaymentApplication>(*args)
}
