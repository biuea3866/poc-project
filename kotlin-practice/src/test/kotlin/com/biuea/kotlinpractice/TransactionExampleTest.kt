package com.biuea.kotlinpractice

import com.biuea.kotlinpractice.async.TransactionExample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class TransactionExampleTest {

    @Test
    fun `코루틴 스레드 전환 시 ThreadLocal 트랜잭션이 유실된다`() = runBlocking {
        TransactionExample.beginTransaction("TX-THREADLOCAL")
        val result = withContext(Dispatchers.Default) {
            TransactionExample.getTransaction()
        }
        TransactionExample.commitTransaction()

        assertNull(result)
    }

    @Test
    fun `코루틴 컨텍스트 전파로 동일 트랜잭션을 조회한다`() = runBlocking {
        val txIds = TransactionExample.withTransaction("TX-COROUTINE") { _ ->
            val child1 = async { TransactionExample.getCurrentTransaction()?.id }
            val child2 = async { TransactionExample.getCurrentTransaction()?.id }
            listOf(child1.await(), child2.await())
        }

        assertEquals(listOf("TX-COROUTINE", "TX-COROUTINE"), txIds)
    }

    @Test
    fun `버추얼 스레드 분기 시 ThreadLocal 트랜잭션이 전파되지 않는다`() {
        val childTxRef = AtomicReference<TransactionExample.TransactionContext?>()
        val latch = CountDownLatch(1)

        Thread.startVirtualThread {
            TransactionExample.beginTransaction("TX-VT")
            Thread.startVirtualThread {
                childTxRef.set(TransactionExample.getTransaction())
                latch.countDown()
            }.join()
            TransactionExample.commitTransaction()
        }.join()

        latch.await()
        assertNull(childTxRef.get())
    }
}
