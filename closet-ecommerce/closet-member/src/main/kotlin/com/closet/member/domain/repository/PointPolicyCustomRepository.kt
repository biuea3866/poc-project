package com.closet.member.domain.repository

import com.closet.member.domain.point.PointEventType
import com.closet.member.domain.point.PointPolicy

interface PointPolicyCustomRepository {
    /**
     * 활성 상태이면서 특정 이벤트 타입에 해당하는 포인트 정책 조회.
     * WHERE is_active = 1 AND event_type = :eventType
     */
    fun findActiveByEventType(eventType: PointEventType): List<PointPolicy>
}
