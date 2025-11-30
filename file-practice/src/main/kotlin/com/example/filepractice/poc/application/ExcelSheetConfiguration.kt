package com.example.filepractice.poc.application

import com.example.filepractice.poc.domain.excel.AbstractExcelSheet
import com.example.filepractice.poc.domain.excel.ExcelData
import com.example.filepractice.poc.domain.excel.sheet.CategoryExcelSheet
import com.example.filepractice.poc.domain.excel.sheet.OrderExcelSheet
import com.example.filepractice.poc.domain.excel.sheet.ProductExcelSheet
import com.example.filepractice.poc.domain.excel.sheet.ReviewExcelSheet
import com.example.filepractice.poc.domain.excel.sheet.ShipmentExcelSheet
import com.example.filepractice.poc.domain.excel.sheet.UserExcelSheet
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ExcelSheetConfiguration {

    /**
     * Apache POI 폰트 설정 에러 방지
     * macOS에서 "Fontconfig head is null" 에러 해결
     */
    @PostConstruct
    fun configureHeadlessMode() {
        System.setProperty("java.awt.headless", "true")
    }

    @Bean
    fun excelSheets(): List<AbstractExcelSheet<ExcelData>> {
        return listOf(
            ProductExcelSheet() as AbstractExcelSheet<ExcelData>,
            OrderExcelSheet() as AbstractExcelSheet<ExcelData>,
            UserExcelSheet() as AbstractExcelSheet<ExcelData>,
            CategoryExcelSheet() as AbstractExcelSheet<ExcelData>,
            ReviewExcelSheet() as AbstractExcelSheet<ExcelData>,
            ShipmentExcelSheet() as AbstractExcelSheet<ExcelData>
        )
    }
}
