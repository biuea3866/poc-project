package com.biuea.springdata.dto

import com.biuea.springdata.domain.Product
import com.biuea.springdata.domain.ProductPartitioned
import java.math.BigDecimal
import java.time.LocalDate

data class ProductDto(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val category: String,
    val description: String?,
    val stockQuantity: Int,
    val createdDate: LocalDate
) {
    companion object {
        fun from(product: Product): ProductDto {
            return ProductDto(
                id = product.id!!,
                name = product.name,
                price = product.price,
                category = product.category,
                description = product.description,
                stockQuantity = product.stockQuantity,
                createdDate = product.createdDate
            )
        }

        fun from(product: ProductPartitioned): ProductDto {
            return ProductDto(
                id = product.id.productId,
                name = product.name,
                price = product.price,
                category = product.category,
                description = product.description,
                stockQuantity = product.stockQuantity,
                createdDate = product.id.createdDate
            )
        }
    }
}
