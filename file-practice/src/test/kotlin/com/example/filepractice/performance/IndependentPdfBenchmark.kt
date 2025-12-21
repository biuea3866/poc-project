package com.example.filepractice.performance

import com.example.filepractice.claude.domain.Coupon
import com.example.filepractice.claude.domain.Order
import com.example.filepractice.claude.domain.Product
import com.example.filepractice.claude.service.PdfBoxGenerationService
import com.example.filepractice.claude.service.PdfGenerationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.lang.management.ManagementFactory
import java.math.BigDecimal
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * iText vs PDFBox 독립적이고 통계적으로 유의미한 성능 비교 테스트
 *
 * Excel 벤치마크와 동일한 방법론 적용:
 * 1. 각 라이브러리를 별도로 테스트하여 상호 영향 최소화
 * 2. 여러 번 반복 실행 (워밍업 + 측정)
 * 3. JMX를 사용한 정확한 메모리 측정
 * 4. 통계 분석 (평균, 표준편차, 중앙값, 최소/최대)
 * 5. 격리된 환경에서 실행
 */
class IndependentPdfBenchmark {

    @TempDir
    lateinit var tempDir: Path

    companion object {
        private const val WARMUP_ITERATIONS = 3
        private const val MEASUREMENT_ITERATIONS = 5
        private const val GC_WAIT_TIME_MS = 500L
    }

    @Test
    fun `iText 독립 벤치마크 - 1000건`() {
        println("\n" + "=".repeat(100))
        println("iText7 독립 벤치마크 - 1,000건")
        println("=".repeat(100))
        runIndependentBenchmark(1_000, BenchmarkTarget.ITEXT)
    }

    @Test
    fun `PDFBox 독립 벤치마크 - 1000건`() {
        println("\n" + "=".repeat(100))
        println("Apache PDFBox 독립 벤치마크 - 1,000건")
        println("=".repeat(100))
        runIndependentBenchmark(1_000, BenchmarkTarget.PDFBOX)
    }

    @Test
    fun `iText 독립 벤치마크 - 10000건`() {
        println("\n" + "=".repeat(100))
        println("iText7 독립 벤치마크 - 10,000건")
        println("=".repeat(100))
        runIndependentBenchmark(10_000, BenchmarkTarget.ITEXT)
    }

    @Test
    fun `PDFBox 독립 벤치마크 - 10000건`() {
        println("\n" + "=".repeat(100))
        println("Apache PDFBox 독립 벤치마크 - 10,000건")
        println("=".repeat(100))
        runIndependentBenchmark(10_000, BenchmarkTarget.PDFBOX)
    }

    @Test
    fun `iText 독립 벤치마크 - 100000건`() {
        println("\n" + "=".repeat(100))
        println("iText7 독립 벤치마크 - 100,000건")
        println("=".repeat(100))
        runIndependentBenchmark(100_000, BenchmarkTarget.ITEXT)
    }

    @Test
    fun `PDFBox 독립 벤치마크 - 100000건`() {
        println("\n" + "=".repeat(100))
        println("Apache PDFBox 독립 벤치마크 - 100,000건")
        println("=".repeat(100))
        runIndependentBenchmark(100_000, BenchmarkTarget.PDFBOX)
    }

    private fun runIndependentBenchmark(dataCount: Int, target: BenchmarkTarget) {
        println("\n📊 벤치마크 설정:")
        println("  - 워밍업 반복: $WARMUP_ITERATIONS 회")
        println("  - 측정 반복: $MEASUREMENT_ITERATIONS 회")
        println("  - 데이터 크기: ${formatNumber(dataCount)} 건")
        println("  - 대상: ${target.displayName}")

        // 워밍업 단계
        println("\n🔥 워밍업 시작...")
        repeat(WARMUP_ITERATIONS) { iteration ->
            print("  워밍업 ${iteration + 1}/$WARMUP_ITERATIONS... ")
            performGC()
            runSingleBenchmark(dataCount, target, isWarmup = true)
            println("완료")
        }

        // 측정 단계
        println("\n📏 측정 시작...")
        val results = mutableListOf<BenchmarkResult>()
        repeat(MEASUREMENT_ITERATIONS) { iteration ->
            print("  측정 ${iteration + 1}/$MEASUREMENT_ITERATIONS... ")
            performGC()
            val result = runSingleBenchmark(dataCount, target, isWarmup = false)
            results.add(result)
            println("완료 (${result.executionTimeMs}ms, ${formatBytes(result.heapUsedBytes)})")
        }

        // 통계 분석 및 출력
        printStatistics(results, target)
    }

