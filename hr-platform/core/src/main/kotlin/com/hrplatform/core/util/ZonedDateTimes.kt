package com.hrplatform.core.util

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ZonedDateTimes {

    private val ISO_8601_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        .withZone(ZoneOffset.UTC)

    fun nowUtc(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

    fun parseIso8601(s: String): ZonedDateTime =
        ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .withZoneSameInstant(ZoneOffset.UTC)

    fun toIso8601(zdt: ZonedDateTime): String =
        ISO_8601_FORMATTER.format(zdt.withZoneSameInstant(ZoneOffset.UTC))
}
