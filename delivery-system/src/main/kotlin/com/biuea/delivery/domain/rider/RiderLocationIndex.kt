package com.biuea.delivery.domain.rider

import com.biuea.delivery.domain.geo.Coordinate

/**
 * 라이더 위치 공간 인덱스 포트.
 *
 * 전수 스캔·Geohash·H3·Redis GEO 네 구현이 이 계약을 동일하게 만족해야 하고,
 * 같은 입력에는 같은 라이더 집합을 돌려줘야 한다.
 */
interface RiderLocationIndex {

    /** 라이더 위치를 최신 값으로 덮어쓴다. 같은 라이더의 이전 위치는 인덱스에서 사라져야 한다. */
    fun update(location: RiderLocation)

    /** 중심에서 반경 안의 라이더를 거리 오름차순으로 최대 limit 명 반환한다. */
    fun searchWithin(center: Coordinate, radiusMeters: Int, limit: Int): List<NearbyRider>

    /**
     * 정확 거리를 계산하기 전에 인덱스가 훑은 후보 수.
     * 공간 인덱스의 효율을 재는 관측 지점이다 — 후보가 적을수록 하버사인 계산과 네트워크 전송이 줄어든다.
     */
    fun candidateCount(center: Coordinate, radiusMeters: Int): Int

    /** 인덱스를 비운다. 벤치마크·테스트 초기화용이다. */
    fun clear()
}
