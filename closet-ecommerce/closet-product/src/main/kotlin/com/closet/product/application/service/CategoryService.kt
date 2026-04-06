package com.closet.product.application.service

import com.closet.product.application.dto.CategoryCreateRequest
import com.closet.product.application.dto.CategoryResponse
import com.closet.product.domain.entity.Category
import com.closet.product.domain.repository.CategoryRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {
    fun findAll(): List<CategoryResponse> {
        val allCategories = categoryRepository.findByDeletedAtIsNullOrderByDepthAscSortOrderAsc()
        return buildTree(allCategories)
    }

    @Transactional
    fun create(request: CategoryCreateRequest): CategoryResponse {
        val category =
            Category(
                parentId = request.parentId,
                name = request.name,
                depth = request.depth,
                sortOrder = request.sortOrder,
            )
        val saved = categoryRepository.save(category)
        logger.info { "카테고리 생성 완료: id=${saved.id}, name=${saved.name}" }
        return CategoryResponse.from(saved)
    }

    private fun buildTree(categories: List<Category>): List<CategoryResponse> {
        val categoryMap = mutableMapOf<Long, MutableList<Category>>()
        val roots = mutableListOf<Category>()

        for (category in categories) {
            if (category.parentId == null) {
                roots.add(category)
            } else {
                categoryMap.getOrPut(category.parentId) { mutableListOf() }.add(category)
            }
        }

        return roots.map { buildCategoryResponse(it, categoryMap) }
    }

    private fun buildCategoryResponse(
        category: Category,
        childrenMap: Map<Long, List<Category>>,
    ): CategoryResponse {
        val children = childrenMap[category.id]?.map { buildCategoryResponse(it, childrenMap) } ?: emptyList()
        return CategoryResponse.from(category, children)
    }
}
