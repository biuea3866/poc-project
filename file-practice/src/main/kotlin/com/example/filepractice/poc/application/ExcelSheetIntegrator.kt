package com.example.filepractice.poc.application

import com.example.filepractice.poc.domain.excel.AbstractExcelSheet
import com.example.filepractice.poc.domain.excel.ExcelData
import com.example.filepractice.poc.domain.excel.SheetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ExcelSheetIntegrator(
    private val sheets: List<AbstractExcelSheet<ExcelData>>,
) {
    // SXSSFWorkbook은 thread-safe하지 않으므로 sheet 생성 시 동기화 필요
    private val workbookMutex = Mutex()
    /**
     * 동기 방식 엑셀 시트 작성
     * - 순차적으로 시트를 생성하고 데이터를 작성
     * - POI는 thread-safe하지 않으므로 순차 처리
     */
    fun composeSync(
        workbook: SXSSFWorkbook,
        excelDataMap: Map<SheetType, Sequence<ExcelData>>,
        sheetTypes: List<SheetType>,
    ) {
        sheets.forEach { sheetData ->
            if (sheetData.sheetType in sheetTypes) {
                val sheet = sheetData.createSheet(workbook)

                // Chunked 방식으로 메모리 사용 최적화
                val dataSequence = excelDataMap[sheetData.sheetType] ?: emptySequence()
                var currentRow = 1
                dataSequence.chunked(CHUNK_SIZE).forEach { chunk ->
                    currentRow = sheetData.writeData(sheet, chunk, currentRow)
                }
            }
        }
    }

    /**
     * 비동기 방식 엑셀 시트 작성 (Mutex 개선 버전)
     * - Mutex를 사용하여 sheet 생성만 동기화
     * - 각 시트의 데이터 쓰기는 병렬로 처리
     * - 성능과 thread-safety 모두 확보
     */
    suspend fun composeAsync(
        workbook: SXSSFWorkbook,
        excelDataMap: Map<SheetType, Sequence<ExcelData>>,
        sheetTypes: List<SheetType>,
    ) = withContext(Dispatchers.IO) {
        // 1단계: 시트별 데이터를 병렬로 List로 변환 (메모리에 로드)
        val sheetDataMap = ConcurrentHashMap<SheetType, List<List<ExcelData>>>()

        sheets
            .filter { it.sheetType in sheetTypes }
            .map { sheetData ->
                async(Dispatchers.Default) {
                    val dataSequence = excelDataMap[sheetData.sheetType] ?: emptySequence()
                    val chunkedData = dataSequence.chunked(CHUNK_SIZE).toList()
                    sheetDataMap[sheetData.sheetType] = chunkedData
                }
            }
            .awaitAll()

        // 2단계: Sheet 생성 + 데이터 쓰기를 병렬로 수행
        // Mutex로 sheet 생성만 동기화, 쓰기는 각 시트별로 독립적으로 병렬 처리
        sheets
            .filter { it.sheetType in sheetTypes }
            .map { sheetData ->
                async(Dispatchers.IO) {
                    // Sheet 생성: Mutex로 동기화 (SXSSFWorkbook.createSheet는 thread-safe하지 않음)
                    val sheet = workbookMutex.withLock {
                        sheetData.createSheet(workbook)
                    }

                    // 데이터 쓰기: 각 시트는 독립적이므로 병렬로 처리 가능
                    val chunkedData = sheetDataMap[sheetData.sheetType] ?: emptyList()
                    var currentRow = 1
                    chunkedData.forEach { chunk ->
                        currentRow = sheetData.writeData(sheet, chunk, currentRow)
                    }
                }
            }
            .awaitAll()
    }

    companion object {
        // 청크 크기를 100으로 증가 (메모리와 성능의 균형)
        const val CHUNK_SIZE = 100
    }
}
