package com.closet.inventory
import org.springframework.boot.autoconfigure.domain.EntityScan

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.closet.inventory", "com.closet.common"])
@EnableScheduling
@EntityScan(basePackages = ["com.closet.inventory", "com.closet.common"])
@EnableJpaRepositories(basePackages = ["com.closet.inventory.repository", "com.closet.common.kafka"])
class ClosetInventoryApplication

fun main(args: Array<String>) {
    runApplication<ClosetInventoryApplication>(*args)
}
