package com.biuea.delivery.infrastructure.geo

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Redis 에 넣고 빼는 위치 갱신 시각 표기. 네 구현이 같은 형식을 쓰도록 한곳에 둔다.
 */
object RiderLocationTimestamp {

    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun format(updatedAt: ZonedDateTime): String = FORMAT.format(updatedAt)

    fun parse(text: String): ZonedDateTime = ZonedDateTime.parse(text, FORMAT)
}
