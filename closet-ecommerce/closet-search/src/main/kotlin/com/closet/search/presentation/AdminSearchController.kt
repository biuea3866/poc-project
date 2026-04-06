package com.closet.search.presentation

import com.closet.search.application.dto.IndexSyncResponse
import com.closet.search.application.service.BannedKeywordService
import com.closet.search.application.service.PopularKeywordService
import com.closet.search.application.service.ProductSearchService
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

/**
 * 검색 어드민 API 컨트롤러.
 *
 * POST /api/v1/admin/search/reindex                   - 벌크 리인덱싱
 * GET  /api/v1/admin/search/banned-keywords            - 금칙어 목록 (CP-21)
 * POST /api/v1/admin/search/banned-keywords            - 금칙어 추가 (CP-21)
 * DELETE /api/v1/admin/search/banned-keywords           - 금칙어 삭제 (CP-21)
 * DELETE /api/v1/admin/search/popular-keywords          - 인기 검색어 초기화 (CP-21)
 */
@RestController
@RequestMapping("/api/v1/admin/search")
class AdminSearchController(
    private val productSearchService: ProductSearchService,
    private val bannedKeywordService: BannedKeywordService,
    private val popularKeywordService: PopularKeywordService,
) {
    /**
     * 벌크 리인덱싱 API.
     */
    @PostMapping("/reindex")
    fun reindex(): ResponseEntity<IndexSyncResponse> {
        logger.info { "벌크 리인덱싱 API 호출" }
        val result = productSearchService.bulkReindex()
        logger.info {
            "벌크 리인덱싱 완료: totalIndexed=${result.totalIndexed}, " +
                "totalFailed=${result.totalFailed}, elapsed=${result.elapsedMillis}ms"
        }
        return ResponseEntity.ok(result)
    }

    /**
     * 금칙어 목록 조회 (CP-21, PD-39).
     */
    @GetMapping("/banned-keywords")
    fun getBannedKeywords(): ResponseEntity<Set<String>> {
        return ResponseEntity.ok(bannedKeywordService.getAllBannedKeywords())
    }

    /**
     * 금칙어 추가 (CP-21, PD-39).
     */
    @PostMapping("/banned-keywords")
    fun addBannedKeyword(
        @RequestBody request: BannedKeywordRequest,
    ): ResponseEntity<Void> {
        bannedKeywordService.addBannedKeyword(request.keyword)
        return ResponseEntity.ok().build()
    }

    /**
     * 금칙어 삭제 (CP-21, PD-39).
     */
    @DeleteMapping("/banned-keywords")
    fun removeBannedKeyword(
        @RequestBody request: BannedKeywordRequest,
    ): ResponseEntity<Void> {
        bannedKeywordService.removeBannedKeyword(request.keyword)
        return ResponseEntity.noContent().build()
    }

    /**
     * 인기 검색어 초기화 (관리용).
     */
    @DeleteMapping("/popular-keywords")
    fun resetPopularKeywords(): ResponseEntity<Void> {
        popularKeywordService.resetPopularKeywords()
        return ResponseEntity.noContent().build()
    }

    data class BannedKeywordRequest(val keyword: String)
}
