package com.closet.content.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.content.application.dto.CoordinationCreateRequest
import com.closet.content.application.dto.CoordinationListResponse
import com.closet.content.application.dto.CoordinationProductAddRequest
import com.closet.content.application.dto.CoordinationProductResponse
import com.closet.content.application.dto.CoordinationResponse
import com.closet.content.domain.entity.Coordination
import com.closet.content.domain.enums.CoordinationStatus
import com.closet.content.domain.enums.CoordinationStyle
import com.closet.content.domain.repository.CoordinationRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class CoordinationService(
    private val coordinationRepository: CoordinationRepository
) {

    @Transactional
    fun create(request: CoordinationCreateRequest): CoordinationResponse {
        val coordination = Coordination(
            title = request.title,
            description = request.description,
            thumbnailUrl = request.thumbnailUrl,
            style = request.style,
            season = request.season,
            gender = request.gender
        )
        val saved = coordinationRepository.save(coordination)
        logger.info { "코디 생성 완료: id=${saved.id}, title=${saved.title}" }
        return CoordinationResponse.from(saved)
    }

    @Transactional
    fun activate(id: Long): CoordinationResponse {
        val coordination = findCoordinationById(id)
        coordination.activate()
        logger.info { "코디 활성화 완료: id=$id" }
        return CoordinationResponse.from(coordination)
    }

    @Transactional
    fun addProduct(id: Long, request: CoordinationProductAddRequest): CoordinationProductResponse {
        val coordination = findCoordinationById(id)
        coordination.addProduct(
            productId = request.productId,
            sortOrder = request.sortOrder,
            description = request.description
        )
        coordinationRepository.flush()
        logger.info { "코디 상품 추가: coordinationId=$id, productId=${request.productId}" }
        return CoordinationProductResponse.from(coordination.products.last())
    }

    fun findById(id: Long): CoordinationResponse {
        val coordination = findCoordinationById(id)
        return CoordinationResponse.from(coordination)
    }

    fun findAll(pageable: Pageable): Page<CoordinationListResponse> {
        return coordinationRepository.findByDeletedAtIsNull(pageable)
            .map { CoordinationListResponse.from(it) }
    }

    fun findByStyle(style: CoordinationStyle, pageable: Pageable): Page<CoordinationListResponse> {
        return coordinationRepository.findByStyleAndStatusAndDeletedAtIsNull(
            style, CoordinationStatus.ACTIVE, pageable
        ).map { CoordinationListResponse.from(it) }
    }

    private fun findCoordinationById(id: Long): Coordination {
        return coordinationRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "코디를 찾을 수 없습니다: $id") }
    }
}
