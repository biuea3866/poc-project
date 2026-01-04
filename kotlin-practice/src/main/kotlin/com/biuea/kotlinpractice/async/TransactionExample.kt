package com.biuea.kotlinpractice.async

import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * 비동기 처리에서의 트랜잭션 예제
 *
 * 실제 데이터베이스 대신 시뮬레이션으로 구현
 */
object TransactionExample {

    /**
     * 트랜잭션 컨텍스트 시뮬레이션
     */
    class TransactionContext(
        val id: String,
        val isActive: Boolean = true
    ) {
        private val operations = mutableListOf<String>()

        fun addOperation(operation: String) {
            operations.add(operation)
        }

        fun getOperations() = operations.toList()

        fun rollback() {
            println("[$id] 롤백: ${operations.reversed()}")
        }

        fun commit() {
            println("[$id] 커밋: $operations")
        }
    }

    /**
     * ThreadLocal을 이용한 트랜잭션 관리 (기본 방식)
     */
    private val threadLocalTransaction = ThreadLocal<TransactionContext>()

    fun getTransaction(): TransactionContext? = threadLocalTransaction.get()

    fun beginTransaction(id: String): TransactionContext {
        val tx = TransactionContext(id)
        threadLocalTransaction.set(tx)
        println("[$id] 트랜잭션 시작 - 스레드: ${Thread.currentThread().name}")
        return tx
    }

    fun commitTransaction() {
        val tx = getTransaction()
        tx?.commit()
        threadLocalTransaction.remove()
    }

    fun rollbackTransaction() {
        val tx = getTransaction()
        tx?.rollback()
        threadLocalTransaction.remove()
    }

    /**
     * 문제점 1: 스레드 기반 트랜잭션에서 비동기 호출 시 컨텍스트 유실
     */
    fun threadLocalLostExample() {
        println("\n=== 문제점: ThreadLocal 트랜잭션 유실 ===")

        val tx = beginTransaction("TX-001")
        tx.addOperation("Operation 1")

        Thread {
            println("새 스레드에서 트랜잭션: ${getTransaction()}")
            println("트랜잭션 컨텍스트가 유실되었습니다!")
        }.apply {
            start()
            join()
        }

        commitTransaction()
    }

    /**
     * 해결 방안 1: 트랜잭션 명시적 전달
     */
    fun explicitTransactionPassingExample() {
        println("\n=== 해결 방안: 트랜잭션 명시적 전달 ===")

        val tx = TransactionContext("TX-002")
        tx.addOperation("Main thread operation")

        Thread {
            tx.addOperation("Child thread operation")
            println("명시적으로 전달받은 트랜잭션: ${tx.id}")
        }.apply {
            start()
            join()
        }

        tx.commit()
    }

    /**
     * 코루틴의 CoroutineContext를 이용한 트랜잭션 전파
     */
    class TransactionElement(
        val transaction: TransactionContext
    ) : CoroutineContext.Element {
        companion object Key : CoroutineContext.Key<TransactionElement>

        override val key: CoroutineContext.Key<*> = Key
    }

    suspend fun getCurrentTransaction(): TransactionContext? {
        return kotlin.coroutines.coroutineContext[TransactionElement]?.transaction
    }

    /**
     * 코루틴 트랜잭션 DSL
     */
    suspend fun <T> withTransaction(
        id: String,
        block: suspend (TransactionContext) -> T
    ): T {
        val tx = TransactionContext(id)
        println("[$id] 코루틴 트랜잭션 시작")

        return withContext(TransactionElement(tx)) {
            try {
                val result = block(tx)
                tx.commit()
                result
            } catch (e: Exception) {
                tx.rollback()
                throw e
            }
        }
    }

    /**
     * 해결 방안 2: 코루틴 CoroutineContext를 통한 전파
     */
    fun coroutineTransactionPropagationExample() = runBlocking {
        println("\n=== 해결 방안: 코루틴 트랜잭션 전파 ===")

        withTransaction("TX-003") { tx ->
            tx.addOperation("부모 코루틴 작업")

            launch {
                val currentTx = getCurrentTransaction()
                println("자식 코루틴 트랜잭션: ${currentTx?.id}")
                currentTx?.addOperation("자식 코루틴 작업 1")
                delay(100)
            }

            launch {
                val currentTx = getCurrentTransaction()
                currentTx?.addOperation("자식 코루틴 작업 2")
                delay(50)
            }

            delay(200)
            tx.addOperation("부모 코루틴 최종 작업")
        }
    }

