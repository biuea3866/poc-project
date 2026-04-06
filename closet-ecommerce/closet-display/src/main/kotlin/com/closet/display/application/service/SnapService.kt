package com.closet.display.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.display.application.dto.SnapCreateRequest
import com.closet.display.application.dto.SnapResponse
import com.closet.display.domain.entity.Snap
import com.closet.display.domain.entity.SnapLike
import com.closet.display.domain.entity.SnapProductTag
import com.closet.display.domain.enums.SnapStatus
import com.closet.display.domain.repository.SnapLikeRepository
import com.closet.display.domain.repository.SnapRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class SnapService(
    private val snapRepository: SnapRepository,
    private val snapLikeRepository: SnapLikeRepository,
) {
    @Transactional
    fun upload(request: SnapCreateRequest): SnapResponse {
        val snap =
            Snap.create(
                memberId = request.memberId,
                imageUrl = request.imageUrl,
                description = request.description,
            )

        request.productTags.forEach { tagRequest ->
            val tag =
                SnapProductTag(
                    productId = tagRequest.productId,
                    positionX = tagRequest.positionX,
                    positionY = tagRequest.positionY,
                )
            snap.addProductTag(tag)
        }

        val saved = snapRepository.save(snap)
        logger.info { "스냅 업로드: id=${saved.id}, memberId=${saved.memberId}" }
        return SnapResponse.from(saved)
    }

    fun getFeed(): List<SnapResponse> {
        return snapRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(SnapStatus.ACTIVE)
            .map { SnapResponse.from(it) }
    }

    fun getById(id: Long): SnapResponse {
        val snap = findSnapById(id)
        return SnapResponse.from(snap)
    }

    fun getByMember(memberId: Long): List<SnapResponse> {
        return snapRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId)
            .map { SnapResponse.from(it) }
    }

    @Transactional
    fun like(
        snapId: Long,
        memberId: Long,
    ): SnapResponse {
        val snap = findSnapById(snapId)

        if (snapLikeRepository.existsBySnapIdAndMemberId(snapId, memberId)) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 좋아요한 스냅입니다")
        }

        snap.like()
        snapLikeRepository.save(SnapLike(snapId = snapId, memberId = memberId))

        logger.info { "스냅 좋아요: snapId=$snapId, memberId=$memberId" }
        return SnapResponse.from(snap)
    }

    @Transactional
    fun unlike(
        snapId: Long,
        memberId: Long,
    ): SnapResponse {
        val snap = findSnapById(snapId)

        val snapLike =
            snapLikeRepository.findBySnapIdAndMemberId(snapId, memberId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "좋아요 기록을 찾을 수 없습니다")

        snap.unlike()
        snapLikeRepository.delete(snapLike)

        logger.info { "스냅 좋아요 취소: snapId=$snapId, memberId=$memberId" }
        return SnapResponse.from(snap)
    }

    @Transactional
    fun report(
        snapId: Long,
        memberId: Long,
    ): SnapResponse {
        val snap = findSnapById(snapId)
        snap.report()

        logger.info { "스냅 신고: snapId=$snapId, memberId=$memberId, reportCount=${snap.reportCount}" }
        return SnapResponse.from(snap)
    }

    @Transactional
    fun delete(id: Long) {
        val snap = findSnapById(id)
        snap.softDelete()
        logger.info { "스냅 삭제: id=$id" }
    }

    private fun findSnapById(id: Long): Snap {
        return snapRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "스냅을 찾을 수 없습니다: $id") }
    }
}
