package com.closet.cs.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.cs.domain.Faq
import com.closet.cs.domain.FaqCategory
import com.closet.cs.domain.repository.FaqRepository
import com.closet.cs.presentation.dto.CreateFaqRequest
import com.closet.cs.presentation.dto.FaqResponse
import com.closet.cs.presentation.dto.UpdateFaqRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FaqService(
    private val faqRepository: FaqRepository,
) {
    /** FAQ 등록 */
    @Transactional
    fun create(request: CreateFaqRequest): FaqResponse {
        val faq = Faq.create(
            category = request.category,
            question = request.question,
            answer = request.answer,
            sortOrder = request.sortOrder,
        )

        val saved = faqRepository.save(faq)
        return FaqResponse.from(saved)
    }

    /** FAQ 수정 */
    @Transactional
    fun update(id: Long, request: UpdateFaqRequest): FaqResponse {
        val faq = faqRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "FAQ를 찾을 수 없습니다") }

        faq.updateContent(
            question = request.question,
            answer = request.answer,
            sortOrder = request.sortOrder,
        )

        return FaqResponse.from(faq)
    }

    /** 카테고리별 FAQ 목록 */
    fun findByCategory(category: FaqCategory): List<FaqResponse> {
        return faqRepository.findByCategoryAndIsVisibleTrueOrderBySortOrderAsc(category)
            .map { FaqResponse.from(it) }
    }

    /** 전체 FAQ 목록 (노출 중인 것만) */
    fun findAll(): List<FaqResponse> {
        return faqRepository.findByIsVisibleTrueOrderBySortOrderAsc()
            .map { FaqResponse.from(it) }
    }

    /** FAQ 노출 토글 */
    @Transactional
    fun toggleVisibility(id: Long): FaqResponse {
        val faq = faqRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "FAQ를 찾을 수 없습니다") }

        if (faq.isVisible) faq.hide() else faq.show()
        return FaqResponse.from(faq)
    }
}
