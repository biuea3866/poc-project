package com.biuea.kotlinpractice.async

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis

/**
 * 버추얼 스레드 예제 (Java 21+)
 */
object VirtualThreadExample {

    /**
     * 기본 버추얼 스레드 생성
     */
    fun basicVirtualThreadExample() {
        println("\n=== 기본 버추얼 스레드 예제 ===")

        Thread.startVirtualThread {
            println("버추얼 스레드 실행")
            println("스레드: ${Thread.currentThread()}")
            println("isVirtual: ${Thread.currentThread().isVirtual}")
            Thread.sleep(100)
            println("작업 완료")
        }.join()
    }

    /**
     * 버추얼 스레드 팩토리 사용
     */
    fun virtualThreadFactoryExample() {
        println("\n=== 버추얼 스레드 팩토리 예제 ===")

        val factory = Thread.ofVirtual()
            .name("VirtualWorker-", 0)
            .factory()

        val threads = List(5) { i ->
            factory.newThread {
                println("[Task-$i] 스레드: ${Thread.currentThread().name}")
                Thread.sleep(200)
                println("[Task-$i] 완료")
            }
        }

        val time = measureTimeMillis {
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        println("총 소요 시간: ${time}ms")
    }

    /**
     * 버추얼 스레드 Executor 사용
     */
    fun virtualThreadExecutorExample() {
        println("\n=== 버추얼 스레드 Executor 예제 ===")

        val factory = Thread.ofVirtual()
            .name("VirtualExecutor-", 0)
            .factory()

        Executors.newThreadPerTaskExecutor(factory).use { executor ->
            val time = measureTimeMillis {
                val futures = List(5) {
                    executor.submit {
                        println("[$it] 스레드: ${Thread.currentThread().name}")
                        Thread.sleep(100)
                    }
                }

                futures.forEach { it.get() }
                println("5개 버추얼 스레드 실행 완료")
            }

            println("총 소요 시간: ${time}ms")
        }
    }

    /**
     * 버추얼 스레드 전역 설정 사용
     */
    fun globalVirtualThreadExecutorExample() {
        println("\n=== spring.virtual.threads.enabled=true Executor 예제 ===")

        List(5) {
            println("[$it] 스레드: ${Thread.currentThread().name}")
            Thread.sleep(100)
        }
    }

    /**
     * Pinning 문제 - synchronized 사용
     */
    fun pinningWithSynchronizedExample() {
        println("\n=== Pinning 문제 (synchronized) ===")

        val lock = Any()
        var counter = 0

        val time = measureTimeMillis {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(1000) {
                    executor.submit {
                        repeat(100) {
                            synchronized(lock) {
                                counter++
                                Thread.sleep(1)
                            }
                        }
                    }
                }
                futures.forEach { it.get() }
            }
        }

        println("Counter: $counter")
        println("Pinning으로 인한 소요 시간: ${time}ms (느림)")
    }

    /**
     * ReentrantLock 사용으로 Pinning 방지
     */
    fun noPinningWithReentrantLockExample() {
        println("\n=== ReentrantLock으로 Pinning 방지 ===")

        val lock = ReentrantLock()
        var counter = 0

        val time = measureTimeMillis {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(1000) {
                    executor.submit {
                        repeat(100) {
                            lock.withLock {
                                counter++
                                Thread.sleep(1)
                            }
                        }
                    }
                }
                futures.forEach { it.get() }
            }
        }

