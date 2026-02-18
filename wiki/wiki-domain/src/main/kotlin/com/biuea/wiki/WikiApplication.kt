package com.biuea.wiki

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WikiApplication

fun main(args: Array<String>) {
	runApplication<WikiApplication>(*args)
}
