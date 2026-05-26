package com.biuea.springai.domain

import org.springframework.stereotype.Repository

/**
 * 의류 상품. PoC 라 영속화는 인메모리.
 */
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val color: String,
    val material: String,
    val fit: String,
    val sizes: List<String>,
    val priceKrw: Int,
    val season: String,
    val description: String,
    val stock: Map<String, Int>,
) {
    fun totalStock(): Int = stock.values.sum()

    fun stockOf(size: String): Int = stock[size] ?: 0
}

@Repository
class ProductRepository {
    private val products = mutableListOf<Product>()

    fun saveAll(items: List<Product>) {
        products.clear()
        products.addAll(items)
    }

    fun findAll(): List<Product> = products.toList()

    fun findById(id: String): Product? = products.find { it.id.equals(id, ignoreCase = true) }

    fun search(
        keyword: String?,
        category: String?,
        color: String?,
        maxPrice: Int?,
    ): List<Product> = products.filter { product ->
        val keywordMatch = keyword == null ||
            product.name.contains(keyword, ignoreCase = true) ||
            product.description.contains(keyword, ignoreCase = true) ||
            product.category.contains(keyword, ignoreCase = true)
        val categoryMatch = category == null || product.category.contains(category, ignoreCase = true)
        val colorMatch = color == null || product.color.contains(color, ignoreCase = true)
        val priceMatch = maxPrice == null || product.priceKrw <= maxPrice
        keywordMatch && categoryMatch && colorMatch && priceMatch
    }

    fun listCategories(): List<String> = products.map { it.category }.distinct().sorted()

    fun listColors(): List<String> = products.map { it.color }.distinct().sorted()

    /** 단일 상품 갱신 (id 일치). 존재하지 않으면 NoSuchElementException. */
    fun save(product: Product): Product {
        val idx = products.indexOfFirst { it.id == product.id }
        if (idx < 0) throw NoSuchElementException("상품을 찾을 수 없습니다: ${product.id}")
        products[idx] = product
        return product
    }
}
