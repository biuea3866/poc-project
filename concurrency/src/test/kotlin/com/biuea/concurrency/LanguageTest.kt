package com.biuea.concurrency

import com.biuea.concurrency.language.ReentrantLockView
import com.biuea.concurrency.language.SemaphoreLockView
import com.biuea.concurrency.language.SequenceCoroutineView
import com.biuea.concurrency.language.SynchronizedView
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.time.measureTime

class LanguageTest {
    @Test
    fun `락 없이 실행`() {
        val threadPools = Executors.newFixedThreadPool(100)
        val latch = CountDownLatch(10000)
        val viewPage = ViewPage()

        val measure = measureTime {
            repeat(10000) {
                threadPools.submit {
                    try {
                        viewPage.view(true)
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }

        latch.await()

        println("measureTime: $measure")
        Assertions.assertEquals(10000, viewPage.count)
    }

    @Test
    fun `Synchronized 테스트`() {
        val threadPools = Executors.newFixedThreadPool(100)
        val latch = CountDownLatch(10000)
        val synchronized = SynchronizedView()

        val measure = measureTime {
            repeat(10000) {
                threadPools.submit {
                    try {
                        synchronized.lock(true)
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }
        latch.await()

        println("measureTime: $measure")
        Assertions.assertEquals(10000, synchronized.count)
    }

    @Test
    fun `ReentrantLock 테스트`() {
        val threadPools = Executors.newFixedThreadPool(100)
        val latch = CountDownLatch(10000)
        val reentrantLockView = ReentrantLockView()

        val measure = measureTime {
            repeat(10000) {
                threadPools.submit {
                    try {
                        reentrantLockView.lock(true)
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }
        latch.await()

        println("measureTime: $measure")
        Assertions.assertEquals(10000, reentrantLockView.count)
    }

    @Test
    fun `Volatile 테스트`() {
        val threadPools = Executors.newFixedThreadPool(100)
        val latch = CountDownLatch(10000)
        val volatileViewPage = VolatileViewPage()

        val measure = measureTime {
            repeat(10000) {
                threadPools.submit {
                    try {
                        volatileViewPage.view()
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }
        latch.await()

        println("measureTime: $measure")
        Assertions.assertEquals(10000, volatileViewPage.count)
    }

    @Test
    fun `CAS vs Synchronized 비교`() {
        val threadPools = Executors.newFixedThreadPool(100)
        val latch = CountDownLatch(1_000_000)
        val synchronized = SynchronizedView()

        val synchronizedMeasure = measureTime {
            repeat(1_000_000) {
                threadPools.submit {
                    try {
                        synchronized.lock(false)
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }
        latch.await()

        Assertions.assertEquals(1_000_000, synchronized.count)

        val latch2 = CountDownLatch(1_000_000)
        val cas = AtomicView()
        val casMeasure = measureTime {
            repeat(1_000_000) {
                threadPools.submit {
                    try {
                        cas.view(false)
                    } finally {
                        latch2.countDown()
                    }
                }
            }
        }

        latch2.await()
        Assertions.assertEquals(1_000_000, cas.count.get())
        println("synchronizedMeasureTime: $synchronizedMeasure vs casMeasureTime: $casMeasure")
    }

    @Test
    fun `세마포어 테스트`() {
        val threadPools = Executors.newFixedThreadPool(3)
        val latch = CountDownLatch(500)
        val semaphore = SemaphoreLockView()

        val synchronizedMeasure = measureTime {
            repeat(500) {
                threadPools.submit {
                    try {
                        semaphore.lock(true)
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }
        latch.await()
    }

    @Test
    fun `코루틴 테스트`() {
        runBlocking { SequenceCoroutineView().lock() }
    }
}