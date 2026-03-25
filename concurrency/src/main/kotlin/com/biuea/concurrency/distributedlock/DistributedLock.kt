package com.biuea.concurrency.distributedlock

/**
 * 분산 락 어노테이션
 *
 * @param key 락 키 (SpEL 지원)
 * @param waitTime 락 대기 시간 (초) — 대기 시간 초과 시 락 획득 실패
 * @param leaseTime 락 점유 시간 (초) — 점유 시간 초과 시 강제 해제
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val waitTime: Long = 5,
    val leaseTime: Long = 10,
)
