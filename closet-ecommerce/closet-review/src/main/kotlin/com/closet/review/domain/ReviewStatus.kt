package com.closet.review.domain

/**
 * 리뷰 상태 enum.
 *
 * VISIBLE: 정상 노출
 * HIDDEN: 관리자 블라인드 (PD-35)
 * DELETED: 작성자 삭제
 */
enum class ReviewStatus {
    VISIBLE,
    HIDDEN,
    DELETED;

    fun canTransitionTo(target: ReviewStatus): Boolean {
        return when (this) {
            VISIBLE -> target in setOf(HIDDEN, DELETED)
            HIDDEN -> target in setOf(VISIBLE, DELETED)
            DELETED -> false
        }
    }

    fun validateTransitionTo(target: ReviewStatus) {
        require(canTransitionTo(target)) {
            "리뷰 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }
}
