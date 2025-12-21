package com.example.filepractice.controller

import com.example.filepractice.service.ExcelService
import com.example.filepractice.service.ITextPdfService
import com.example.filepractice.service.PdfService
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(FileDownloadController::class)
class FileDownloadControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var excelService: ExcelService

    @MockBean
    private lateinit var pdfService: PdfService

    @MockBean
    private lateinit var iTextPdfService: ITextPdfService

    @Test
    fun `동기 엑셀 다운로드 엔드포인트 테스트`() {
        // given
        val emptyWorkbook = SXSSFWorkbook() // Use SXSSFWorkbook here
        whenever(excelService.createOrderExcel(any(), any())).thenReturn(emptyWorkbook)

        // when & then
        mockMvc.perform(get("/download/excel"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"orders.xlsx\""))
    }

    @Test
    fun `비동기 엑셀 생성 엔드포인트 테스트`() {
        mockMvc.perform(get("/generate/excel-async"))
            .andExpect(status().isAccepted)
            .andExpect(content().string("엑셀 파일 생성 요청을 받았습니다. 잠시 후 처리됩니다."))
    }

    @Test
    fun `동기 PDF 다운로드 엔드포인트 테스트`() {
        // given
        val emptyPdf = ByteArray(0)
        whenever(pdfService.createOrderPdf(any())).thenReturn(emptyPdf)

        // when & then
        mockMvc.perform(get("/download/pdf"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"orders.pdf\""))
    }

    @Test
    fun `비동기 PDF 생성 엔드포인트 테스트`() {
        mockMvc.perform(get("/generate/pdf-async"))
            .andExpect(status().isAccepted)
            .andExpect(content().string("PDF 파일 생성 요청을 받았습니다. 잠시 후 처리됩니다."))
    }

    @Test
    fun `동기 iText PDF 다운로드 엔드포인트 테스트`() {
        // given
        val emptyPdf = ByteArray(0)
        whenever(iTextPdfService.createOrderPdf(any())).thenReturn(emptyPdf)

        // when & then
        mockMvc.perform(get("/download/pdf-itext"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"orders-itext.pdf\""))
    }
}
