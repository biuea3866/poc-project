package com.hrplatform.attendance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableAsync
@EnableScheduling
class AttendanceServiceApplication

fun main(args: Array<String>) {
    runApplication<AttendanceServiceApplication>(*args)
}
