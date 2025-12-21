package com.example.filepractice.poc.domain.newexcel

/**
 * 셀 렌더링 타입
 */
enum class CellType {
    /**
     * 단일 값을 하나의 셀에 표시 (기본값)
     */
    SINGLE,

    /**
     * 리스트를 여러 셀로 펼쳐서 표시 (기본 isList 동작)
     * 예: [A, B, C] -> | A | B | C |
     */
    LIST_EXPANDED,

    /**
     * 리스트를 하나의 셀에 join해서 표시
     * 예: [A, B, C] -> | A, B, C |
     */
    LIST_JOINED
}
