package com.biuea.kafkaretry.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["com.biuea.kafkaretry"])
@EntityScan("com.biuea.kafkaretry.common.entity")
@EnableJpaRepositories("com.biuea.kafkaretry.common.repository")
class ApiApplication

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
