package com.biuea.springdata.kafka.config

object KafkaTopics {
    // Basic example
    const val USER_EVENTS = "user-events"

    // High-throughput example
    const val NORMAL_EVENTS = "normal-events"
    const val BATCH_EVENTS = "batch-events"
    const val OPTIMIZED_EVENTS = "optimized-events"
    const val PARALLEL_EVENTS = "parallel-events"  // Confluent Parallel Consumer

    // DLT (Dead Letter Topic)
    const val NORMAL_EVENTS_DLT = "normal-events.DLT"
    const val BATCH_EVENTS_DLT = "batch-events.DLT"
    const val OPTIMIZED_EVENTS_DLT = "optimized-events.DLT"
}
