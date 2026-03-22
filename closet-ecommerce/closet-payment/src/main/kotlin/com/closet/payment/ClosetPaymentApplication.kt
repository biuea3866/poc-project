package com.closet.payment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.closet.payment", "com.closet.common"])
@EnableScheduling
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = ["com.closet.payment.domain", "com.closet.common.event"])
class ClosetPaymentApplication

fun main(args: Array<String>) {
    runApplication<ClosetPaymentApplication>(*args)
}
