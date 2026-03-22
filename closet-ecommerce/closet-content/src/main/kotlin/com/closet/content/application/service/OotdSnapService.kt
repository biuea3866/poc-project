package com.closet.content.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.content.application.dto.OotdSnapCreateRequest
import com.closet.content.application.dto.OotdSnapListResponse
import com.closet.content.application.dto.OotdSnapResponse
import com.closet.content.domain.entity.OotdSnap
import com.closet.content.domain.enums.OotdSnapStatus
import com.closet.content.domain.repository.OotdSnapRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class OotdSnapService(
    private val ootdSnapRepository: OotdSnapRepository
) {

    @Transactional
    fun create(memberId: Long, request: OotdSnapCreateRequest): OotdSnapResponse {
        val snap = OotdSnap(
            memberId = memberId,
            imageUrl = request.imageUrl,
            content = request.content
        )
        val saved = ootdSnapRepository.save(snap)
        logger.info { "OOTD 스냅 생성 완료: id=${saved.id}, memberId=$memberId" }
        return OotdSnapResponse.from(saved)
    }

    @Transactional
    fun like(id: Long): OotdSnapResponse {
        val snap = findSnapById(id)
        snap.like()
        logger.info { "OOTD 스냅 좋아요: id=$id, likeCount=${snap.likeCount}" }
        return OotdSnapResponse.from(snap)
    }

    @Transactional
    fun hide(id: Long): OotdSnapResponse {
        val snap = findSnapById(id)
        snap.hide()
        logger.info { "OOTD 스냅 숨김 처리: id=$id" }
        return OotdSnapResponse.from(snap)
    }

    @Transactional
    fun delete(id: Long) {
        val snap = findSnapById(id)
        snap.delete()
        logger.info { "OOTD 스냅 삭제 처리: id=$id" }
    }

    fun findById(id: Long): OotdSnapResponse {
        val snap = findSnapById(id)
        return OotdSnapResponse.from(snap)
    }

    fun findAll(pageable: Pageable): Page<OotdSnapListResponse> {
        return ootdSnapRepository.findByStatusAndDeletedAtIsNull(OotdSnapStatus.ACTIVE, pageable)
            .map { OotdSnapListResponse.from(it) }
    }

    fun findByMember(memberId: Long, pageable: Pageable): Page<OotdSnapListResponse> {
        return ootdSnapRepository.findByMemberIdAndStatusAndDeletedAtIsNull(
            memberId, OotdSnapStatus.ACTIVE, pageable
        ).map { OotdSnapListResponse.from(it) }
    }

    private fun findSnapById(id: Long): OotdSnap {
        return ootdSnapRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "OOTD 스냅을 찾을 수 없습니다: $id") }
    }
}
