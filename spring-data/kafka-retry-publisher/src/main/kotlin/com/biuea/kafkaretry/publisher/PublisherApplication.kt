package com.biuea.kafkaretry.publisher

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["com.biuea.kafkaretry"],
    excludeName = [
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration"
    ]
)
class PublisherApplication

fun main(args: Array<String>) {
    runApplication<PublisherApplication>(*args)
}
