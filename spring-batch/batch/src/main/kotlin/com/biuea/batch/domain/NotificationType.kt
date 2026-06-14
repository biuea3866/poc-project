package com.biuea.batch.domain

/**
 * 예약 알림 유형. group_key 의 의미는 유형마다 다르다.
 * - POST_COMMENT: group_key = postId (게시글 단위로 댓글 알림을 묶는다)
 * - KEYWORD_INTEREST: group_key = keyword (키워드 단위로 신규 물건 알림을 묶는다)
 * - TICKET_REMINDER: group_key = ticketId (예매 단위 리마인드)
 */
enum class NotificationType(val label: String) {
    POST_COMMENT("게시글"),
    KEYWORD_INTEREST("관심 키워드"),
    TICKET_REMINDER("예매"),
    ;

    fun singleContent(groupKey: String, payload: String): String =
        "[$label] $groupKey · $payload"

    fun bundledContent(groupKey: String, count: Int): String =
        "[$label] $groupKey 외 묶음 ${count}건"
}
