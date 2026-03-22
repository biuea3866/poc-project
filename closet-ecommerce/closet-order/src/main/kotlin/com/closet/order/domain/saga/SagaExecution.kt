package com.closet.order.domain.saga

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "saga_execution")
class SagaExecution(

    @Column(name = "saga_id", nullable = false, unique = true, length = 100)
    val sagaId: String = UUID.randomUUID().toString(),

    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 50)
    var currentStep: SagaStep = SagaStep.STARTED,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: SagaStatus = SagaStatus.STARTED,

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    var failureReason: String? = null,
) : BaseEntity()

enum class SagaStep {
    STARTED,
    INVENTORY_RESERVING,
    PAYMENT_PROCESSING,
    COMPLETED,
    COMPENSATING,
}

enum class SagaStatus {
    STARTED,
    INVENTORY_RESERVED,
    PAYMENT_APPROVED,
    COMPLETED,
    COMPENSATING,
    FAILED,
}
