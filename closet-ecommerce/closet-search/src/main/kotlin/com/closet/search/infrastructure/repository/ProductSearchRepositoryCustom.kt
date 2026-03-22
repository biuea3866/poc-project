package com.closet.search.infrastructure.repository

import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.domain.ProductDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductSearchRepositoryCustom {
    fun search(keyword: String?, filter: ProductSearchFilter, pageable: Pageable): Page<ProductDocument>
    fun autocomplete(prefix: String, limit: Int): List<String>
}
