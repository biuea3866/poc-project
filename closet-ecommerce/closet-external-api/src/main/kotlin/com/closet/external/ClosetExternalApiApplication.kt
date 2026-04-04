package com.closet.external

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class ClosetExternalApiApplication

fun main(args: Array<String>) {
    runApplication<ClosetExternalApiApplication>(*args)
}
