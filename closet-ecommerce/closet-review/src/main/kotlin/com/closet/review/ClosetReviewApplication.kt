package com.closet.review

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["com.closet.review", "com.closet.common"],
)
class ClosetReviewApplication

fun main(args: Array<String>) {
    runApplication<ClosetReviewApplication>(*args)
}
