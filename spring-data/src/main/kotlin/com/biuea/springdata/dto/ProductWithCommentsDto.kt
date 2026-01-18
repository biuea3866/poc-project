package com.biuea.springdata.dto

import java.math.BigDecimal
import java.time.LocalDate

data class ProductWithCommentsDto(
    val productId: Long,
    val productName: String,
    val productPrice: BigDecimal,
    val productCategory: String,
    val productCreatedDate: LocalDate,
    val comments: List<CommentDto>
)

data class CommentDto(
    val commentId: Long,
    val userName: String,
    val content: String,
    val rating: Int,
    val createdDate: LocalDate
)
