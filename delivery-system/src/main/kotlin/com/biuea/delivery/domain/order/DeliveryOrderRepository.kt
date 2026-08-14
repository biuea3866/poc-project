package com.biuea.delivery.domain.order

interface DeliveryOrderRepository {
    fun findBy(orderId: Long): DeliveryOrder?

    /** SELECT ... FOR UPDATE. 호출부 트랜잭션이 끝날 때까지 행을 잠근다 — 트랜잭션 밖에서 부르면 의미가 없다. */
    fun findForUpdateBy(orderId: Long): DeliveryOrder?

    fun save(order: DeliveryOrder): DeliveryOrder

    fun saveAll(orders: List<DeliveryOrder>): List<DeliveryOrder>

    fun deleteAll()
}
