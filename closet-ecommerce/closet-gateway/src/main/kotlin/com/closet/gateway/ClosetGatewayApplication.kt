package com.closet.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ClosetGatewayApplication

fun main(args: Array<String>) {
    runApplication<ClosetGatewayApplication>(*args)
}
