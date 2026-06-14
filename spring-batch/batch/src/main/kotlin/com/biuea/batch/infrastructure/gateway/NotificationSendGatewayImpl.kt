package com.biuea.batch.infrastructure.gateway

import com.biuea.batch.domain.NotificationMessage
import com.biuea.batch.domain.NotificationSendGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.locks.LockSupport

/**
 * 알림 발송 시뮬레이션. 실제 외부 발송 대신 CPU 작업 + 약간의 latency 를 발생시켜
 * 병렬화 전략(async / multi-thread / partition)의 효과를 측정 가능하게 만든다.
 *
 * latency 는 마이크로초 단위(parkNanos)로 둔다 — Thread.sleep(ms) 은 최소 단위가 커서
 * 수십만 묶음을 단일 스레드로 처리할 때 런타임이 비현실적으로 길어진다.
 *
 * stateless — 인스턴스 필드를 변경하지 않으므로 멀티스레드에서 공유 안전하다.
 */
@Component
class NotificationSendGatewayImpl(
    @param:Value("\${benchmark.send.cpu-iterations:15000}")
    private val cpuIterations: Int,
    @param:Value("\${benchmark.send.latency-micros:300}")
    private val latencyMicros: Long,
) : NotificationSendGateway {

    override fun send(message: NotificationMessage) {
        burnCpu(message.content)
        if (latencyMicros > 0) {
            LockSupport.parkNanos(latencyMicros * 1_000)
        }
    }

    /**
     * CPU 부하 시뮬레이션 — content 를 반복 해싱한다.
     * 공유 필드 쓰기(캐시라인 경합)는 CPU 측정을 왜곡하므로 피하고,
     * 사실상 발생하지 않는 분기로 hash 를 소비해 JIT 의 dead-code 제거만 막는다.
     */
    private fun burnCpu(content: String) {
        var hash = content.hashCode().toLong()
        repeat(cpuIterations) { i ->
            hash = hash * 31 + (content[i % content.length].code)
            hash = hash xor (hash ushr 17)
        }
        if (hash == Long.MIN_VALUE) {
            throw IllegalStateException("unreachable sentinel")
        }
    }
}
