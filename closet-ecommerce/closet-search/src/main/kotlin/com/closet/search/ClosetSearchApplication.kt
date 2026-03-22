package com.closet.search

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.search"])
class ClosetSearchApplication

fun main(args: Array<String>) {
    runApplication<ClosetSearchApplication>(*args)
}
