package com.example.filepractice.claude.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 이메일 발송 서비스
 *
 * 실제 이메일을 발송하지 않고 로깅만 수행하는 모킹 서비스입니다.
 * 실제 환경에서는 SMTP 서버나 이메일 발송 API를 연동하여 사용합니다.
 */
@Service
class EmailService {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 파일 첨부 이메일 발송
     *
     * @param to 수신자 이메일 주소
     * @param subject 이메일 제목
     * @param body 이메일 본문
     * @param attachment 첨부 파일 데이터
     * @param filename 첨부 파일명
     */
    fun sendEmailWithAttachment(
        to: String,
        subject: String,
        body: String,
        attachment: ByteArray,
        filename: String
    ) {
        logger.info("=== 이메일 발송 시뮬레이션 ===")
        logger.info("수신자: $to")
        logger.info("제목: $subject")
        logger.info("본문: $body")
        logger.info("첨부 파일: $filename (크기: ${attachment.size} bytes)")
        logger.info("============================")

        // 실제 환경에서는 여기에 SMTP 또는 이메일 API를 사용하여 이메일을 발송합니다.
        // 예: JavaMailSender, AWS SES, SendGrid 등
    }

    /**
     * 주문 내역 엑셀 파일을 이메일로 발송
     *
     * @param email 수신자 이메일
     * @param excelData 엑셀 파일 데이터
     */
    fun sendOrderExcelEmail(email: String, excelData: ByteArray) {
        val subject = "주문 내역 엑셀 파일"
        val body = """
            안녕하세요,

            요청하신 주문 내역 엑셀 파일을 첨부하여 보내드립니다.

            감사합니다.
        """.trimIndent()

        sendEmailWithAttachment(
            to = email,
            subject = subject,
            body = body,
            attachment = excelData,
            filename = "order_history.xlsx"
        )
    }

    /**
     * 주문 내역 PDF 파일을 이메일로 발송
     *
     * @param email 수신자 이메일
     * @param pdfData PDF 파일 데이터
     */
    fun sendOrderPdfEmail(email: String, pdfData: ByteArray) {
        val subject = "주문 내역 PDF 파일"
        val body = """
            안녕하세요,

            요청하신 주문 내역 PDF 파일을 첨부하여 보내드립니다.

            감사합니다.
        """.trimIndent()

        sendEmailWithAttachment(
            to = email,
            subject = subject,
            body = body,
            attachment = pdfData,
            filename = "order_history.pdf"
        )
    }
}
