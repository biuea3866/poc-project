package com.closet.display.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.display.application.dto.ExhibitionCreateRequest
import com.closet.display.application.dto.ExhibitionProductCreateRequest
import com.closet.display.application.dto.ExhibitionProductResponse
import com.closet.display.application.dto.ExhibitionResponse
import com.closet.display.domain.entity.Exhibition
import com.closet.display.domain.entity.ExhibitionProduct
import com.closet.display.domain.repository.ExhibitionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class ExhibitionService(
    private val exhibitionRepository: ExhibitionRepository,
) {
    @Transactional
    fun create(request: ExhibitionCreateRequest): ExhibitionResponse {
        require(request.endAt.isAfter(request.startAt)) { "종료일시는 시작일시 이후여야 합니다" }
        val exhibition =
            Exhibition(
                title = request.title,
                description = request.description,
                thumbnailUrl = request.thumbnailUrl,
                startAt = request.startAt,
                endAt = request.endAt,
            )
        val saved = exhibitionRepository.save(exhibition)
        logger.info { "기획전 생성 완료: id=${saved.id}, title=${saved.title}" }
        return ExhibitionResponse.from(saved)
    }

    @Transactional
    fun activate(id: Long): ExhibitionResponse {
        val exhibition = findExhibitionById(id)
        exhibition.activate()
        logger.info { "기획전 활성화: id=$id" }
        return ExhibitionResponse.from(exhibition)
    }

    @Transactional
    fun end(id: Long): ExhibitionResponse {
        val exhibition = findExhibitionById(id)
        exhibition.end()
        logger.info { "기획전 종료: id=$id" }
        return ExhibitionResponse.from(exhibition)
    }

    fun findActive(): List<ExhibitionResponse> {
        val now = ZonedDateTime.now()
        return exhibitionRepository.findByDeletedAtIsNullOrderByStartAtDesc()
            .map { ExhibitionResponse.from(it) }
    }

    @Transactional
    fun addProduct(
        exhibitionId: Long,
        request: ExhibitionProductCreateRequest,
    ): ExhibitionProductResponse {
        val exhibition = findExhibitionById(exhibitionId)
        val product =
            ExhibitionProduct(
                productId = request.productId,
                sortOrder = request.sortOrder,
                discountRate = request.discountRate,
            )
        exhibition.addProduct(product)
        exhibitionRepository.flush()
        logger.info { "기획전 상품 추가: exhibitionId=$exhibitionId, productId=${request.productId}" }
        return ExhibitionProductResponse.from(product)
    }

    @Transactional
    fun removeProduct(
        exhibitionId: Long,
        productId: Long,
    ) {
        val exhibition = findExhibitionById(exhibitionId)
        exhibition.removeProduct(productId)
        logger.info { "기획전 상품 제거: exhibitionId=$exhibitionId, productId=$productId" }
    }

    fun getProducts(exhibitionId: Long): List<ExhibitionProductResponse> {
        val exhibition = findExhibitionById(exhibitionId)
        return exhibition.products
            .sortedBy { it.sortOrder }
            .map { ExhibitionProductResponse.from(it) }
    }

    private fun findExhibitionById(id: Long): Exhibition {
        return exhibitionRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "기획전을 찾을 수 없습니다: $id") }
    }
}
