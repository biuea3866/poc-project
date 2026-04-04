package com.closet.search

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["com.closet.search"],
    exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class],
)
class ClosetSearchApplication

fun main(args: Array<String>) {
    runApplication<ClosetSearchApplication>(*args)
}
