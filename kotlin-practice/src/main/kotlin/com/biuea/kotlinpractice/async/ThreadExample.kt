package com.biuea.kotlinpractice.async

import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis

/**
 * 플랫폼 스레드 예제
 */
object ThreadExample {

    /**
     * 기본 스레드 생성 및 실행
     */
    fun basicThreadExample() {
        println("\n=== 기본 스레드 예제 ===")

        val thread = Thread {
            println("스레드 이름: ${Thread.currentThread().name}")
            println("스레드 ID: ${Thread.currentThread().id}")
            Thread.sleep(100)
            println("작업 완료")
        }

        thread.start()
        thread.join() // 스레드 종료 대기
    }

    /**
     * 스레드 풀 사용 예제
     */
    fun threadPoolExample() {
        println("\n=== 스레드 풀 예제 ===")

        val executor = Executors.newFixedThreadPool(3)
        val latch = CountDownLatch(5)

        val startTime = System.currentTimeMillis()

        repeat(5) { i ->
            executor.submit {
                println("[Task-$i] 시작 - 스레드: ${Thread.currentThread().name}")
                Thread.sleep(200) // I/O 작업 시뮬레이션
                println("[Task-$i] 완료")
                latch.countDown()
            }
        }

        latch.await() // 모든 작업 완료 대기
        executor.shutdown()

        val duration = System.currentTimeMillis() - startTime
        println("총 소요 시간: ${duration}ms")
    }

    /**
     * 공유 상태 동기화 - 문제 발생 예제
     */
    fun sharedStateWithoutSyncExample() {
        println("\n=== 동기화 없는 공유 상태 (Race Condition) ===")

        var counter = 0
        val threads = List(10) {
            Thread {
                repeat(1000) {
                    counter++ // Race Condition 발생!
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        println("예상 값: 10000, 실제 값: $counter")
        println("Race Condition으로 인한 데이터 손실 발생!")
    }

    /**
     * synchronized를 이용한 동기화
     */
    fun synchronizedExample() {
        println("\n=== synchronized 동기화 예제 ===")

        val lock = Any()
        var counter = 0

        val time = measureTimeMillis {
            val threads = List(10) {
                Thread {
                    repeat(1000) {
                        synchronized(lock) {
                            counter++
                        }
                    }
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        println("예상 값: 10000, 실제 값: $counter")
        println("소요 시간: ${time}ms")
    }

    /**
     * AtomicInteger를 이용한 동기화
     */
    fun atomicExample() {
        println("\n=== AtomicInteger 동기화 예제 ===")

        val counter = AtomicInteger(0)

        val time = measureTimeMillis {
            val threads = List(10) {
                Thread {
                    repeat(1000) {
                        counter.incrementAndGet()
                    }
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        println("예상 값: 10000, 실제 값: ${counter.get()}")
        println("소요 시간: ${time}ms")
    }

    /**
     * ReentrantLock을 이용한 동기화
     */
    fun reentrantLockExample() {
        println("\n=== ReentrantLock 동기화 예제 ===")

        val lock = ReentrantLock()
        var counter = 0

        val time = measureTimeMillis {
            val threads = List(10) {
                Thread {
                    repeat(1000) {
                        lock.withLock {
                            counter++
                        }
                    }
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        println("예상 값: 10000, 실제 값: $counter")
        println("소요 시간: ${time}ms")
    }

    /**
     * ThreadLocal 예제
     */
    fun threadLocalExample() {
        println("\n=== ThreadLocal 예제 ===")

        val threadLocal = ThreadLocal<String>()

        val threads = List(3) { i ->
            Thread {
                threadLocal.set("Thread-$i-Data")
                println("[${Thread.currentThread().name}] ThreadLocal 값: ${threadLocal.get()}")
                Thread.sleep(100)
                println("[${Thread.currentThread().name}] 100ms 후 값: ${threadLocal.get()}")
                threadLocal.remove() // 반드시 정리
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }

    /**
     * 스레드 인터럽트 예제
     */
    fun threadInterruptExample() {
        println("\n=== 스레드 인터럽트 예제 ===")

        val thread = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    println("작업 중...")
                    Thread.sleep(200)
                }
            } catch (e: InterruptedException) {
                println("스레드가 인터럽트되었습니다.")
            }
        }

        thread.start()
        Thread.sleep(600)
        thread.interrupt() // 인터럽트 발생
        thread.join()
    }

    /**
     * 컨텍스트 스위칭 비용 측정
     */
    fun contextSwitchingCostExample() {
        println("\n=== 컨텍스트 스위칭 비용 측정 ===")

        val threadCounts = listOf(1, 5, 10, 50, 100)
        val tasksPerThread = 100

        threadCounts.forEach { threadCount ->
            val time = measureTimeMillis {
                val latch = CountDownLatch(threadCount)
                val threads = List(threadCount) {
                    Thread {
                        repeat(tasksPerThread) {
                            // CPU 작업 시뮬레이션
                            var sum = 0
                            for (i in 1..1000) {
                                sum += i
                            }
                        }
                        latch.countDown()
                    }
                }

                threads.forEach { it.start() }
                latch.await()
            }

            println("스레드 수: $threadCount, 소요 시간: ${time}ms")
        }
    }

    /**
     * 데드락 예제
     */
    fun deadlockExample() {
        println("\n=== 데드락 예제 ===")

        val lock1 = Any()
        val lock2 = Any()

        val thread1 = Thread {
            synchronized(lock1) {
                println("Thread1: lock1 획득")
                Thread.sleep(100)
                println("Thread1: lock2 획득 대기...")
                synchronized(lock2) {
                    println("Thread1: lock2 획득")
                }
            }
        }

        val thread2 = Thread {
            synchronized(lock2) {
                println("Thread2: lock2 획득")
                Thread.sleep(100)
                println("Thread2: lock1 획득 대기...")
                synchronized(lock1) {
                    println("Thread2: lock1 획득")
                }
            }
        }

        thread1.start()
        thread2.start()

        // 2초 대기 후 강제 종료
        Thread.sleep(2000)
        println("\n데드락 발생으로 인해 스레드가 무한 대기 상태입니다.")
        println("실제 환경에서는 스레드 덤프 분석이 필요합니다.")

        @Suppress("DEPRECATION")
        thread1.stop()
        @Suppress("DEPRECATION")
        thread2.stop()
    }
}

/*
fun main() {
    ThreadExample.basicThreadExample()
    ThreadExample.threadPoolExample()
    ThreadExample.sharedStateWithoutSyncExample()
    ThreadExample.synchronizedExample()
    ThreadExample.atomicExample()
    ThreadExample.reentrantLockExample()
    ThreadExample.threadLocalExample()
    ThreadExample.threadInterruptExample()
    ThreadExample.contextSwitchingCostExample()

    println("\n데드락 예제는 주석을 해제하여 실행하세요:")
    println("// ThreadExample.deadlockExample()")
}
*/
