package com.example.filepractice.poc.domain.newexcel

import kotlin.random.Random

/**
 * 테스트용 상품 엑셀 데이터 생성기
 */
object ProductExcelDataGenerator {

    fun generate(size: Int): List<ProductExcelData> {
        return (1..size).map { i ->
            ProductExcelData(
                productId = i.toLong(),
                productName = "상품 $i",
                amount = Random.nextInt(1000, 100000),
                qty = Random.nextInt(1, 100),
                content = "상품 $i 에 대한 상세 설명입니다.",
                images = generateImages(i, Random.nextInt(1, 4))
            )
        }
    }

    private fun generateImages(productId: Int, count: Int): List<ProductImage> {
        return (1..count).map { i ->
            ProductImage(
                imageId = (productId * 100 + i).toLong(),
                imageName = "product_${productId}_image_$i.jpg",
                imageSize = Random.nextInt(512, 2048),
                imageComments = generateComments(productId, i, Random.nextInt(0, 5))
            )
        }
    }

    private fun generateComments(productId: Int, imageId: Int, count: Int): List<ProductImageComment> {
        return (1..count).map { i ->
            ProductImageComment(
                imageCommentId = (productId * 1000 + imageId * 10 + i).toLong(),
                comment = "상품 $productId 이미지 $imageId 에 대한 댓글 $i"
            )
        }
    }
}
