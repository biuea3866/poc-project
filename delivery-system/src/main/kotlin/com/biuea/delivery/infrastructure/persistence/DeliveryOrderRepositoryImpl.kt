package com.biuea.delivery.infrastructure.persistence

import com.biuea.delivery.domain.order.DeliveryOrder
import com.biuea.delivery.domain.order.DeliveryOrderRepository
import org.springframework.stereotype.Component

@Component
class DeliveryOrderRepositoryImpl(
    private val deliveryOrderJpaRepository: DeliveryOrderJpaRepository,
) : DeliveryOrderRepository {

    override fun findBy(orderId: Long): DeliveryOrder? =
        deliveryOrderJpaRepository.findById(orderId).orElse(null)?.toDomain()

    override fun findForUpdateBy(orderId: Long): DeliveryOrder? =
        deliveryOrderJpaRepository.findWithRowLockById(orderId)?.toDomain()

    override fun save(order: DeliveryOrder): DeliveryOrder =
        deliveryOrderJpaRepository.save(DeliveryOrderJpaEntity.from(order)).toDomain()

    override fun saveAll(orders: List<DeliveryOrder>): List<DeliveryOrder> =
        deliveryOrderJpaRepository.saveAll(orders.map(DeliveryOrderJpaEntity::from)).map { it.toDomain() }

    override fun deleteAll() = deliveryOrderJpaRepository.deleteAllInBatch()
}
