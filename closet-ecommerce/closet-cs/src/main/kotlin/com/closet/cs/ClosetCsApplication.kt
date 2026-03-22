package com.closet.cs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.cs", "com.closet.common"])
class ClosetCsApplication

fun main(args: Array<String>) {
    runApplication<ClosetCsApplication>(*args)
}
