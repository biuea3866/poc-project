package com.closet.product.application.event

/**
 * 상품 삭제 이벤트.
 * ApplicationEventPublisher를 통해 발행되며,
 * ProductOutboxListener가 outbox_event 테이블에 INSERT한다.
 */
data class ProductDeletedEvent(
    val productId: Long,
    val name: String,
    val brandId: Long,
    val categoryId: Long,
)
