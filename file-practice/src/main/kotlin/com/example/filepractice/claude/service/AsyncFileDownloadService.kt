package com.example.filepractice.claude.service

import com.example.filepractice.claude.dto.ColumnConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

/**
 * 비동기 파일 다운로드 서비스
 *
 * 파일 생성 및 이메일 발송을 비동기로 처리합니다.
 * 실제 환경에서는 메시지 큐(RabbitMQ, Kafka 등)를 사용하는 것이 좋습니다.
 *
 * 참고: 비동기 이메일 발송의 경우 ByteArray가 필요하므로
 * ByteArrayOutputStream을 사용하지만, 메모리 사용량을 줄이기 위해
 * 생성 후 즉시 이메일 발송하고 GC가 수거하도록 합니다.
 */
@Service
class AsyncFileDownloadService(
    private val getOrderService: GetOrderService,
    private val excelGenerationService: ExcelGenerationService,
    private val pdfGenerationService: PdfGenerationService,
    private val emailService: EmailService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * 엑셀 파일을 비동기로 생성하고 이메일로 발송
     *
     * @param userId 사용자 ID
     * @param email 수신자 이메일
     * @param columnConfig 컬럼 설정
     */
    fun generateAndSendExcelAsync(
        userId: Long,
        email: String,
        columnConfig: ColumnConfig
    ) {
        coroutineScope.launch {
            try {
                logger.info("비동기 엑셀 생성 시작: userId=$userId, email=$email")

                // ByteArrayOutputStream 사용 (이메일 첨부를 위해 필요)
                val outputStream = ByteArrayOutputStream()

                // 주문 데이터를 Sequence로 스트리밍 조회하여 엑셀 생성
                val ordersSequence = getOrderService.getOrdersByUserIdAsSequence(userId)
                excelGenerationService.generateOrderExcelFromSequence(
                    ordersSequence,
                    columnConfig,
                    outputStream
                )

                // ByteArray 추출 및 이메일 발송
                val excelData = outputStream.toByteArray()
                emailService.sendOrderExcelEmail(email, excelData)

                logger.info("비동기 엑셀 생성 및 발송 완료: userId=$userId, email=$email, size=${excelData.size} bytes")

                // 명시적으로 null 할당하여 GC 힌트 제공 (선택적)
                // excelData는 메서드 종료 후 자동으로 GC 대상이 됨

            } catch (e: Exception) {
                logger.error("비동기 엑셀 생성 실패: userId=$userId, email=$email", e)
            }
        }
    }

    /**
     * PDF 파일을 비동기로 생성하고 이메일로 발송
     *
     * @param userId 사용자 ID
     * @param email 수신자 이메일
     */
    fun generateAndSendPdfAsync(
        userId: Long,
        email: String
    ) {
        coroutineScope.launch {
            try {
                logger.info("비동기 PDF 생성 시작: userId=$userId, email=$email")

                // ByteArrayOutputStream 사용 (이메일 첨부를 위해 필요)
                val outputStream = ByteArrayOutputStream()

                // 주문 데이터를 Sequence로 스트리밍 조회하여 PDF 생성
                val ordersSequence = getOrderService.getOrdersByUserIdAsSequence(userId)
                pdfGenerationService.generateOrderPdfFromSequence(
                    ordersSequence,
                    outputStream
                )

                // ByteArray 추출 및 이메일 발송
                val pdfData = outputStream.toByteArray()
                emailService.sendOrderPdfEmail(email, pdfData)

                logger.info("비동기 PDF 생성 및 발송 완료: userId=$userId, email=$email, size=${pdfData.size} bytes")

            } catch (e: Exception) {
                logger.error("비동기 PDF 생성 실패: userId=$userId, email=$email", e)
            }
        }
    }
}
