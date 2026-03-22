package com.closet.display.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.display.application.dto.BannerCreateRequest
import com.closet.display.application.dto.BannerResponse
import com.closet.display.application.dto.BannerUpdateRequest
import com.closet.display.domain.entity.Banner
import com.closet.display.domain.enums.BannerPosition
import com.closet.display.domain.repository.BannerRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class BannerService(
    private val bannerRepository: BannerRepository
) {

    @Transactional
    fun create(request: BannerCreateRequest): BannerResponse {
        require(request.endAt.isAfter(request.startAt)) { "종료일시는 시작일시 이후여야 합니다" }
        val banner = Banner(
            title = request.title,
            imageUrl = request.imageUrl,
            linkUrl = request.linkUrl,
            position = request.position,
            sortOrder = request.sortOrder,
            startAt = request.startAt,
            endAt = request.endAt
        )
        val saved = bannerRepository.save(banner)
        logger.info { "배너 생성 완료: id=${saved.id}, title=${saved.title}" }
        return BannerResponse.from(saved)
    }

    @Transactional
    fun update(id: Long, request: BannerUpdateRequest): BannerResponse {
        val banner = findBannerById(id)
        banner.update(
            title = request.title,
            imageUrl = request.imageUrl,
            linkUrl = request.linkUrl,
            position = request.position,
            sortOrder = request.sortOrder,
            startAt = request.startAt,
            endAt = request.endAt
        )
        logger.info { "배너 수정 완료: id=$id" }
        return BannerResponse.from(banner)
    }

    fun findActive(position: BannerPosition): List<BannerResponse> {
        val now = LocalDateTime.now()
        return bannerRepository.findActiveByPosition(position, now)
            .map { BannerResponse.from(it) }
    }

    @Transactional
    fun toggleVisibility(id: Long): BannerResponse {
        val banner = findBannerById(id)
        if (banner.isVisible) {
            banner.hide()
        } else {
            banner.show()
        }
        logger.info { "배너 노출 상태 변경: id=$id, isVisible=${banner.isVisible}" }
        return BannerResponse.from(banner)
    }

    private fun findBannerById(id: Long): Banner {
        return bannerRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "배너를 찾을 수 없습니다: $id") }
    }
}
