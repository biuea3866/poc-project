package com.biuea.springai.chat

/**
 * `/chat/vision` 비전 모델 응답의 구조화 출력 타겟 — 작은 모델 친화적으로 필드를 축소.
 *
 * 필드:
 * - `description` — 사용자에게 보여줄 한국어 자연어 설명
 * - `detectedCategory` — 추정 카테고리 (예: 셔츠, 자켓, 청바지)
 * - `detectedColor` — 주요 색상
 */
data class VisionAnswer(
    val description: String = "",
    val detectedCategory: String? = null,
    val detectedColor: String? = null,
)
