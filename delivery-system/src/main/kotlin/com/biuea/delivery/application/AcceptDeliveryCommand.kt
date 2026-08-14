package com.biuea.delivery.application

data class AcceptDeliveryCommand(
    val orderId: Long,
    val riderId: Long,
)