    private fun runSingleBenchmark(
        dataCount: Int,
        target: BenchmarkTarget,
        isWarmup: Boolean
    ): BenchmarkResult {
        val orders = generateOrders(1L, dataCount)
        val memoryBean = ManagementFactory.getMemoryMXBean()

        if (!isWarmup) {
            performGC()
        }

        val beforeHeap = memoryBean.heapMemoryUsage.used
        val beforeNonHeap = memoryBean.nonHeapMemoryUsage.used

        val file = tempDir.resolve("${target.name}_${System.nanoTime()}.pdf").toFile()
        val executionTime = measureTimeMillis {
            FileOutputStream(file).use { outputStream ->
                when (target) {
                    BenchmarkTarget.ITEXT -> {
                        val service = PdfGenerationService()
                        service.generateOrderPdfFromSequence(orders, outputStream)
                    }
                    BenchmarkTarget.PDFBOX -> {
                        val service = PdfBoxGenerationService()
                        service.generateOrderPdfFromSequence(orders, outputStream)
                    }
                }
            }
        }

        Thread.sleep(100)

        val afterHeap = memoryBean.heapMemoryUsage.used
        val afterNonHeap = memoryBean.nonHeapMemoryUsage.used

        val heapUsed = afterHeap - beforeHeap
        val nonHeapUsed = afterNonHeap - beforeNonHeap
        val fileSize = file.length()

        if (isWarmup) {
            file.delete()
        }

        return BenchmarkResult(
            executionTimeMs = executionTime,
            heapUsedBytes = heapUsed,
            nonHeapUsedBytes = nonHeapUsed,
            fileSizeBytes = fileSize,
            peakHeapBytes = memoryBean.heapMemoryUsage.max
        )
    }

    private fun performGC() {
        repeat(3) {
            System.gc()
            System.runFinalization()
        }
        Thread.sleep(GC_WAIT_TIME_MS)
    }

    private fun printStatistics(results: List<BenchmarkResult>, target: BenchmarkTarget) {
        println("\n" + "=".repeat(100))
        println("📊 ${target.displayName} 벤치마크 결과 (${results.size}회 측정)")
        println("=".repeat(100))

        // 실행 시간 통계
        val executionTimes = results.map { it.executionTimeMs }
        println("\n⏱️  실행 시간 (ms):")
        printDetailedStats(executionTimes.map { it.toDouble() })

        // 힙 메모리 사용량 통계
        val heapUsages = results.map { it.heapUsedBytes }
        println("\n💾 힙 메모리 사용량:")
        printDetailedStats(heapUsages.map { it.toDouble() }, isMemory = true)

        // Non-heap 메모리 사용량 통계
        val nonHeapUsages = results.map { it.nonHeapUsedBytes }
        println("\n📦 Non-Heap 메모리 사용량:")
        printDetailedStats(nonHeapUsages.map { it.toDouble() }, isMemory = true)

        // 파일 크기 통계
        val fileSizes = results.map { it.fileSizeBytes }
        println("\n📄 파일 크기:")
        printDetailedStats(fileSizes.map { it.toDouble() }, isMemory = true)

        // 전체 요약
        println("\n" + "=".repeat(100))
        println("✅ 요약")
        println("=".repeat(100))
        println("  평균 실행 시간: ${executionTimes.average().toLong()} ms (${executionTimes.average() / 1000.0} 초)")
        println("  평균 힙 메모리: ${formatBytes(heapUsages.average().toLong())}")
        println("  평균 파일 크기: ${formatBytes(fileSizes.average().toLong())}")
        println("  안정성 (실행시간 변동계수): ${String.format("%.2f", calculateCV(executionTimes.map { it.toDouble() }))}%")
        println("=".repeat(100) + "\n")
    }

