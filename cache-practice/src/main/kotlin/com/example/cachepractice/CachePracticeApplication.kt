package com.example.cachepractice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CachePracticeApplication

fun main(args: Array<String>) {
    runApplication<CachePracticeApplication>(*args)
}
