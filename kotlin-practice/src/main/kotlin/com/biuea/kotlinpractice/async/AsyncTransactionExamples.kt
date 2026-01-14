package com.biuea.kotlinpractice.async

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.atomic.AtomicReference

object AsyncTransactionExamples {

    suspend fun coroutineBadExample(): Boolean {
        TransactionExample.beginTransaction("TX-CORO-BAD")
        // 스레드 전환으로 ThreadLocal 트랜잭션이 사라질 수 있음
        val seen = withContext(Dispatchers.Default) {
            TransactionExample.getTransaction() != null
        }
        TransactionExample.commitTransaction()
        return seen
    }

    suspend fun coroutineGoodExample(): List<String?> {
        return TransactionExample.withTransaction("TX-CORO-GOOD") { _ ->
            coroutineScope {
                val child1 = async { TransactionExample.getCurrentTransaction()?.id }
                val child2 = async { TransactionExample.getCurrentTransaction()?.id }
                // 코루틴 컨텍스트 전파로 같은 트랜잭션 확인
                listOf(child1.await(), child2.await())
            }
        }
    }

    fun virtualThreadBadExample(): Boolean {
        val childSeen = AtomicReference(false)

        Thread.startVirtualThread {
            TransactionExample.beginTransaction("TX-VT-BAD")
            Thread.startVirtualThread {
                // 새 버추얼 스레드에는 ThreadLocal이 전파되지 않음
                childSeen.set(TransactionExample.getTransaction() != null)
            }.join()
            TransactionExample.commitTransaction()
        }.join()

        return childSeen.get()
    }

    fun virtualThreadGoodExample(): Boolean {
        val seen = AtomicReference(false)

        Thread.startVirtualThread {
            TransactionExample.beginTransaction("TX-VT-GOOD")
            // 같은 버추얼 스레드 안에서는 ThreadLocal 유지
            seen.set(TransactionExample.getTransaction() != null)
            TransactionExample.commitTransaction()
        }.join()

        return seen.get()
    }
}
