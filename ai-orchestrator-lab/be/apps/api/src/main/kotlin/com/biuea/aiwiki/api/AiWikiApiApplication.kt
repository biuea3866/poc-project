package com.biuea.aiwiki.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.biuea.aiwiki"])
class AiWikiApiApplication

fun main(args: Array<String>) {
    runApplication<AiWikiApiApplication>(*args)
}
