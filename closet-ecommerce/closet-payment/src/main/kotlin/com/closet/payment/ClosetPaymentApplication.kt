package com.closet.payment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.closet.payment", "com.closet.common"])
@EntityScan(basePackages = ["com.closet.payment", "com.closet.common"])
@EnableJpaRepositories(basePackages = ["com.closet.payment", "com.closet.common.kafka"])
@EnableScheduling
class ClosetPaymentApplication

fun main(args: Array<String>) {
    runApplication<ClosetPaymentApplication>(*args)
}
