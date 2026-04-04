package com.closet.search.presentation

import com.closet.search.application.dto.IndexSyncResponse
import com.closet.search.application.service.ProductSearchService
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

/**
 * 검색 어드민 API 컨트롤러.
 *
 * POST /api/v1/admin/search/reindex - 전체 상품 벌크 리인덱싱 (ADMIN 전용)
 */
@RestController
@RequestMapping("/api/v1/admin/search")
class AdminSearchController(
    private val productSearchService: ProductSearchService,
) {

    /**
     * 벌크 리인덱싱 API.
     *
     * closet-product 서비스에서 전체 상품을 1000건 단위로 조회하여 ES에 벌크 인덱싱한다.
     * ADMIN 권한이 필요하며, X-Member-Role: ADMIN 헤더가 필수이다.
     *
     * 향후 @RoleRequired(MemberRole.ADMIN) 어노테이션 적용 예정.
     */
    @PostMapping("/reindex")
    fun reindex(): ResponseEntity<IndexSyncResponse> {
        logger.info { "벌크 리인덱싱 API 호출" }

        val result = productSearchService.bulkReindex()

        logger.info { "벌크 리인덱싱 완료: totalIndexed=${result.totalIndexed}, totalFailed=${result.totalFailed}, elapsed=${result.elapsedMillis}ms" }

        return ResponseEntity.ok(result)
    }
}
