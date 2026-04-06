package com.closet.common.exception

enum class ErrorCode(
    val code: String,
    val message: String,
    val status: Int,
) {
    // Common
    INVALID_INPUT("C001", "잘못된 입력값입니다", 400),
    ENTITY_NOT_FOUND("C002", "엔티티를 찾을 수 없습니다", 404),
    INTERNAL_SERVER_ERROR("C003", "서버 오류가 발생했습니다", 500),
    UNAUTHORIZED("C004", "인증이 필요합니다", 401),
    FORBIDDEN("C005", "접근 권한이 없습니다", 403),
    DUPLICATE_ENTITY("C006", "이미 존재하는 엔티티입니다", 409),
    INVALID_STATE_TRANSITION("C007", "잘못된 상태 전이입니다", 400),
}
