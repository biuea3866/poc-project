package com.hrplatform.core.domain

import java.time.ZonedDateTime
import java.util.UUID

/**
 * 외부로 발행되는 도메인 이벤트의 표준 페이로드 규약.
 *
 * 토픽 명명: event.{domain}.v{n}
 * 페이로드: action(무엇을 했는가) + state(행위 직후 aggregate 상태) 모두 포함
 *
 * 한 aggregate의 모든 이벤트가 같은 토픽으로 발행되고 aggregateId가 파티션 키가 되어
 * partition-level 순서 보장이 가능합니다.
 */
interface DomainEvent {
    val eventId: UUID                       // 멱등 키
    val eventType: String                   // 행위 이름 PascalCase (예: "EmployeeHired")
    val eventVersion: Int                   // 페이로드 스키마 버전 (1부터 시작)
    val occurredAt: ZonedDateTime           // ISO-8601 UTC 마이크로초

    val aggregateType: String               // 예: "Employment"
    val aggregateId: Long                   // 파티션 키
    val companyId: Long
    val actorEmploymentId: Long?            // 변경 주체, 시스템은 null

    val action: DomainEventAction           // 무엇을 했는가
    val state: DomainEventState             // 행위 직후 aggregate snapshot
}