    /**
     * 패턴 1: 트랜잭션 경계 명확화
     */
    fun transactionBoundaryExample() = runBlocking {
        println("\n=== 패턴 1: 트랜잭션 경계 명확화 ===")

        // 나쁜 예: 트랜잭션 경계 불명확
        println("나쁜 예:")
        launch {
            val tx = TransactionContext("TX-BAD")
            tx.addOperation("작업 1")

            launch {
                delay(100)
                tx.addOperation("비동기 작업")
            }

            // 비동기 작업이 완료되기 전에 커밋 가능
            tx.commit()
        }.join()

        delay(200)

        // 좋은 예: 트랜잭션 경계 명확
        println("\n좋은 예:")
        withTransaction("TX-GOOD") { tx ->
            tx.addOperation("작업 1")

            // coroutineScope는 모든 자식이 완료될 때까지 대기
            coroutineScope {
                launch {
                    delay(100)
                    tx.addOperation("비동기 작업")
                }
            }

            tx.addOperation("작업 2")
            // withTransaction 블록이 끝나면 자동 커밋
        }
    }

    /**
     * 패턴 2: 읽기/쓰기 분리
     */
    fun readWriteSeparationExample() = runBlocking {
        println("\n=== 패턴 2: 읽기/쓰기 분리 ===")

        // 읽기 작업 (병렬, 트랜잭션 없음)
        val data = coroutineScope {
            val data1 = async {
                delay(100)
                "Data 1"
            }

            val data2 = async {
                delay(150)
                "Data 2"
            }

            Pair(data1.await(), data2.await())
        }

        // 쓰기 작업 (순차, 트랜잭션)
        withTransaction("TX-004") { tx ->
            tx.addOperation("저장: ${data.first}")
            delay(50)
            tx.addOperation("저장: ${data.second}")
        }
    }

    /**
     * 패턴 3: 보상 트랜잭션 (Saga 패턴)
     */
    class SagaTransaction {
        private val completedSteps = mutableListOf<String>()
        private val compensations = mutableMapOf<String, suspend () -> Unit>()

        suspend fun executeStep(
            name: String,
            action: suspend () -> Unit,
            compensation: suspend () -> Unit
        ) {
            try {
                action()
                completedSteps.add(name)
                compensations[name] = compensation
                println("[$name] 실행 성공")
            } catch (e: Exception) {
                println("[$name] 실행 실패: ${e.message}")
                compensate()
                throw e
            }
        }

        suspend fun compensate() {
            println("\n보상 트랜잭션 시작")
            completedSteps.reversed().forEach { step ->
                compensations[step]?.invoke()
                println("[$step] 보상 완료")
            }
        }
    }

    fun sagaPatternExample() = runBlocking {
        println("\n=== 패턴 3: 보상 트랜잭션 (Saga) ===")

        val saga = SagaTransaction()

        try {
            saga.executeStep(
                name = "주문 생성",
                action = {
                    delay(100)
                    println("주문 생성 완료")
                },
                compensation = {
                    println("주문 취소")
                }
            )

            saga.executeStep(
                name = "재고 차감",
                action = {
                    delay(100)
                    println("재고 차감 완료")
                },
                compensation = {
                    println("재고 복구")
                }
            )

            saga.executeStep(
                name = "결제 처리",
                action = {
                    delay(100)
                    throw RuntimeException("결제 실패!")
                },
                compensation = {
                    println("결제 취소")
                }
            )

            println("모든 단계 완료")
        } catch (e: Exception) {
            println("\n트랜잭션 실패: ${e.message}")
        }
    }

    /**
     * 버추얼 스레드에서의 트랜잭션
     */
    fun virtualThreadTransactionExample() {
        println("\n=== 버추얼 스레드 트랜잭션 ===")

        // 부모 버추얼 스레드
        Thread.startVirtualThread {
            val tx = beginTransaction("VTX-001")
            tx.addOperation("부모 작업")

            // 자식 버추얼 스레드
            Thread.startVirtualThread {
                val childTx = getTransaction()
                println("자식 버추얼 스레드 트랜잭션: $childTx")
                println("ThreadLocal은 상속되지 않습니다!")

                // 명시적 전달 필요
                tx.addOperation("자식 작업 (명시적 전달)")
            }.join()

            commitTransaction()
        }.join()
    }

