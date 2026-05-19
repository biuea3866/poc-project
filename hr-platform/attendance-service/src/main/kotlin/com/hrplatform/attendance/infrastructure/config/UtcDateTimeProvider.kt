package com.hrplatform.attendance.infrastructure.config

import org.springframework.data.auditing.DateTimeProvider
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.TemporalAccessor
import java.util.Optional

/**
 * JPA Auditing 에 사용할 UTC ZonedDateTime 제공자.
 *
 * Spring Data JPA 의 @CreatedDate / @LastModifiedDate 가 ZonedDateTime 타입 필드를
 * 처리하기 위해 DateTimeProvider 가 ZonedDateTime(TemporalAccessor 구현체) 을 반환해야 한다.
 */
@Component("utcDateTimeProvider")
class UtcDateTimeProvider : DateTimeProvider {

    override fun getNow(): Optional<TemporalAccessor> =
        Optional.of(ZonedDateTime.now(ZoneOffset.UTC))
}
