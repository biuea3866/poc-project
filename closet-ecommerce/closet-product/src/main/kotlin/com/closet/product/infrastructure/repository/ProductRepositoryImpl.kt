package com.closet.product.infrastructure.repository

import com.closet.product.domain.entity.Product
import com.closet.product.domain.entity.QProduct.product
import com.closet.product.domain.enums.ProductStatus
import com.closet.product.domain.repository.ProductRepositoryCustom
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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
        status?.let { builder.and(product.status.eq(it)) }

        val results = queryFactory
            .selectFrom(product)
            .where(builder)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(product.createdAt.desc())
            .fetch()

        val total = queryFactory
            .select(product.count())
            .from(product)
            .where(builder)
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }
}
