package com.closet.review.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

/**
 * 리뷰 작성 가능한 주문 아이템 엔티티.
 *
 * 주문 아이템이 구매확정(CONFIRMED) 상태가 되면 이 테이블에 기록되어,
 * 리뷰 작성 시 구매확정 여부를 검증할 수 있다.
 * order-service의 OrderItemConfirmed 이벤트를 Kafka로 수신하여 생성한다.
 */
@Entity
@Table(name = "reviewable_order_item")
@EntityListeners(AuditingEntityListener::class)
class ReviewableOrderItem(
    @Column(name = "order_item_id", nullable = false, unique = true)
    val orderItemId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    companion object {
        fun create(orderItemId: Long, memberId: Long, productId: Long): ReviewableOrderItem {
            return ReviewableOrderItem(
                orderItemId = orderItemId,
                memberId = memberId,
                productId = productId,
            )
        }
    }
}
