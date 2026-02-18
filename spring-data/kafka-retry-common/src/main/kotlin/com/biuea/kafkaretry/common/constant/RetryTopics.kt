package com.biuea.kafkaretry.common.constant

object RetryTopics {
    const val PAYMENT_TOPIC = "payment-topic"
    const val PAYMENT_DLT = "payment-topic.DLT"

    const val ORDER_TOPIC = "order-topic"
    const val ORDER_DLT = "order-topic-dlt"

    const val GROUP_PAYMENT = "payment-consumer-group"
    const val GROUP_ORDER = "order-consumer-group"
}
