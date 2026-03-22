package com.closet.order
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EntityScan(basePackages = ["com.closet.order", "com.closet.common"])
@EnableJpaRepositories(basePackages = ["com.closet.order", "com.closet.common.kafka"])
@SpringBootApplication(scanBasePackages = ["com.closet.order", "com.closet.common"])
class ClosetOrderApplication

fun main(args: Array<String>) {
    runApplication<ClosetOrderApplication>(*args)
}
