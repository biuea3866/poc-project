package com.closet.display.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.display.application.dto.MagazineCreateRequest
import com.closet.display.application.dto.MagazineProductCreateRequest
import com.closet.display.application.dto.MagazineProductResponse
import com.closet.display.application.dto.MagazineResponse
import com.closet.display.application.dto.MagazineTagCreateRequest
import com.closet.display.application.dto.MagazineTagResponse
import com.closet.display.application.dto.MagazineUpdateRequest
import com.closet.display.domain.entity.Magazine
import com.closet.display.domain.entity.MagazineProduct
import com.closet.display.domain.entity.MagazineTag
import com.closet.display.domain.repository.MagazineRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class MagazineService(
    private val magazineRepository: MagazineRepository,
) {
    @Transactional
    fun create(request: MagazineCreateRequest): MagazineResponse {
        val magazine =
            Magazine(
                title = request.title,
                subtitle = request.subtitle,
                contentBody = request.contentBody,
                thumbnailUrl = request.thumbnailUrl,
                category = request.category,
                authorName = request.authorName,
            )

        val saved = magazineRepository.save(magazine)
        logger.info { "매거진 생성: id=${saved.id}, title=${saved.title}" }
        return MagazineResponse.from(saved)
    }

    @Transactional
    fun update(
        id: Long,
        request: MagazineUpdateRequest,
    ): MagazineResponse {
        val magazine = findMagazineById(id)
        magazine.update(
            title = request.title,
            subtitle = request.subtitle,
            contentBody = request.contentBody,
            thumbnailUrl = request.thumbnailUrl,
            category = request.category,
            authorName = request.authorName,
        )

        logger.info { "매거진 수정: id=$id" }
        return MagazineResponse.from(magazine)
    }

    fun getById(id: Long): MagazineResponse {
        val magazine = findMagazineById(id)
        return MagazineResponse.from(magazine)
    }

    fun getPublishedList(category: String?): List<MagazineResponse> {
        val magazines =
            if (category != null) {
                magazineRepository.findByCategoryAndIsPublishedTrueAndDeletedAtIsNullOrderByPublishedAtDesc(category)
            } else {
                magazineRepository.findByIsPublishedTrueAndDeletedAtIsNullOrderByPublishedAtDesc()
            }
        return magazines.map { MagazineResponse.from(it) }
    }

    @Transactional
    fun publish(id: Long): MagazineResponse {
        val magazine = findMagazineById(id)
        magazine.publish()
        logger.info { "매거진 발행: id=$id" }
        return MagazineResponse.from(magazine)
    }

    @Transactional
    fun unpublish(id: Long): MagazineResponse {
        val magazine = findMagazineById(id)
        magazine.unpublish()
        logger.info { "매거진 비발행: id=$id" }
        return MagazineResponse.from(magazine)
    }

    @Transactional
    fun addProduct(
        magazineId: Long,
        request: MagazineProductCreateRequest,
    ): MagazineProductResponse {
        val magazine = findMagazineById(magazineId)
        val product =
            MagazineProduct(
                productId = request.productId,
                sortOrder = request.sortOrder,
            )
        magazine.addProduct(product)
        magazineRepository.flush()
        logger.info { "매거진 상품 추가: magazineId=$magazineId, productId=${request.productId}" }
        return MagazineProductResponse.from(product)
    }

    @Transactional
    fun removeProduct(
        magazineId: Long,
        productId: Long,
    ) {
        val magazine = findMagazineById(magazineId)
        magazine.removeProduct(productId)
        logger.info { "매거진 상품 제거: magazineId=$magazineId, productId=$productId" }
    }

    @Transactional
    fun addTag(
        magazineId: Long,
        request: MagazineTagCreateRequest,
    ): MagazineTagResponse {
        val magazine = findMagazineById(magazineId)
        val tag = MagazineTag(tagName = request.tagName)
        magazine.addTag(tag)
        magazineRepository.flush()
        logger.info { "매거진 태그 추가: magazineId=$magazineId, tagName=${request.tagName}" }
        return MagazineTagResponse.from(tag)
    }

    @Transactional
    fun removeTag(
        magazineId: Long,
        tagName: String,
    ) {
        val magazine = findMagazineById(magazineId)
        magazine.removeTag(tagName)
        logger.info { "매거진 태그 제거: magazineId=$magazineId, tagName=$tagName" }
    }

    @Transactional
    fun delete(id: Long) {
        val magazine = findMagazineById(id)
        magazine.softDelete()
        logger.info { "매거진 삭제: id=$id" }
    }

    private fun findMagazineById(id: Long): Magazine {
        return magazineRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "매거진을 찾을 수 없습니다: $id") }
    }
}
