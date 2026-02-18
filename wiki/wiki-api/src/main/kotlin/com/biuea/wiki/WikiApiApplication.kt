package com.biuea.wiki

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.biuea.wiki"])
class WikiApiApplication

fun main(args: Array<String>) {
    runApplication<WikiApiApplication>(*args)
}
