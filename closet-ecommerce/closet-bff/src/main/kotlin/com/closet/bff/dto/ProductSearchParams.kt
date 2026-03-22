package com.closet.bff.dto

data class ProductSearchParams(
    val categoryId: Long? = null,
    val brandId: Long? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sort: String = "newest",
)
