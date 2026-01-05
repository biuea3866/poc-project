package com.biuea.kotlinpractice

import com.biuea.kotlinpractice.async.AsyncTransactionExamples
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AsyncTransactionExamplesTest {

    @Test
    fun `coroutine bad example loses ThreadLocal transaction`() = runBlocking {
        assertFalse(AsyncTransactionExamples.coroutineBadExample())
    }

    @Test
    fun `coroutine good example propagates transaction context`() = runBlocking {
        val ids = AsyncTransactionExamples.coroutineGoodExample()
        assertEquals(listOf("TX-CORO-GOOD", "TX-CORO-GOOD"), ids)
    }

    @Test
    fun `virtual thread bad example does not inherit ThreadLocal`() {
        assertFalse(AsyncTransactionExamples.virtualThreadBadExample())
    }

    @Test
    fun `virtual thread good example keeps ThreadLocal within same thread`() {
        assertTrue(AsyncTransactionExamples.virtualThreadGoodExample())
    }
}
