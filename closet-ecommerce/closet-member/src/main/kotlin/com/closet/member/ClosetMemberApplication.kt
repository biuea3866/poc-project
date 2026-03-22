package com.closet.member

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.closet.member", "com.closet.common"])
class ClosetMemberApplication

fun main(args: Array<String>) {
    runApplication<ClosetMemberApplication>(*args)
}
