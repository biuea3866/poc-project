package com.closet.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["com.closet.bff"],
    exclude = [
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration::class,
        org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration::class,
    ]
)
class ClosetBffApplication

fun main(args: Array<String>) {
    runApplication<ClosetBffApplication>(*args)
}
