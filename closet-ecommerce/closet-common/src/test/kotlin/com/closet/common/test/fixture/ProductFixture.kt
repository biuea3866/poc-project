package com.closet.common.test.fixture

import java.math.BigDecimal
import java.time.ZonedDateTime

/**
 * Product 도메인 테스트 Fixture.
 *
 * 테스트에서 Product 관련 객체를 간편하게 생성한다.
 * 엔티티가 아직 구현되지 않은 시점에서 Map 기반으로 데이터를 표현하며,
 * 엔티티 구현 후 실제 타입으로 교체한다.
 */
object ProductFixture {
    fun createProduct(
        name: String = "오버핏 코튼 티셔츠",
        brandName: String = "CLOSET",
        categoryCode: String = "TOP_001",
        price: BigDecimal = BigDecimal("29900"),
        salePrice: BigDecimal = BigDecimal("24900"),
        description: String = "편안한 오버핏 코튼 티셔츠입니다.",
        status: String = "DRAFT",
        season: String = "SS",
        gender: String = "UNISEX",
        material: String = "면 100%",
        fit: String = "OVERFIT",
    ): Map<String, Any?> =
        mapOf(
            "name" to name,
            "brandName" to brandName,
            "categoryCode" to categoryCode,
            "price" to price,
            "salePrice" to salePrice,
            "description" to description,
            "status" to status,
            "season" to season,
            "gender" to gender,
            "material" to material,
            "fit" to fit,
            "createdAt" to ZonedDateTime.now(),
        )

    fun createProductOption(
        productId: Long = 1L,
        size: String = "L",
        color: String = "BLACK",
        stockQuantity: Int = 100,
        additionalPrice: BigDecimal = BigDecimal.ZERO,
        skuCode: String = "CLO-TOP001-BK-L",
    ): Map<String, Any?> =
        mapOf(
            "productId" to productId,
            "size" to size,
            "color" to color,
            "stockQuantity" to stockQuantity,
            "additionalPrice" to additionalPrice,
            "skuCode" to skuCode,
            "createdAt" to ZonedDateTime.now(),
        )

    fun createProductWithOptions(
        name: String = "슬림핏 데님 팬츠",
        brandName: String = "CLOSET",
        sizes: List<String> = listOf("S", "M", "L", "XL"),
        colors: List<String> = listOf("INDIGO", "BLACK"),
        basePrice: BigDecimal = BigDecimal("59900"),
        stockPerOption: Int = 50,
    ): Map<String, Any?> {
        val product =
            createProduct(
                name = name,
                brandName = brandName,
                price = basePrice,
                salePrice = basePrice,
                categoryCode = "BOTTOM_001",
            )
        val options =
            sizes.flatMapIndexed { sIdx, size ->
                colors.mapIndexed { cIdx, color ->
                    createProductOption(
                        size = size,
                        color = color,
                        stockQuantity = stockPerOption,
                        skuCode = "CLO-BTM001-${color.take(2)}-$size",
                    )
                }
            }
        return product + ("options" to options)
    }
}
