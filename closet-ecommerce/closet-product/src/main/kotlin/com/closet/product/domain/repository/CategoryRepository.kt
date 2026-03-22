package com.closet.product.domain.repository

import com.closet.product.domain.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByParentIdIsNullAndDeletedAtIsNullOrderBySortOrder(): List<Category>
    fun findByParentIdAndDeletedAtIsNullOrderBySortOrder(parentId: Long): List<Category>
    fun findByDeletedAtIsNullOrderByDepthAscSortOrderAsc(): List<Category>
}
