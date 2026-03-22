package com.closet.order.repository

import com.closet.order.domain.saga.SagaExecution
import com.closet.order.domain.saga.SagaStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SagaExecutionRepository : JpaRepository<SagaExecution, Long> {
    fun findBySagaId(sagaId: String): SagaExecution?
    fun findByOrderId(orderId: Long): SagaExecution?
    fun findByStatus(status: SagaStatus): List<SagaExecution>
}
