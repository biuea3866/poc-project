package com.closet.content.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.content.application.dto.MagazineCreateRequest
import com.closet.content.application.dto.MagazineListResponse
import com.closet.content.application.dto.MagazineResponse
import com.closet.content.domain.entity.Magazine
import com.closet.content.domain.enums.MagazineStatus
import com.closet.content.domain.repository.MagazineRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class MagazineService(
    private val magazineRepository: MagazineRepository
) {

    @Transactional
    fun create(request: MagazineCreateRequest): MagazineResponse {
        val magazine = Magazine(
            title = request.title,
            subtitle = request.subtitle,
            content = request.content,
            thumbnailUrl = request.thumbnailUrl,
            author = request.author
        )
        request.tags.forEach { magazine.addTag(it) }
        val saved = magazineRepository.save(magazine)
        logger.info { "매거진 생성 완료: id=${saved.id}, title=${saved.title}" }
        return MagazineResponse.from(saved)
    }

    @Transactional
    fun publish(id: Long): MagazineResponse {
        val magazine = findMagazineById(id)
        magazine.publish()
        logger.info { "매거진 발행 완료: id=$id" }
        return MagazineResponse.from(magazine)
    }

    @Transactional
    fun archive(id: Long): MagazineResponse {
        val magazine = findMagazineById(id)
        magazine.archive()
        logger.info { "매거진 보관 처리 완료: id=$id" }
        return MagazineResponse.from(magazine)
    }

    fun findById(id: Long): MagazineResponse {
        val magazine = findMagazineById(id)
        return MagazineResponse.from(magazine)
    }

    fun findAll(pageable: Pageable): Page<MagazineListResponse> {
        return magazineRepository.findByDeletedAtIsNull(pageable)
            .map { MagazineListResponse.from(it) }
    }

    fun findByTag(tagName: String, pageable: Pageable): Page<MagazineListResponse> {
        return magazineRepository.findByTagName(tagName, pageable)
            .map { MagazineListResponse.from(it) }
    }

    private fun findMagazineById(id: Long): Magazine {
        return magazineRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "매거진을 찾을 수 없습니다: $id") }
    }
}
