package com.closet.order

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.closet.order", "com.closet.common"])
@EnableScheduling
@EnableJpaRepositories(basePackages = ["com.closet.order.repository", "com.closet.common.event"])
class ClosetOrderApplication

fun main(args: Array<String>) {
    runApplication<ClosetOrderApplication>(*args)
}
