package com.closet.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(
    scanBasePackages = ["com.closet.bff"],
    exclude = [
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration::class,
        org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration::class,
    ]
)
@EnableFeignClients(basePackages = ["com.closet.bff.client"])
class ClosetBffApplication

fun main(args: Array<String>) {
    runApplication<ClosetBffApplication>(*args)
}
