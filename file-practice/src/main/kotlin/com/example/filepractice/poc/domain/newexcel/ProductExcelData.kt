package com.example.filepractice.poc.domain.newexcel

class ProductExcelData(
    val productId: Long,
    val productName: String,
    val amount: Int,
    val qty: Int,
    val content: String,
    val images: List<ProductImage>
): ExcelData {
}

class ProductImage(
    val imageId: Long,
    val imageName: String,
    val imageSize: Int,
    val imageComments: List<ProductImageComment>
)

class ProductImageComment(
    val imageCommentId: Long,
    val comment: String
)