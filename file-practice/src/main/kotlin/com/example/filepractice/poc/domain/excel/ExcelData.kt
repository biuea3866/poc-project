package com.example.filepractice.poc.domain.excel

interface ExcelData

data class ProductData(val productId: Int) : ExcelData

data class OrderData(val orderId: Int) : ExcelData

data class UserData(val userId: Int) : ExcelData

data class CategoryData(val categoryId: Int) : ExcelData

data class ReviewData(val reviewId: Int) : ExcelData

data class ShipmentData(val shipmentId: Int) : ExcelData
