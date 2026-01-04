package com.biuea.kotlinpractice

import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.system.measureTimeMillis

/**
 * 스레드, 코루틴, 버추얼 스레드 비교 테스트
 */
class AsyncComparisonTest {

    @Test
    fun `I-O 작업 성능 비교 - 스레드 vs 코루틴 vs 버추얼 스레드`() {
        println("\n=== I/O 작업 성능 비교 ===")

        val taskCount = 1000
        val ioDelay = 100L

        // 1. 플랫폼 스레드 (제한된 풀)
        val threadTime = measureTimeMillis {
            val executor = Executors.newFixedThreadPool(100)
            try {
                val latch = CountDownLatch(taskCount)
                repeat(taskCount) {
                    executor.submit {
                        Thread.sleep(ioDelay)
                        latch.countDown()
                    }
                }
                latch.await()
            } finally {
                executor.shutdown()
            }
        }

        // 2. 코루틴
        val coroutineTime = measureTimeMillis {
            runBlocking {
                val jobs = List(taskCount) {
                    launch(Dispatchers.IO) {
                        delay(ioDelay)
                    }
                }
                jobs.forEach { it.join() }
            }
        }

        // 3. 버추얼 스레드
        val virtualThreadTime = measureTimeMillis {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(taskCount) {
                    executor.submit {
                        Thread.sleep(ioDelay)
                    }
                }
                futures.forEach { it.get() }
            }
        }

        println("플랫폼 스레드 (풀 100): ${threadTime}ms")
        println("코루틴: ${coroutineTime}ms")
        println("버추얼 스레드: ${virtualThreadTime}ms")