    private fun printDetailedStats(values: List<Double>, isMemory: Boolean = false) {
        val sorted = values.sorted()
        val mean = values.average()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }
        val stdDev = calculateStdDev(values)
        val cv = calculateCV(values)
        val min = sorted.first()
        val max = sorted.last()

        if (isMemory) {
            println("  평균(Mean):      ${formatBytes(mean.toLong())}")
            println("  중앙값(Median):   ${formatBytes(median.toLong())}")
            println("  표준편차(StdDev): ${formatBytes(stdDev.toLong())}")
            println("  최소값(Min):      ${formatBytes(min.toLong())}")
            println("  최대값(Max):      ${formatBytes(max.toLong())}")
        } else {
            println("  평균(Mean):      ${String.format("%.2f", mean)}")
            println("  중앙값(Median):   ${String.format("%.2f", median)}")
            println("  표준편차(StdDev): ${String.format("%.2f", stdDev)}")
            println("  최소값(Min):      ${String.format("%.2f", min)}")
            println("  최대값(Max):      ${String.format("%.2f", max)}")
        }
        println("  변동계수(CV):     ${String.format("%.2f", cv)}%")
    }

    private fun calculateStdDev(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    private fun calculateCV(values: List<Double>): Double {
        val mean = values.average()
        val stdDev = calculateStdDev(values)
        return (stdDev / mean) * 100
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 0 -> "-${formatBytes(-bytes)}"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    private fun formatNumber(num: Int): String {
        return String.format("%,d", num)
    }

    private fun generateOrders(userId: Long, count: Int): Sequence<Order> {
        return sequence {
            repeat(count) { index ->
                val orderId = (index + 1).toLong()
                val orderNumber = "ORD-2025-${String.format("%06d", orderId)}"

                val productCount = Random.nextInt(2, 4)
                val products = (1..productCount).map { productIndex ->
                    val productId = orderId * 100 + productIndex
                    val price = BigDecimal(Random.nextInt(10_000, 100_000))

                    Product(
                        id = productId,
                        name = "Product ${productId}",
                        price = price,
                        quantity = Random.nextInt(1, 5),
                        category = listOf("Electronics", "Clothing", "Food", "Home", "Books").random()
                    )
                }

                val totalAmount = products.sumOf { it.price.multiply(it.quantity.toBigDecimal()) }

                val coupon = if (Random.nextBoolean()) {
                    val discountRate = BigDecimal("0.${Random.nextInt(5, 20)}")
                    val discountAmount = totalAmount.multiply(discountRate)
                    Coupon(
                        id = orderId,
                        code = "COUPON-${orderId}",
                        name = "${(discountRate.multiply(BigDecimal(100))).toInt()}% 할인 쿠폰",
                        discountRate = discountRate,
                        discountAmount = discountAmount
                    )
                } else null

                val discountedAmount = coupon?.let { totalAmount.subtract(it.discountAmount) } ?: totalAmount

                val order = Order(
                    id = orderId,
                    orderNumber = orderNumber,
                    userId = userId,
                    products = products,
                    coupon = coupon,
                    totalAmount = totalAmount,
                    discountedAmount = discountedAmount,
                    orderDate = LocalDateTime.now().minusDays(Random.nextLong(1, 30)),
                    status = Order.OrderStatus.entries.random()
                )

                yield(order)
            }
        }
    }

    enum class BenchmarkTarget(val displayName: String) {
        ITEXT("iText7"),
        PDFBOX("Apache PDFBox")
    }

    data class BenchmarkResult(
        val executionTimeMs: Long,
        val heapUsedBytes: Long,
        val nonHeapUsedBytes: Long,
        val fileSizeBytes: Long,
        val peakHeapBytes: Long
    )
}
