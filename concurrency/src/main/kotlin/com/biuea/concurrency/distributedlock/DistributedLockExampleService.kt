package com.biuea.concurrency.distributedlock

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

/**
 * 분산 락 사용 예제 서비스
 *
 * 실제 사용 시나리오:
 * - 주문 생성 (동일 사용자 따닥 방지)
 * - 재고 차감 (동시 주문 방지)
 * - 결제 처리 (중복 결제 방지)
 * - 채용 단계 이동 (동시 이동 방지)
 */
@Service
class DistributedLockExampleService {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 재고 (테스트용 in-memory) */
    val stock = AtomicInteger(100)

    /** 처리 횟수 추적 */
    val processedCount = AtomicInteger(0)

    /**
     * 시나리오 1: 따닥 방지 (wait=0)
     * - 이미 처리 중이면 즉시 튕김
     * - 결제, AI 서류평가 등 중복 실행이 위험한 경우
     */
    @DistributedLock(key = "payment:{orderId}", waitTime = 0, leaseTime = 10)
    fun processPayment(orderId: Long): String {
        log.info("[Payment] 결제 처리 시작 — orderId={}", orderId)
        Thread.sleep(1000) // 1초 소요되는 결제 처리
        processedCount.incrementAndGet()
        log.info("[Payment] 결제 처리 완료 — orderId={}", orderId)
        return "결제 완료: orderId=$orderId"
    }

    /**
     * 시나리오 2: 순차 처리 (wait > API 종료 시간)
     * - 두 번째 요청도 대기 후 정상 처리
     * - 멱등한 로직이면서 에러가 불필요한 경우
     */
    @DistributedLock(key = "stock:{productId}", waitTime = 5, leaseTime = 10)
    fun decreaseStock(productId: Long, quantity: Int): Int {
        log.info("[Stock] 재고 차감 시작 — productId={}, quantity={}", productId, quantity)
        Thread.sleep(500) // 0.5초 소요
        val remaining = stock.addAndGet(-quantity)
        processedCount.incrementAndGet()
        log.info("[Stock] 재고 차감 완료 — productId={}, remaining={}", productId, remaining)
        return remaining
    }

    /**
     * 시나리오 3: lease 만료 위험 (의도적으로 긴 처리)
     * - lease보다 처리 시간이 길면 동시성 문제 발생!
     * - 테스트에서 이 위험 상황을 검증
     */
    @DistributedLock(key = "slow:{taskId}", waitTime = 3, leaseTime = 2)
    fun slowOperation(taskId: Long): String {
        log.info("[Slow] 느린 작업 시작 — taskId={}", taskId)
        Thread.sleep(3000) // 3초 (lease 2초보다 김!)
        processedCount.incrementAndGet()
        log.info("[Slow] 느린 작업 완료 — taskId={}", taskId)
        return "완료: taskId=$taskId"
    }

    /**
     * 처리 시간을 외부에서 지정할 수 있는 범용 메서드 (테스트용)
     */
    @DistributedLock(key = "test:{lockKey}", waitTime = 5, leaseTime = 10)
    fun processWithDuration(lockKey: String, durationMs: Long): String {
        log.info("[Test] 처리 시작 — key={}, duration={}ms", lockKey, durationMs)
        Thread.sleep(durationMs)
        processedCount.incrementAndGet()
        log.info("[Test] 처리 완료 — key={}", lockKey)
        return "완료: key=$lockKey"
    }

    fun reset() {
        stock.set(100)
        processedCount.set(0)
    }
}
