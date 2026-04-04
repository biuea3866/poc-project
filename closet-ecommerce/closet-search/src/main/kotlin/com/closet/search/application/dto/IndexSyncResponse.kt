package com.closet.search.application.dto

/**
 * 벌크 인덱싱/리인덱스 결과 응답 DTO.
 */
data class IndexSyncResponse(
    val totalRequested: Int,
    val totalIndexed: Int,
    val totalFailed: Int,
    val elapsedMillis: Long,
)
