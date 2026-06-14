package com.biuea.batch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// @EnableBatchProcessing 는 선언하지 않는다 — Spring Boot 4 의 Batch 자동구성을 그대로 사용한다.
@SpringBootApplication
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}