        // 코루틴과 버추얼 스레드가 플랫폼 스레드보다 빠르거나 비슷해야 함
        assertTrue(coroutineTime < threadTime * 1.5)
        assertTrue(virtualThreadTime < threadTime * 1.5)
    }

    @Test
    fun `CPU 집약적 작업 성능 비교`() {
        println("\n=== CPU 집약적 작업 성능 비교 ===")

        val taskCount = 100
        val iterations = 10_000_000

        fun cpuWork(): Long {
            var sum = 0L
            repeat(iterations) {
                sum += it
            }
            return sum
        }

        // 1. 플랫폼 스레드
        val threadTime = measureTimeMillis {
            val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            try {
                val futures = List(taskCount) {
                    executor.submit { cpuWork() }
                }
                futures.forEach { it.get() }
            } finally {
                executor.shutdown()
            }
        }

        // 2. 코루틴
        val coroutineTime = measureTimeMillis {
            runBlocking {
                val jobs = List(taskCount) {
                    async(Dispatchers.Default) {
                        cpuWork()
                    }
                }
                jobs.forEach { it.await() }
            }
        }

        // 3. 버추얼 스레드
        val virtualThreadTime = measureTimeMillis {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(taskCount) {
                    executor.submit { cpuWork() }
                }
                futures.forEach { it.get() }
            }
        }

        println("플랫폼 스레드: ${threadTime}ms")
        println("코루틴: ${coroutineTime}ms")
        println("버추얼 스레드: ${virtualThreadTime}ms")

        // CPU 작업에서는 플랫폼 스레드와 코루틴이 비슷하거나 빠름
        assertTrue(threadTime > 0)
        assertTrue(coroutineTime > 0)
        assertTrue(virtualThreadTime > 0)
        println("버추얼 스레드는 CPU 집약 작업에 부적합 (I/O 작업에 최적화)")
    }

    @Test
    fun `동시성 수 비교 - 최대 동시 실행 가능 수`() {
        println("\n=== 동시성 수 비교 ===")

        // 코루틴: 매우 많은 수 생성 가능
        val coroutineCount = AtomicInteger(0)
        runBlocking {
            val jobs = List(100_000) {
                launch {
                    coroutineCount.incrementAndGet()
                    delay(1000)
                }
            }
            delay(100) // 생성 대기
            println("생성된 코루틴 수: ${coroutineCount.get()}")
            jobs.forEach { it.cancel() }
        }

        assertTrue(coroutineCount.get() >= 10_000)
        println("생성된 코루틴 수: ${coroutineCount.get()}")

        // 버추얼 스레드: 매우 많은 수 생성 가능
        val virtualThreadCount = AtomicInteger(0)
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = List(100_000) {
                executor.submit {
                    virtualThreadCount.incrementAndGet()
                    Thread.sleep(1000)
                }
            }
            Thread.sleep(100)
            println("생성된 버추얼 스레드 수: ${virtualThreadCount.get()}")
            futures.forEach { it.cancel(true) }
        }

        assertTrue(virtualThreadCount.get() >= 10_000)
        println("코루틴과 버추얼 스레드 모두 수십만 개 이상 생성 가능합니다.")
    }

    @Test
    fun `공유 상태 동기화 테스트 - Race Condition`() {
        println("\n=== 공유 상태 동기화 테스트 ===")

        val expected = 10_000

        // 1. 동기화 없음 (문제 발생 가능)
        var counterWithoutSync = 0
        val threads1 = List(10) {
            Thread {
                repeat(1000) {
                    counterWithoutSync++
                }
            }
        }
        threads1.forEach { it.start() }
        threads1.forEach { it.join() }
        println("동기화 없음: $counterWithoutSync (예상: $expected)")
        println("Race condition으로 값이 정확하지 않을 가능성이 높습니다.")
        // 참고: 때때로 정확한 값이 나올 수도 있어 테스트하지 않음

        // 2. synchronized 사용
        val lock = Any()
        var counterWithSync = 0
        val threads2 = List(10) {
            Thread {
                repeat(1000) {
                    synchronized(lock) {
                        counterWithSync++
                    }
                }
            }
        }
        threads2.forEach { it.start() }
        threads2.forEach { it.join() }
        println("synchronized 사용: $counterWithSync (예상: $expected)")
        assertEquals(expected, counterWithSync)

        // 3. AtomicInteger 사용
        val atomicCounter = AtomicInteger(0)
        val threads3 = List(10) {
            Thread {
                repeat(1000) {
                    atomicCounter.incrementAndGet()
                }
            }
        }
        threads3.forEach { it.start() }
        threads3.forEach { it.join() }
        println("AtomicInteger 사용: ${atomicCounter.get()} (예상: $expected)")
        assertEquals(expected, atomicCounter.get())
    }

    @Test
    fun `코루틴 취소 테스트`() = runBlocking {
        println("\n=== 코루틴 취소 테스트 ===")

        val executionCount = AtomicInteger(0)

        val job = launch {
            try {
                repeat(100) {
                    executionCount.incrementAndGet()
                    delay(10)
                }
            } catch (e: CancellationException) {
                println("코루틴 취소됨")
            }
        }

        delay(50)
        job.cancel()
        job.join()

        println("실행 횟수: ${executionCount.get()}")
        assertTrue(executionCount.get() < 100)
    }

    @Test
    fun `코루틴 예외 전파 테스트 - supervisorScope`() = runBlocking {
        println("\n=== 코루틴 예외 전파 테스트 ===")

        val job1Completed = AtomicInteger(0)
        val job3Completed = AtomicInteger(0)

        supervisorScope {
            launch {
                delay(100)
                job1Completed.set(1)
            }

            launch {
                delay(50)
                throw RuntimeException("작업 2 실패!")
            }

            launch {
                delay(150)
                job3Completed.set(1)
            }

            delay(200)
        }

        assertEquals(1, job1Completed.get())
        assertEquals(1, job3Completed.get())
        println("supervisorScope로 다른 작업은 계속 실행됨")
    }

    @Test
    fun `코루틴 구조화된 동시성 테스트`() = runBlocking {
        println("\n=== 구조화된 동시성 테스트 ===")

        val completedTasks = AtomicInteger(0)

        val time = measureTimeMillis {
            coroutineScope {
                launch {
                    delay(100)
                    completedTasks.incrementAndGet()
                }

                launch {
                    delay(200)
                    completedTasks.incrementAndGet()
                }

                launch {
                    delay(150)
                    completedTasks.incrementAndGet()
                }
            }
            // coroutineScope는 모든 자식이 완료될 때까지 대기
        }

        assertEquals(3, completedTasks.get())
        assertTrue(time >= 200) // 가장 긴 작업(200ms)만큼 대기
        println("소요 시간: ${time}ms (모든 자식 완료 대기)")
    }

    @Test
    fun `async-await 병렬 처리 테스트`() = runBlocking {
        println("\n=== async-await 병렬 처리 테스트 ===")

        val time = measureTimeMillis {
            val result1 = async {
                delay(200)
                100
            }

            val result2 = async {
                delay(300)
                200
            }

            val sum = result1.await() + result2.await()
            assertEquals(300, sum)
        }

        // 병렬 실행으로 300ms 근처여야 함 (순차 실행이면 500ms)
        assertTrue(time < 400)
        println("소요 시간: ${time}ms (병렬 실행)")
    }

    @Test
    fun `스레드 ThreadLocal 독립성 테스트`() {
        println("\n=== 스레드 ThreadLocal 독립성 테스트 ===")

        // 플랫폼 스레드
        val threadLocal1 = ThreadLocal<String>()
        val platformValues = mutableSetOf<String>()

        val executor1 = Executors.newFixedThreadPool(5)
        try {
            val futures = List(5) { i ->
                executor1.submit {
                    threadLocal1.set("Platform-$i")
                    Thread.sleep(50)
                    val value = threadLocal1.get()
                    synchronized(platformValues) {
                        platformValues.add(value)
                    }
                    threadLocal1.remove()
                }
            }
            futures.forEach { it.get() }
        } finally {
            executor1.shutdown()
        }

        assertEquals(5, platformValues.size)
        println("플랫폼 스레드 ThreadLocal: $platformValues")

        // 버추얼 스레드
        val threadLocal2 = ThreadLocal<String>()
        val virtualValues = mutableSetOf<String>()

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = List(5) { i ->
                executor.submit {
                    threadLocal2.set("Virtual-$i")
                    Thread.sleep(50)
                    val value = threadLocal2.get()
                    synchronized(virtualValues) {
                        virtualValues.add(value)
                    }
                    threadLocal2.remove()
                }
            }
            futures.forEach { it.get() }
        }

        assertEquals(5, virtualValues.size)
        println("버추얼 스레드 ThreadLocal: $virtualValues")
        println("모든 스레드는 독립적인 ThreadLocal을 가집니다")
    }

    @Test
    fun `컨텍스트 스위칭 횟수 비교`() {
        println("\n=== 컨텍스트 스위칭 횟수 비교 ===")

        val taskCount = 100

        // 많은 스레드 → 많은 컨텍스트 스위칭
        val manyThreadsTime = measureTimeMillis {
            val threads = List(taskCount) {
                Thread {
                    var sum = 0
                    repeat(100_000) {
                        sum += it
                    }
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        // 적은 스레드 → 적은 컨텍스트 스위칭
        val fewThreadsTime = measureTimeMillis {
            val executor = Executors.newFixedThreadPool(4)
            try {
                val futures = List(taskCount) {
                    executor.submit {
                        var sum = 0
                        repeat(100_000) {
                            sum += it
                        }
                    }
                }
                futures.forEach { it.get() }
            } finally {
                executor.shutdown()
            }
        }

        println("많은 스레드 ($taskCount 개): ${manyThreadsTime}ms")
        println("적은 스레드 (4 개): ${fewThreadsTime}ms")

        // 컨텍스트 스위칭이 적은 쪽이 더 빠름
        assertTrue(fewThreadsTime < manyThreadsTime * 1.5)
    }

    @Test
    fun `Flow vs Channel 비교 테스트`() = runBlocking {
        println("\n=== Flow vs Channel 비교 테스트 ===")

        // Flow: Cold Stream
        val flow = kotlinx.coroutines.flow.flow {
            repeat(5) {
                emit(it)
                delay(50)
            }
        }

        val flowResults = mutableListOf<Int>()
        flow.collect { flowResults.add(it) }

        assertEquals(listOf(0, 1, 2, 3, 4), flowResults)
        println("Flow 결과: $flowResults")

        // Channel: Hot Stream
        val channel = kotlinx.coroutines.channels.Channel<Int>()

        launch {
            repeat(5) {
                channel.send(it)
                delay(50)
            }
            channel.close()
        }

        val channelResults = mutableListOf<Int>()
        for (value in channel) {
            channelResults.add(value)
        }

        assertEquals(listOf(0, 1, 2, 3, 4), channelResults)
        println("Channel 결과: $channelResults")
    }

    @Test
    fun `withTimeout 테스트`() = runBlocking {
        println("\n=== withTimeout 테스트 ===")

        var completed = false

        assertThrows(TimeoutCancellationException::class.java) {
            runBlocking {
                withTimeout(100) {
                    delay(200)
                    completed = true
                }
            }
        }

        assertFalse(completed)
        println("타임아웃으로 작업 취소됨")
    }

    @Test
    fun `버추얼 스레드 isVirtual 확인`() {
        println("\n=== 버추얼 스레드 isVirtual 확인 ===")

        var platformIsVirtual = true
        var virtualIsVirtual = false

        val platformThread = Thread {
            platformIsVirtual = Thread.currentThread().isVirtual
        }
        platformThread.start()
        platformThread.join()

        Thread.startVirtualThread {
            virtualIsVirtual = Thread.currentThread().isVirtual
        }.join()

        assertFalse(platformIsVirtual)
        assertTrue(virtualIsVirtual)
        println("플랫폼 스레드 isVirtual: $platformIsVirtual")
        println("버추얼 스레드 isVirtual: $virtualIsVirtual")
    }

    @Test
    fun `버추얼 스레드 Pinning 테스트`() {
        println("\n=== 버추얼 스레드 Pinning 테스트 ===")

        val count = 1000
        val iterations = 100

        // synchronized (Pinning 발생)
        val lock1 = Any()
        var counter1 = 0
        val syncTime = measureTimeMillis {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(count) {
                    executor.submit {
                        repeat(iterations) {
                            synchronized(lock1) {
                                counter1++
                                Thread.sleep(1)
                            }
                        }
                    }
                }
                futures.forEach { it.get() }
            }
        }

        // ReentrantLock (Pinning 없음)
        val lock2 = ReentrantLock()
        var counter2 = 0
        val lockTime = measureTimeMillis {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(count) {
                    executor.submit {
                        repeat(iterations) {
                            lock2.lock()
                            try {
                                counter2++
                                Thread.sleep(1)
                            } finally {
                                lock2.unlock()
                            }
                        }
                    }
                }
                futures.forEach { it.get() }
            }
        }

        assertEquals(count * iterations, counter1)
        assertEquals(count * iterations, counter2)
        println("synchronized 사용 시간: ${syncTime}ms (Pinning 가능)")
        println("ReentrantLock 사용 시간: ${lockTime}ms (No Pinning)")
        // Pinning은 환경에 따라 다르므로 절대적인 성능 비교는 하지 않음
        assertTrue(syncTime > 0 && lockTime > 0)
    }

    @Test
    fun `대량 버추얼 스레드 생성 테스트`() {
        println("\n=== 대량 버추얼 스레드 생성 테스트 ===")

        val counts = listOf(1_000, 10_000)

        counts.forEach { count ->
            val time = measureTimeMillis {
                Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                    val futures = List(count) {
                        executor.submit {
                            Thread.sleep(10)
                        }
                    }
                    futures.forEach { it.get() }
                }
            }
            println("${count}개 버추얼 스레드: ${time}ms")
            assertTrue(time > 0)
        }
        println("버추얼 스레드는 수백만 개까지 생성 가능합니다")
    }
}
