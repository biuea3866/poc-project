package com.biuea.delivery.infrastructure.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface DeliveryOrderJpaRepository : JpaRepository<DeliveryOrderJpaEntity, Long> {

    /**
     * SELECT ... FOR UPDATE. 조회 시점에 행을 잠가 뒤따라온 라이더들을 대기시킨다.
     * 대기 시간은 innodb_lock_wait_timeout 을 넘기면 예외로 끝난다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithRowLockById(id: Long): DeliveryOrderJpaEntity?
}
