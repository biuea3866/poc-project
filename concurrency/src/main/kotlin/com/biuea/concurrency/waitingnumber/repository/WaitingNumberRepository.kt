package com.biuea.concurrency.waitingnumber.repository

import com.biuea.concurrency.waitingnumber.domain.WaitingNumberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

interface WaitingNumberRepository : JpaRepository<WaitingNumberEntity, Long> {

    @Query("SELECT MAX(w.waitingNumber) FROM WaitingNumberEntity w WHERE w.productId = :productId")
    fun findMaxWaitingNumberByProductId(productId: Long): String?
}
