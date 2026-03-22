package com.closet.product.infrastructure.repository

import com.closet.product.domain.entity.Product
import com.closet.product.domain.entity.QProduct.product
import com.closet.product.domain.enums.ProductStatus
import com.closet.product.domain.repository.ProductRepositoryCustom
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class ProductRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductRepositoryCustom {

    override fun findByFilter(
        categoryId: Long?,
        brandId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        status: ProductStatus?,
        pageable: Pageable
    ): Page<Product> {
        val builder = BooleanBuilder()
        builder.and(product.deletedAt.isNull)

        categoryId?.let { builder.and(product.categoryId.eq(it)) }
        brandId?.let { builder.and(product.brandId.eq(it)) }
        minPrice?.let { builder.and(product.salePrice.amount.goe(it)) }
        maxPrice?.let { builder.and(product.salePrice.amount.loe(it)) }
        // status 파라미터가 없으면 ACTIVE 상품만 기본 조회 (DRAFT 노출 방지)
        builder.and(product.status.eq(status ?: ProductStatus.ACTIVE))

        val orderSpecifiers = getOrderSpecifiers(pageable.sort)

        val results = queryFactory
            .selectFrom(product)
            .where(builder)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(*orderSpecifiers.toTypedArray())
            .fetch()

        val total = queryFactory
            .select(product.count())
            .from(product)
            .where(builder)
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }

    private fun getOrderSpecifiers(sort: Sort): List<OrderSpecifier<*>> {
        val orderSpecifiers = mutableListOf<OrderSpecifier<*>>()

        sort.forEach { order ->
            val isAsc = order.isAscending
            when (order.property) {
                "salePrice" -> orderSpecifiers.add(
                    if (isAsc) product.salePrice.amount.asc() else product.salePrice.amount.desc()
                )
                "createdAt" -> orderSpecifiers.add(
                    if (isAsc) product.createdAt.asc() else product.createdAt.desc()
                )
                "name" -> orderSpecifiers.add(
                    if (isAsc) product.name.asc() else product.name.desc()
                )
                "id" -> orderSpecifiers.add(
                    if (isAsc) product.id.asc() else product.id.desc()
                )
            }
        }

        // 기본 정렬: 최신순
        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(product.id.desc())
        }

        return orderSpecifiers
    }
}
