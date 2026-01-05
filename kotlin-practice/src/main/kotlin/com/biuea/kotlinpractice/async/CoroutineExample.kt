package com.biuea.kotlinpractice.async

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.system.measureTimeMillis

/**
 * 코루틴 예제
 */
object CoroutineExample {

    /**
     * 기본 코루틴 실행
     */
    fun basicCoroutineExample() = runBlocking {
        println("\n=== 기본 코루틴 예제 ===")

        launch {
            delay(100)
            println("코루틴 실행 완료")
        }

        println("메인 함수 실행")
    }

    /**
     * 여러 코루틴 동시 실행
     */
    fun multipleCoroutinesExample() = runBlocking {
        println("\n=== 여러 코루틴 동시 실행 ===")

        val time = measureTimeMillis {
            val jobs = List(5) { i ->
                launch {
                    println("[Coroutine-$i] 시작 - 스레드: ${Thread.currentThread().name}")
                    delay(200)
                    println("[Coroutine-$i] 완료")
                }
            }
            jobs.forEach { it.join() }
        }

        println("총 소요 시간: ${time}ms (병렬 실행으로 ~200ms)")
    }

    /**
     * async/await 패턴
     */
    fun asyncAwaitExample() = runBlocking {
        println("\n=== async/await 예제 ===")

        val time = measureTimeMillis {
            val deferred1 = async {
                delay(200)
                println("작업 1 완료")
                100
            }

            val deferred2 = async {
                delay(300)
                println("작업 2 완료")
                200
            }

            val result = deferred1.await() + deferred2.await()
            println("결과: $result")
        }

        println("총 소요 시간: ${time}ms (병렬 실행으로 ~300ms)")
    }

    /**
     * Dispatcher 비교
     */
    fun dispatcherExample() = runBlocking {
        println("\n=== Dispatcher 비교 ===")

        // Default: CPU 집약 작업
        launch(Dispatchers.Default) {
            println("Default - 스레드: ${Thread.currentThread().name}")
            var sum = 0
            repeat(1000) { sum += it }
        }

        // IO: I/O 작업
        launch(Dispatchers.IO) {
            println("IO - 스레드: ${Thread.currentThread().name}")
            delay(100)
        }

        // Unconfined: 호출 스레드에서 시작
        launch(Dispatchers.Unconfined) {
            println("Unconfined (시작) - 스레드: ${Thread.currentThread().name}")
            delay(100)
            println("Unconfined (재개) - 스레드: ${Thread.currentThread().name}")
        }

        delay(500)
    }

    /**
     * 구조화된 동시성 (Structured Concurrency)
     */
    fun structuredConcurrencyExample() = runBlocking {
        println("\n=== 구조화된 동시성 ===")

        val time = measureTimeMillis {
            coroutineScope {
                launch {
                    delay(200)
                    println("자식 코루틴 1 완료")
                }

                launch {
                    delay(300)
                    println("자식 코루틴 2 완료")
                }
                // coroutineScope는 모든 자식이 완료될 때까지 대기
            }
            println("모든 자식 코루틴 완료")
        }

        println("소요 시간: ${time}ms")
    }

    /**
     * 코루틴 취소
     */
    fun coroutineCancellationExample() = runBlocking {
        println("\n=== 코루틴 취소 예제 ===")

        val job = launch {
            try {
                repeat(10) { i ->
                    println("작업 중... $i")
                    delay(100)
                }
            } catch (e: CancellationException) {
                println("코루틴이 취소되었습니다.")
            } finally {
                println("정리 작업 실행")
            }
        }

        delay(350)
        println("코루틴 취소 요청")
        job.cancelAndJoin()
        println("코루틴 취소 완료")
    }

    /**
     * CPU 집약 작업 취소 - isActive 체크
     */
    fun cpuIntensiveCancellationExample() = runBlocking {
        println("\n=== CPU 집약 작업 취소 예제 ===")

        val job = launch(Dispatchers.Default) {
            var i = 0
            while (isActive) { // isActive 체크 필수!
                i++
                if (i % 100_000_000 == 0) {
                    println("계산 중... $i")
                }
            }
            println("작업이 취소되었습니다.")
        }

        delay(500)
        job.cancelAndJoin()
        println("코루틴 취소 완료")
    }

    /**
     * 예외 처리 - SupervisorScope
     */
    fun exceptionHandlingExample() = runBlocking {
        println("\n=== 예외 처리 (supervisorScope) ===")

        supervisorScope {
            val job1 = launch {
                delay(100)
                println("작업 1 완료")
            }

            val job2 = launch {
                delay(50)
                throw RuntimeException("작업 2 실패!")
            }

            val job3 = launch {
                delay(150)
                println("작업 3 완료")
            }

            try {
                job2.join()
            } catch (e: Exception) {
                println("작업 2에서 예외 발생: ${e.message}")
            }

            job1.join()
            job3.join()
        }

        println("supervisorScope로 다른 작업은 계속 실행됨")
    }

    /**
     * CoroutineExceptionHandler
     */
    fun coroutineExceptionHandlerExample() = runBlocking {
        println("\n=== CoroutineExceptionHandler ===")

        val handler = CoroutineExceptionHandler { _, exception ->
            println("예외 처리: ${exception.message}")
        }

        val job = GlobalScope.launch(handler) {
            throw RuntimeException("예외 발생!")
        }

        job.join()
    }

