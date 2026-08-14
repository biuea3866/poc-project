package com.biuea.delivery.domain.rider

import java.time.ZonedDateTime

/**
 * 라이더 위치의 신선도 정책.
 *
 * 라이더 앱은 주행 중 수 초 간격으로 위치를 올린다. 30초 넘게 소식이 없으면 앱이 죽었거나
 * 통신이 끊긴 것이라 그 좌표를 믿고 배차하면 엉뚱한 위치의 라이더에게 배달을 준다.
 * 그래서 인덱스에는 남아 있어도 배차 후보에서는 제외한다.
 */
object RiderLocationFreshness {

    const val MAX_AGE_SECONDS = 30L

    fun isFresh(updatedAt: ZonedDateTime): Boolean =
        updatedAt.isAfter(ZonedDateTime.now().minusSeconds(MAX_AGE_SECONDS))
}
