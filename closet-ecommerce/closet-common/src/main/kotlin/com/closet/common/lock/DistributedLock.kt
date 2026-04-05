package com.closet.common.lock

/**
 * Redisson 기반 분산 락 어노테이션.
 *
 * 메서드에 선언하면 DistributedLockAspect가 AOP로 락을 획득/해제한다.
 * SpEL 표현식으로 동적 키를 생성할 수 있다.
 *
 * 사용 예:
 * ```
 * @DistributedLock(key = "'inventory:lock:' + #sku")
 * fun deduct(sku: String, quantity: Int) { ... }
 * ```
 *
 * @param key 락 키 (SpEL 표현식 지원)
 * @param waitTime 락 획득 대기 시간 (초). 기본 5초
 * @param leaseTime 락 유지 시간 (초). -1이면 Watch Dog 자동 갱신. 기본 3초
 * @param maxRetries 락 획득 실패 시 재시도 횟수. 기본 3회
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class DistributedLock(
    val key: String,
    val waitTime: Long = 5L,
    val leaseTime: Long = 3L,
    val maxRetries: Int = 3,
)
