package com.closet.product.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.product.application.dto.BrandCreateRequest
import com.closet.product.application.dto.BrandResponse
import com.closet.product.domain.entity.Brand
import com.closet.product.domain.repository.BrandRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class BrandService(
    private val brandRepository: BrandRepository
) {

    fun findAll(): List<BrandResponse> {
        return brandRepository.findByDeletedAtIsNull()
            .map { BrandResponse.from(it) }
    }

    fun findById(id: Long): BrandResponse {
        val brand = brandRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "브랜드를 찾을 수 없습니다: $id") }
        return BrandResponse.from(brand)
    }

    @Transactional
    fun create(request: BrandCreateRequest): BrandResponse {
        val brand = Brand(
            name = request.name,
            logoUrl = request.logoUrl,
            description = request.description,
            sellerId = request.sellerId
        )
        val saved = brandRepository.save(brand)
        logger.info { "브랜드 생성 완료: id=${saved.id}, name=${saved.name}" }
        return BrandResponse.from(saved)
    }
}
