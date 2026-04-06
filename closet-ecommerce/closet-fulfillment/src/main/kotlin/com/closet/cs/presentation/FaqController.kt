package com.closet.cs.presentation

import com.closet.common.response.ApiResponse
import com.closet.cs.application.FaqService
import com.closet.cs.domain.FaqCategory
import com.closet.cs.presentation.dto.CreateFaqRequest
import com.closet.cs.presentation.dto.FaqResponse
import com.closet.cs.presentation.dto.UpdateFaqRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/cs")
class FaqController(
    private val faqService: FaqService,
) {
    /** FAQ 목록 (카테고리 필터 가능) */
    @GetMapping("/faqs")
    fun getFaqs(
        @RequestParam(required = false) category: FaqCategory?,
    ): ApiResponse<List<FaqResponse>> {
        val result =
            if (category != null) {
                faqService.findByCategory(category)
            } else {
                faqService.findAll()
            }
        return ApiResponse.ok(result)
    }

    /** FAQ 등록 */
    @PostMapping("/faqs")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFaq(
        @Valid @RequestBody request: CreateFaqRequest,
    ): ApiResponse<FaqResponse> {
        return ApiResponse.created(faqService.create(request))
    }

    /** FAQ 수정 */
    @PutMapping("/faqs/{id}")
    fun updateFaq(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateFaqRequest,
    ): ApiResponse<FaqResponse> {
        return ApiResponse.ok(faqService.update(id, request))
    }
}
