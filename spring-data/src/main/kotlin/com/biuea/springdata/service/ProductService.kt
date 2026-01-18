package com.biuea.springdata.service

import com.biuea.springdata.domain.Comment
import com.biuea.springdata.domain.Product
import com.biuea.springdata.domain.ProductPartitioned
import com.biuea.springdata.dto.CommentDto
import com.biuea.springdata.dto.ProductDto
import com.biuea.springdata.dto.ProductWithCommentsDto
import com.biuea.springdata.repository.CommentRepository
import com.biuea.springdata.repository.ProductPartitionedRepository
import com.biuea.springdata.repository.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
    private val productPartitionedRepository: ProductPartitionedRepository,
    private val commentRepository: CommentRepository
) {

    /**
     * API 1: 파티셔닝 미적용 상품 조회
     */
    fun getProducts(page: Int, size: Int, startDate: LocalDate?, endDate: LocalDate?): Page<ProductDto> {
        val pageable = PageRequest.of(page, size)

        val products = if (startDate != null && endDate != null) {
            productRepository.findByCreatedDateBetween(startDate, endDate, pageable)
        } else {
            productRepository.findAll(pageable)
        }

        return products.map { ProductDto.from(it) }
    }

    /**
     * API 2: 파티셔닝 적용 상품 조회
     */
    fun getProductsPartitioned(page: Int, size: Int, startDate: LocalDate?, endDate: LocalDate?): Page<ProductDto> {
        val pageable = PageRequest.of(page, size)

        val products = if (startDate != null && endDate != null) {
            productPartitionedRepository.findByCreatedDateBetween(startDate, endDate, pageable)
        } else {
            productPartitionedRepository.findAll(pageable)
        }

        return products.map { ProductDto.from(it) }
    }

    /**
     * API 3: JOIN 파티션 키 미포함
     * - 상품의 created_date와 댓글의 created_date가 다를 수 있음
     * - 파티션 프루닝이 적용되지 않음 (전체 파티션 스캔)
     */
    fun getProductsWithCommentsWithoutPartitionKey(
        page: Int,
        size: Int,
        productStartDate: LocalDate?,
        productEndDate: LocalDate?
    ): List<ProductWithCommentsDto> {
        val pageable = PageRequest.of(page, size)

        // 상품 조회 (파티션 키 사용)
        val products = if (productStartDate != null && productEndDate != null) {
            productPartitionedRepository.findByCreatedDateBetween(productStartDate, productEndDate, pageable)
        } else {
            productPartitionedRepository.findAll(pageable)
        }

        val productIds = products.content.map { it.id.productId }

        // 댓글 조회 (파티션 키 미사용 - productId만으로 조회)
        // 이 경우 모든 파티션을 스캔해야 함
        val comments = if (productIds.isNotEmpty()) {
            commentRepository.findByProductIdIn(productIds)
        } else {
            emptyList()
        }

        // 그룹핑
        val commentsByProductId = comments.groupBy { it.productId }

        return products.content.map { product ->
            ProductWithCommentsDto(
                productId = product.id.productId,
                productName = product.name,
                productPrice = product.price,
                productCategory = product.category,
                productCreatedDate = product.id.createdDate,
                comments = (commentsByProductId[product.id.productId] ?: emptyList()).map { comment ->
                    CommentDto(
                        commentId = comment.id.commentId,
                        userName = comment.userName,
                        content = comment.content,
                        rating = comment.rating,
                        createdDate = comment.id.createdDate
                    )
                }
            )
        }
    }

    /**
     * API 4: JOIN 파티션 키 포함
     * - 상품과 댓글의 created_date 범위를 함께 지정
     * - 파티션 프루닝이 적용됨 (특정 파티션만 스캔)
     */
    fun getProductsWithCommentsWithPartitionKey(
        page: Int,
        size: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ProductWithCommentsDto> {
        val pageable = PageRequest.of(page, size)

        // 상품 조회 (파티션 키 사용)
        val products = productPartitionedRepository.findByCreatedDateBetween(startDate, endDate, pageable)

        val productIds = products.content.map { it.id.productId }

        // 댓글 조회 (파티션 키 포함 - productId와 created_date 범위로 조회)
        // 이 경우 특정 파티션만 스캔
        val comments = if (productIds.isNotEmpty()) {
            commentRepository.findByProductIdAndDateRange(productIds, startDate, endDate)
        } else {
            emptyList()
        }

        // 그룹핑
        val commentsByProductId = comments.groupBy { it.productId }

        return products.content.map { product ->
            ProductWithCommentsDto(
                productId = product.id.productId,
                productName = product.name,
                productPrice = product.price,
                productCategory = product.category,
                productCreatedDate = product.id.createdDate,
                comments = (commentsByProductId[product.id.productId] ?: emptyList()).map { comment ->
                    CommentDto(
                        commentId = comment.id.commentId,
                        userName = comment.userName,
                        content = comment.content,
                        rating = comment.rating,
                        createdDate = comment.id.createdDate
                    )
                }
            )
        }
    }
}