    /**
     * 실전 시나리오: 주문 처리 시스템
     */
    data class Order(val id: String, var status: String = "PENDING")

    fun orderProcessingExample() = runBlocking {
        println("\n=== 실전 시나리오: 주문 처리 ===")

        val order = Order("ORDER-001")

        withTransaction("TX-ORDER-001") { tx ->
            // 1. 주문 검증
            tx.addOperation("주문 검증: ${order.id}")
            delay(50)

            // 2. 병렬 처리 (재고 확인, 사용자 정보 조회)
            coroutineScope {
                launch {
                    delay(100)
                    tx.addOperation("재고 확인")
                }

                launch {
                    delay(80)
                    tx.addOperation("사용자 정보 조회")
                }
            }

            // 3. 주문 상태 업데이트
            order.status = "CONFIRMED"
            tx.addOperation("주문 상태 업데이트: ${order.status}")

            // 4. 결제 처리 (외부 API)
            try {
                withContext(Dispatchers.IO) {
                    delay(200)
                    tx.addOperation("결제 처리 완료")
                }
            } catch (e: Exception) {
                order.status = "FAILED"
                throw e
            }
        }

        // 트랜잭션 외부 작업 (이메일 발송)
        launch {
            delay(50)
            println("이메일 발송: ${order.id}")
        }.join()

        println("주문 처리 완료: ${order.status}")
    }

    /**
     * 동시성 제어: 낙관적 락
     */
    data class Entity(
        val id: String,
        var value: Int,
        var version: Int = 0
    )

    private val entityStore = mutableMapOf<String, Entity>()
    private val updateCounter = AtomicInteger(0)

    fun optimisticLockingExample() = runBlocking {
        println("\n=== 동시성 제어: 낙관적 락 ===")

        val entity = Entity("ENTITY-001", value = 100)
        entityStore[entity.id] = entity

        val jobs = List(10) { i ->
            launch {
                repeat(10) {
                    var success = false
                    while (!success) {
                        val current = entityStore[entity.id]!!
                        val updated = current.copy(
                            value = current.value + 1,
                            version = current.version + 1
                        )

                        // 버전 체크 (CAS 시뮬레이션)
                        synchronized(entityStore) {
                            if (entityStore[entity.id]!!.version == current.version) {
                                entityStore[entity.id] = updated
                                updateCounter.incrementAndGet()
                                success = true
                            }
                        }

                        if (!success) {
                            delay(1) // 재시도 전 대기
                        }
                    }
                }
            }
        }

        jobs.forEach { it.join() }

        println("최종 값: ${entityStore[entity.id]!!.value}")
        println("최종 버전: ${entityStore[entity.id]!!.version}")
        println("업데이트 시도 횟수: ${updateCounter.get()}")
    }

    /**
     * 동시성 제어: 비관적 락
     */
    fun pessimisticLockingExample() = runBlocking {
        println("\n=== 동시성 제어: 비관적 락 ===")

        val entity = Entity("ENTITY-002", value = 0)
        val lock = Any()

        val jobs = List(10) {
            launch {
                repeat(10) {
                    synchronized(lock) {
                        entity.value++
                        entity.version++
                    }
                    delay(1)
                }
            }
        }

        jobs.forEach { it.join() }

        println("최종 값: ${entity.value}")
        println("최종 버전: ${entity.version}")
    }
}

/*
suspend fun main() {
    TransactionExample.threadLocalLostExample()
    TransactionExample.explicitTransactionPassingExample()
    TransactionExample.coroutineTransactionPropagationExample()
    TransactionExample.transactionBoundaryExample()
    TransactionExample.readWriteSeparationExample()
    TransactionExample.sagaPatternExample()
    TransactionExample.virtualThreadTransactionExample()
    TransactionExample.orderProcessingExample()
    TransactionExample.optimisticLockingExample()
    TransactionExample.pessimisticLockingExample()
}
*/
