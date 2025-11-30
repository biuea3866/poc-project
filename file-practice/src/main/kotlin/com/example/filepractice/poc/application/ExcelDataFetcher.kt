package com.example.filepractice.poc.application

import com.example.filepractice.poc.domain.excel.CategoryData
import com.example.filepractice.poc.domain.excel.ExcelData
import com.example.filepractice.poc.domain.excel.OrderData
import com.example.filepractice.poc.domain.excel.ProductData
import com.example.filepractice.poc.domain.excel.ReviewData
import com.example.filepractice.poc.domain.excel.SheetType
import com.example.filepractice.poc.domain.excel.ShipmentData
import com.example.filepractice.poc.domain.excel.UserData
import org.springframework.stereotype.Component

@Component
class ExcelDataFetcher {
    fun fetchMapBy(
        downloadableData: DownloadableData,
        sheetTypes: List<SheetType>
    ): Map<SheetType, Sequence<ExcelData>> {
        val excelDataMap = mutableMapOf<SheetType, Sequence<ExcelData>>()

        sheetTypes.forEach { type ->
            when (type) {
                SheetType.PRODUCT -> excelDataMap[type] = getProductData(downloadableData)
                SheetType.ORDER -> excelDataMap[type] = getOrderData(downloadableData)
                SheetType.USER -> excelDataMap[type] = getUserData(downloadableData)
                SheetType.CATEGORY -> excelDataMap[type] = getCategoryData(downloadableData)
                SheetType.REVIEW -> excelDataMap[type] = getReviewData(downloadableData)
                SheetType.SHIPMENT -> excelDataMap[type] = getShipmentData(downloadableData)
            }
        }

        return excelDataMap
    }

    /**
     * 제품 데이터 조회
     */
    private fun getProductData(downloadableData: DownloadableData): Sequence<ExcelData> {
        val productIds = downloadableData.productIds

        return sequence {
            productIds.forEach { productId ->
                yield(ProductData(productId = productId))
            }
        }
    }

    /**
     * 주문 데이터 조회
     */
    private fun getOrderData(downloadableData: DownloadableData): Sequence<ExcelData> {
        val orderIds = downloadableData.orderIds

        return sequence {
            orderIds.forEach { orderId ->
                yield(OrderData(orderId = orderId))
            }
        }
    }

    /**
     * 사용자 데이터 조회
     */
    private fun getUserData(downloadableData: DownloadableData): Sequence<ExcelData> {
        val userIds = downloadableData.userIds

        return sequence {
            userIds.forEach { userId ->
                yield(UserData(userId = userId))
            }
        }
    }

    /**
     * 카테고리 데이터 조회
     */
    private fun getCategoryData(downloadableData: DownloadableData): Sequence<ExcelData> {
        val categoryIds = downloadableData.categoryIds

        return sequence {
            categoryIds.forEach { categoryId ->
                yield(CategoryData(categoryId = categoryId))
            }
        }
    }

    /**
     * 리뷰 데이터 조회
     */
    private fun getReviewData(downloadableData: DownloadableData): Sequence<ExcelData> {
        val reviewIds = downloadableData.reviewIds

        return sequence {
            reviewIds.forEach { reviewId ->
                yield(ReviewData(reviewId = reviewId))
            }
        }
    }

    /**
     * 배송 데이터 조회
     */
    private fun getShipmentData(downloadableData: DownloadableData): Sequence<ExcelData> {
        val shipmentIds = downloadableData.shipmentIds

        return sequence {
            shipmentIds.forEach { shipmentId ->
                yield(ShipmentData(shipmentId = shipmentId))
            }
        }
    }
}

data class DownloadableData(
    val productIds: List<Int> = emptyList(),
    val orderIds: List<Int> = emptyList(),
    val userIds: List<Int> = emptyList(),
    val categoryIds: List<Int> = emptyList(),
    val reviewIds: List<Int> = emptyList(),
    val shipmentIds: List<Int> = emptyList()
)
