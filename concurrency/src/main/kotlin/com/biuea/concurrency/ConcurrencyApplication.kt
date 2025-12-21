package com.biuea.concurrency

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories
@ComponentScan("com.biuea.concurrency")
class ConcurrencyApplication

fun main(args: Array<String>) {
	runApplication<ConcurrencyApplication>(*args)
}
