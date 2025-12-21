package com.example.filepractice.performance

import com.example.filepractice.claude.domain.Coupon
import com.example.filepractice.claude.domain.Order
import com.example.filepractice.claude.domain.Product
import com.example.filepractice.claude.dto.ColumnConfig
import com.example.filepractice.claude.service.EasyExcelGenerationService
import com.example.filepractice.claude.service.ExcelGenerationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * POI vs EasyExcel 성능 비교 테스트
 *
 * 다양한 데이터 크기에서 메모리 사용량과 실행 시간을 비교합니다.
 */
class ExcelPerformanceComparisonTest {

    private val poiService = ExcelGenerationService()
    private val easyExcelService = EasyExcelGenerationService()
    private val columnConfig = ColumnConfig.default()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `성능 비교 - 1000건`() {
        println("\n=== 1,000건 데이터 성능 비교 ===")
        comparePerformance(1_000)
    }

    @Test
    fun `성능 비교 - 10000건`() {
        println("\n=== 10,000건 데이터 성능 비교 ===")
        comparePerformance(10_000)
    }

    @Test
    fun `성능 비교 - 100000건`() {
        println("\n=== 100,000건 데이터 성능 비교 ===")
        comparePerformance(100_000)
    }

    @Test
    fun `성능 비교 - 500000건`() {
        println("\n=== 500,000건 데이터 성능 비교 ===")
        comparePerformance(500_000)
    }

    private fun comparePerformance(dataCount: Int) {
        // 데이터 준비
        println("데이터 생성 중...")
        val orders = generateOrders(1L, dataCount)

        // 메모리 초기화
        System.gc()
        Thread.sleep(1000)

        // POI 성능 측정
        val poiResult = measureExcelGeneration("POI (SXSSFWorkbook)", orders) { outputStream ->
            poiService.generateOrderExcelFromSequence(orders, columnConfig, outputStream)
        }

        // 메모리 초기화
        System.gc()
        Thread.sleep(1000)

        // EasyExcel 성능 측정
        val easyExcelResult = measureExcelGeneration("EasyExcel", orders) { outputStream ->
            easyExcelService.generateOrderExcelFromSequence(orders, columnConfig, outputStream)
        }

        // 결과 출력
        printComparisonResult(poiResult, easyExcelResult)
    }

    private fun measureExcelGeneration(
        name: String,
        orders: Sequence<Order>,
        generator: (FileOutputStream) -> Unit
    ): PerformanceResult {
        println("\n[$name] 측정 시작...")

        val runtime = Runtime.getRuntime()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

        val file = tempDir.resolve("${name.replace(" ", "_")}_${System.currentTimeMillis()}.xlsx").toFile()
        val executionTime = measureTimeMillis {
            FileOutputStream(file).use { outputStream ->
                generator(outputStream)
            }
        }

        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = afterMemory - beforeMemory
        val fileSize = file.length()

        return PerformanceResult(
            name = name,
            executionTimeMs = executionTime,
            memoryUsedBytes = memoryUsed,
            fileSizeBytes = fileSize,
            file = file
        )
    }

    private fun printComparisonResult(poiResult: PerformanceResult, easyExcelResult: PerformanceResult) {
        println("\n" + "=".repeat(80))
        println("성능 비교 결과")
        println("=".repeat(80))

        println("\n[POI (SXSSFWorkbook)]")
        println("  실행 시간: ${poiResult.executionTimeMs} ms (${poiResult.executionTimeMs / 1000.0} 초)")
        println("  메모리 사용량: ${formatBytes(poiResult.memoryUsedBytes)}")
        println("  파일 크기: ${formatBytes(poiResult.fileSizeBytes)}")

        println("\n[EasyExcel]")
        println("  실행 시간: ${easyExcelResult.executionTimeMs} ms (${easyExcelResult.executionTimeMs / 1000.0} 초)")
        println("  메모리 사용량: ${formatBytes(easyExcelResult.memoryUsedBytes)}")
        println("  파일 크기: ${formatBytes(easyExcelResult.fileSizeBytes)}")

        println("\n[성능 차이]")
        val timeDiff = poiResult.executionTimeMs - easyExcelResult.executionTimeMs
        val timeImprovement = (timeDiff.toDouble() / poiResult.executionTimeMs * 100)
        println("  실행 시간 차이: ${timeDiff} ms (${String.format("%.2f", timeImprovement)}%)")

        val memoryDiff = poiResult.memoryUsedBytes - easyExcelResult.memoryUsedBytes
        val memoryImprovement = if (poiResult.memoryUsedBytes > 0) {
            (memoryDiff.toDouble() / poiResult.memoryUsedBytes * 100)
        } else 0.0
        println("  메모리 사용량 차이: ${formatBytes(memoryDiff)} (${String.format("%.2f", memoryImprovement)}%)")

        val fileSizeDiff = poiResult.fileSizeBytes - easyExcelResult.fileSizeBytes
        val fileSizeImprovement = (fileSizeDiff.toDouble() / poiResult.fileSizeBytes * 100)
        println("  파일 크기 차이: ${formatBytes(fileSizeDiff)} (${String.format("%.2f", fileSizeImprovement)}%)")

        println("\n" + "=".repeat(80))

        // 승자 결정
        val winner = when {
            easyExcelResult.executionTimeMs < poiResult.executionTimeMs -> "EasyExcel"
            else -> "POI"
        }
        println("🏆 실행 시간 기준 승자: $winner")
        println("=".repeat(80) + "\n")
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    /**
     * 대량의 더미 주문 데이터 생성
     */
    private fun generateOrders(userId: Long, count: Int): Sequence<Order> {
        return sequence {
            repeat(count) { index ->
                val orderId = (index + 1).toLong()
                val orderNumber = "ORD-2025-${String.format("%06d", orderId)}"

                // 상품 생성 (2-3개)
                val productCount = Random.nextInt(2, 4)
                val products = (1..productCount).map { productIndex ->
                    val productId = orderId * 100 + productIndex
                    val price = BigDecimal(Random.nextInt(10_000, 100_000))

                    Product(
                        id = productId,
                        name = "상품 ${productId}",
                        price = price,
                        quantity = Random.nextInt(1, 5),
                        category = listOf("전자기기", "의류", "식품", "생활용품", "도서").random()
                    )
                }

                // 총 금액 계산
                val totalAmount = products.sumOf { it.price.multiply(it.quantity.toBigDecimal()) }

                // 쿠폰 생성 (50% 확률)
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

    data class PerformanceResult(
        val name: String,
        val executionTimeMs: Long,
        val memoryUsedBytes: Long,
        val fileSizeBytes: Long,
        val file: File
    )
}
