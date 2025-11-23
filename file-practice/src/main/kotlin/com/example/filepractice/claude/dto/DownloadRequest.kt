package com.example.filepractice.claude.dto

/**
 * 파일 다운로드 요청 DTO
 *
 * @property userId 사용자 ID
 * @property email 사용자 이메일 (비동기 처리 시 사용)
 * @property columnConfig 컬럼 설정
 */
data class DownloadRequest(
    val userId: Long,
    val email: String? = null,
    val columnConfig: ColumnConfig = ColumnConfig.default()
)
