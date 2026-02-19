package com.biuea.wiki.worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.biuea.wiki"])
class WikiWorkerApplication

fun main(args: Array<String>) {
    runApplication<WikiWorkerApplication>(*args)
}
