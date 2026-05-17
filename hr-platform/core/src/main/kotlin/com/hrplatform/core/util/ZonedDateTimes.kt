package com.hrplatform.core.util

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ZonedDateTimes {

    // ISO-8601 with microsecond precision (DATETIME(6) DB 컬럼·Kafka 이벤트 occurredAt 정밀도 보존)
    private val ISO_8601_MICROS: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX").withZone(ZoneOffset.UTC)

    fun nowUtc(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

    fun parseIso8601(s: String): ZonedDateTime =
        ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .withZoneSameInstant(ZoneOffset.UTC)

    fun toIso8601(zdt: ZonedDateTime): String =
        ISO_8601_MICROS.format(zdt.withZoneSameInstant(ZoneOffset.UTC))
}
