package com.biuea.springdata

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@EnableKafka
@SpringBootApplication
class SpringDataApplication

fun main(args: Array<String>) {
	runApplication<SpringDataApplication>(*args)
}
