package com.biuea.kafkaretry.scheduler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(scanBasePackages = ["com.biuea.kafkaretry"])
@EntityScan("com.biuea.kafkaretry.common.entity")
@EnableJpaRepositories("com.biuea.kafkaretry.common.repository")
class SchedulerApplication

fun main(args: Array<String>) {
    runApplication<SchedulerApplication>(*args)
}
