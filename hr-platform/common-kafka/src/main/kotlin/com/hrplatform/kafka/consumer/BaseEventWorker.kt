package com.hrplatform.kafka.consumer

import org.slf4j.LoggerFactory

abstract class BaseEventWorker {

    protected val logger = LoggerFactory.getLogger(this::class.java)

    protected fun logConsuming(eventType: String) {
        logger.info("Consuming event: type={}", eventType)
    }

    protected fun logConsumed(eventType: String) {
        logger.info("Event consumed successfully: type={}", eventType)
    }

    protected fun logFailed(eventType: String, message: String?) {
        logger.error("Failed to consume event: type={}, error={}", eventType, message)
    }
}
