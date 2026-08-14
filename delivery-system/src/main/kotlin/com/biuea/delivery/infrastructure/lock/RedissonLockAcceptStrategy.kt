package com.biuea.delivery.infrastructure.lock

import com.biuea.delivery.domain.order.AcceptOutcome
import com.biuea.delivery.domain.order.DeliveryAcceptStrategy
import com.biuea.delivery.infrastructure.persistence.DeliveryOrderAssignmentTransaction
import org.redisson.api.RedissonClient
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Redisson 분산락 전략 — [DistributedLockAcceptStrategy] 와 임계 구역 로직은 같고 락 메커니즘만 다르다.
 *
 * 직접 구현 스핀락([RedisDistributedLock])은 `SET NX PX` 를 20ms 간격으로 다시 던지며 대기한다.
 * Redisson 은 획득에 실패하면 남은 TTL 을 받아 `redisson_lock__channel:{락이름}` 을 구독하고 블로킹하며,
 * 보유자가 해제할 때 Lua 안에서 `DEL` + `PUBLISH` 로 대기자 하나를 깨운다.
 *
 * 이 클래스의 존재 이유는 "스핀락의 낮은 처리량이 20ms 폴링 탓인가, 락 직렬화 자체 탓인가" 를 가리는 것이다.
 * 그래서 획득 대기 예산(5초)·트랜잭션 경계([DeliveryOrderAssignmentTransaction])·version 최후 방어선까지
 * 스핀락과 동일하게 맞췄다. 다른 것은 대기 방식 하나뿐이어야 측정값이 락 메커니즘의 차이를 뜻한다.
 */
@Component
class RedissonLockAcceptStrategy(
    private val redissonClient: RedissonClient,
    private val deliveryOrderAssignmentTransaction: DeliveryOrderAssignmentTransaction,
) : DeliveryAcceptStrategy {

    override fun accept(orderId: Long, riderId: Long): AcceptOutcome {
        val lock = redissonClient.getLock(lockNameOf(orderId))
        // leaseTime 을 주지 않는다 → 워치독이 TTL 을 자동 갱신한다.
        // 고정 TTL(스핀락의 3초)로 잡으면 커넥션 풀 20개에 라이더 200명이 몰려 커넥션 획득 대기가 길어질 때
        // 임계 구역이 TTL 보다 오래 걸릴 수 있다. 그러면 두 스레드가 같은 락을 동시에 들고 있게 되고,
        // 중복 배차를 DB version 에만 의존해 막아야 한다. 워치독은 그 창을 애초에 만들지 않는다.
        // 비용은 락 하나당 만료 시간의 1/3(기본 10초) 주기 갱신 태스크 하나 — 수 ms 임계 구역에서는 갱신이 돌지도 않는다.
        if (!lock.tryLock(ACQUIRE_WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            return AcceptOutcome.LockAcquisitionFailed(orderId)
        }
        return try {
            deliveryOrderAssignmentTransaction.assignWithVersionCheck(orderId, riderId)
        } catch (exception: OptimisticLockingFailureException) {
            // 워치독이 있어도 클라이언트가 Redis 와 끊기면 락은 만료된다. 중복 배차의 최후 방어선은 DB version 이다.
            AcceptOutcome.LockAcquisitionFailed(orderId)
        } finally {
            // 워치독이 TTL 을 갱신하므로 여기서 소유권이 살아 있다.
            // isHeldByCurrentThread 로 확인하지 않는 이유: Redis 명령을 1회 더 써서 대기 구간 명령 수 비교를 오염시킨다.
            lock.unlock()
        }
    }

    private fun lockNameOf(orderId: Long): String = "$LOCK_NAME_PREFIX$orderId"

    companion object {
        /** 스핀락과 키 공간을 분리한다. 같은 이름을 쓰면 벤치마크에서 두 전략이 서로의 락에 걸린다. */
        private const val LOCK_NAME_PREFIX = "lock:redisson:order:"

        /** 스핀락의 대기 예산(5초)과 동일하게 맞춘다 — 대기 방식만 다르게 두기 위한 조건 통제다. */
        private val ACQUIRE_WAIT_TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}