        println("Counter: $counter")
        println("ReentrantLock 사용 소요 시간: ${time}ms (빠름)")
    }

    /**
     * ThreadLocal 사용 예제
     */
    fun threadLocalExample() {
        println("\n=== 버추얼 스레드 ThreadLocal 예제 ===")

        val threadLocal = ThreadLocal<String>()

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = List(5) { i ->
                executor.submit {
                    threadLocal.set("VirtualThread-$i-Data")
                    println("[${Thread.currentThread().name}] ThreadLocal: ${threadLocal.get()}")
                    Thread.sleep(100)
                    println("[${Thread.currentThread().name}] 100ms 후: ${threadLocal.get()}")
                    threadLocal.remove()
                }
            }
            futures.forEach { it.get() }
        }

        println("각 버추얼 스레드는 독립적인 ThreadLocal을 가집니다")
    }

    /**
     * 캐리어 스레드 확인
     */
    fun carrierThreadExample() {
        println("\n=== 캐리어 스레드 확인 ===")

        val carrierThreads = mutableSetOf<String>()

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = List(100) {
                executor.submit {
                    synchronized(carrierThreads) {
                        // 버추얼 스레드 정보 수집
                        val threadInfo = Thread.currentThread().toString()
                        carrierThreads.add(threadInfo)
                    }
                    Thread.sleep(10)
                }
            }
            futures.forEach { it.get() }
        }

        println("생성된 버추얼 스레드: 100개")
        println("캐리어 스레드는 ForkJoinPool이 관리합니다")
        println("CPU 코어 수: ${Runtime.getRuntime().availableProcessors()}개")
    }

    /**
     * I/O 작업 시뮬레이션
     */
    fun ioIntensiveExample() {
        println("\n=== I/O 집약적 작업 예제 ===")

        val time = measureTimeMillis {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(1000) { i ->
                    executor.submit {
                        Thread.sleep(100)
                        "Result-$i"
                    }
                }

                val results = futures.map { it.get() }
                println("${results.size}개 I/O 작업 완료")
            }
        }

        println("총 소요 시간: ${time}ms")
        println("버추얼 스레드로 높은 동시성 달성")
    }

    /**
     * 플랫폼 스레드 vs 버추얼 스레드 비교
     */
    fun platformVsVirtualThreadExample() {
        println("\n=== 플랫폼 스레드 vs 버추얼 스레드 비교 ===")

        val taskCount = 10_000
        val sleepTime = 100L

        // 플랫폼 스레드 (제한된 풀 사용)
        val platformTime = measureTimeMillis {
            val executor = Executors.newFixedThreadPool(100)
            try {
                val futures = List(taskCount) {
                    executor.submit {
                        Thread.sleep(sleepTime)
                    }
                }
                futures.forEach { it.get() }
            } finally {
                executor.shutdown()
            }
        }

        // 버추얼 스레드
        val virtualTime = measureTimeMillis {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(taskCount) {
                    executor.submit {
                        Thread.sleep(sleepTime)
                    }
                }
                futures.forEach { it.get() }
            }
        }

        println("플랫폼 스레드 (풀 크기 100) 소요 시간: ${platformTime}ms")
        println("버추얼 스레드 소요 시간: ${virtualTime}ms")
        println("성능 향상: ${(platformTime.toDouble() / virtualTime).format(2)}배")
    }

    /**
     * 메모리 효율성 테스트
     */
    fun memoryEfficiencyExample() {
        println("\n=== 메모리 효율성 테스트 ===")

        val runtime = Runtime.getRuntime()
        System.gc()
        Thread.sleep(100)

        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        println("초기 메모리 사용량: ${initialMemory / 1024 / 1024}MB")

        // 대량의 버추얼 스레드 생성
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = List(100_000) {
                executor.submit {
                    Thread.sleep(1000)
                }
            }

            Thread.sleep(200) // 스레드 생성 대기

            val peakMemory = runtime.totalMemory() - runtime.freeMemory()
            println("100,000개 버추얼 스레드 생성 후 메모리: ${peakMemory / 1024 / 1024}MB")
            println("증가량: ${(peakMemory - initialMemory) / 1024 / 1024}MB")

            futures.forEach { it.cancel(true) }
        }

        println("버추얼 스레드는 매우 적은 메모리를 사용합니다")
    }

    /**
     * CPU 집약적 작업 (부적합한 경우)
     */
    fun cpuIntensiveExample() {
        println("\n=== CPU 집약적 작업 (부적합) ===")

        val taskCount = 100

        val virtualTime = measureTimeMillis {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(taskCount) {
                    executor.submit {
                        var sum = 0L
                        repeat(10_000_000) {
                            sum += it
                        }
                        sum
                    }
                }
                futures.forEach { it.get() }
            }
        }

        val platformTime = measureTimeMillis {
            val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            try {
                val futures = List(taskCount) {
                    executor.submit {
                        var sum = 0L
                        repeat(10_000_000) {
                            sum += it
                        }
                        sum
                    }
                }
                futures.forEach { it.get() }
            } finally {
                executor.shutdown()
            }
        }

        println("버추얼 스레드 소요 시간: ${virtualTime}ms")
        println("플랫폼 스레드 소요 시간: ${platformTime}ms")
        println("CPU 집약 작업에는 플랫폼 스레드가 더 효율적입니다")
    }

    /**
     * 대량 동시 작업 처리
     */
    fun massiveConcurrencyExample() {
        println("\n=== 대량 동시 작업 처리 ===")

        val counts = listOf(1_000, 10_000, 100_000)

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
        }
    }

    /**
     * 버추얼 스레드 생성 비용 측정
     */
    fun creationCostExample() {
        println("\n=== 스레드 생성 비용 측정 ===")

        val count = 10_000

        // 플랫폼 스레드
        val platformTime = measureTimeMillis {
            val threads = List(count) {
                Thread {
                    Thread.sleep(1)
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        // 버추얼 스레드
        val virtualTime = measureTimeMillis {
            val threads = List(count) {
                Thread.startVirtualThread {
                    Thread.sleep(1)
                }
            }
            threads.forEach { it.join() }
        }

        println("플랫폼 스레드 ${count}개 생성: ${platformTime}ms")
        println("버추얼 스레드 ${count}개 생성: ${virtualTime}ms")
        println("생성 속도 차이: ${(platformTime.toDouble() / virtualTime).format(2)}배")
    }

    /**
     * isVirtual 확인
     */
    fun checkIsVirtualExample() {
        println("\n=== isVirtual 확인 ===")

        val platformThread = Thread {
            println("플랫폼 스레드 isVirtual: ${Thread.currentThread().isVirtual}")
        }
        platformThread.start()
        platformThread.join()

        Thread.startVirtualThread {
            println("버추얼 스레드 isVirtual: ${Thread.currentThread().isVirtual}")
        }.join()
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}

fun main() {
    VirtualThreadExample.basicVirtualThreadExample()
    VirtualThreadExample.virtualThreadFactoryExample()
    VirtualThreadExample.virtualThreadExecutorExample()
    VirtualThreadExample.globalVirtualThreadExecutorExample()

    println("\n=== Pinning 비교 (synchronized vs ReentrantLock) ===")
    VirtualThreadExample.pinningWithSynchronizedExample()
    VirtualThreadExample.noPinningWithReentrantLockExample()

    VirtualThreadExample.threadLocalExample()
    VirtualThreadExample.carrierThreadExample()
    VirtualThreadExample.ioIntensiveExample()
    VirtualThreadExample.platformVsVirtualThreadExample()
    VirtualThreadExample.cpuIntensiveExample()
    VirtualThreadExample.massiveConcurrencyExample()
    VirtualThreadExample.creationCostExample()
    VirtualThreadExample.checkIsVirtualExample()
}

@RestController
class VirtualThreadController {
    // Spring Boot에서 spring.virtual.threads.enabled=true 설정 시
    // 모든 @Async, Web 요청이 버추얼 스레드로 처리됩니다.
    @GetMapping("/virtual-thread-test")
    fun virtualThreadTest(): String {
        return "스레드: ${Thread.currentThread().name}, isVirtual: ${Thread.currentThread().isVirtual}"
    }
}