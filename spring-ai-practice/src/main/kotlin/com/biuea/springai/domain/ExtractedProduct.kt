package com.biuea.springai.domain

/**
 * `BeanOutputConverter` 가 LLM 응답을 strict JSON 으로 강제할 때의 **타겟 데이터 클래스**.
 *
 * Spring AI 는 이 클래스의 구조를 JSON Schema 로 변환해 LLM 프롬프트에 추가한다.
 * → 모델이 "이 스키마에 맞춰 답하라" 는 지침을 받게 되어 자유 텍스트 응답을 받기보다
 *    안정적으로 데이터 클래스로 역직렬화할 수 있다.
 *
 * 모든 필드가 nullable + 기본값 — LLM 이 추출하지 못한 항목을 null 로 두는 게 자연스럽다.
 */
data class ExtractedProduct(
    val category: String? = null,
    val color: String? = null,
    val material: String? = null,
    val fit: String? = null,
    val sizeHint: String? = null,
    val season: String? = null,
    val keywords: List<String> = emptyList(),
)