    /**
     * Channel 예제
     */
    fun channelExample() = runBlocking {
        println("\n=== Channel 예제 ===")

        val channel = Channel<Int>()

        launch {
            repeat(5) {
                println("전송: $it")
                channel.send(it)
                delay(100)
            }
            channel.close()
        }

        for (value in channel) {
            println("수신: $value")
        }
    }

    /**
     * Flow 예제
     */
    fun flowExample() = runBlocking {
        println("\n=== Flow 예제 ===")

        val flow = flow {
            repeat(5) {
                emit(it)
                delay(100)
            }
        }

        flow.collect { value ->
            println("수집: $value")
        }
    }

    /**
     * Flow 연산자
     */
    fun flowOperatorsExample() = runBlocking {
        println("\n=== Flow 연산자 예제 ===")

        val time = measureTimeMillis {
            flow {
                repeat(10) {
                    emit(it)
                    delay(50)
                }
            }
                .filter { it % 2 == 0 }
                .map { it * it }
                .take(3)
                .collect { value ->
                    println("결과: $value")
                }
        }

        println("소요 시간: ${time}ms")
    }

    /**
     * 블로킹 호출 처리
     */
    fun blockingCallExample() = runBlocking {
        println("\n=== 블로킹 호출 처리 ===")

        val time = measureTimeMillis {
            // 잘못된 방법
            launch {
                Thread.sleep(200) // 스레드 블로킹!
                println("블로킹 호출 완료")
            }

            // 올바른 방법
            launch {
                delay(200) // 코루틴 중단
                println("논블로킹 호출 완료")
            }

            // 블로킹 API를 사용해야 하는 경우
            launch {
                withContext(Dispatchers.IO) {
                    Thread.sleep(200) // IO Dispatcher에서 실행
                    println("IO Dispatcher에서 블로킹 호출 완료")
                }
            }

            delay(300)
        }

        println("소요 시간: ${time}ms")
    }

    /**
     * withTimeout 예제
     */
    fun withTimeoutExample() = runBlocking {
        println("\n=== withTimeout 예제 ===")

        try {
            withTimeout(500) {
                repeat(10) {
                    println("작업 중... $it")
                    delay(100)
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("타임아웃으로 작업 취소")
        }
    }

    /**
     * 먼저 완료되는 작업 처리 예제
     */
    fun raceExample() = runBlocking {
        println("\n=== 먼저 완료되는 작업 처리 예제 ===")

        // 여러 작업 중 먼저 완료되는 것만 사용
        val result = coroutineScope {
            val deferred1 = async {
                delay(300)
                "결과 1"
            }

            val deferred2 = async {
                delay(200)
                "결과 2"
            }

            // 간단한 구현: 타임아웃을 이용한 경쟁
            val result2 = deferred2.await()
            deferred1.cancel()
            result2
        }

        println("먼저 완료된 결과: $result")
    }

    /**
     * 코루틴 컨텍스트 전파
     */
    fun coroutineContextPropagationExample() = runBlocking {
        println("\n=== 코루틴 컨텍스트 전파 ===")

        val customContext = CoroutineName("CustomCoroutine")

        launch(customContext) {
            println("부모 - ${coroutineContext[CoroutineName]}")

            launch {
                println("자식 - ${coroutineContext[CoroutineName]}")
            }
        }

        delay(100)
    }

    /**
     * 성능 비교: 코루틴 vs 스레드
     */
    fun performanceComparisonExample() = runBlocking {
        println("\n=== 성능 비교: 코루틴 vs 스레드 ===")

        val taskCount = 10_000

        // 코루틴
        val coroutineTime = measureTimeMillis {
            val jobs = List(taskCount) {
                launch {
                    delay(1)
                }
            }
            jobs.forEach { it.join() }
        }

        println("코루틴 $taskCount 개 실행 시간: ${coroutineTime}ms")

        // 스레드 (적은 수로 테스트)
        val threadCount = 100
        val threadTime = measureTimeMillis {
            val threads = List(threadCount) {
                Thread {
                    Thread.sleep(1)
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        println("스레드 $threadCount 개 실행 시간: ${threadTime}ms")
        println("코루틴이 약 ${threadTime / (coroutineTime.toDouble() / (taskCount / threadCount))}배 빠름")
    }
}

/*
suspend fun main() {
    CoroutineExample.basicCoroutineExample()
    CoroutineExample.multipleCoroutinesExample()
    CoroutineExample.asyncAwaitExample()
    CoroutineExample.dispatcherExample()
    CoroutineExample.structuredConcurrencyExample()
    CoroutineExample.coroutineCancellationExample()
    CoroutineExample.cpuIntensiveCancellationExample()
    CoroutineExample.exceptionHandlingExample()
    CoroutineExample.coroutineExceptionHandlerExample()
    CoroutineExample.channelExample()
    CoroutineExample.flowExample()
    CoroutineExample.flowOperatorsExample()
    CoroutineExample.blockingCallExample()
    CoroutineExample.withTimeoutExample()
    CoroutineExample.raceExample()
    CoroutineExample.coroutineContextPropagationExample()
    CoroutineExample.performanceComparisonExample()
}
*/
