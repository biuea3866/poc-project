package com.biuea.delivery.domain.rider

/**
 * 반경 확장까지 마친 최종 검색 결과. 어느 반경에서 찾았는지가 배차 품질 지표라 함께 보관한다.
 */
data class NearbyRiderSearchResult(
    val searchRadiusMeters: Int,
    val riders: List<NearbyRider>,
) {
    fun hasAtLeast(riderCount: Int): Boolean = riders.size >= riderCount
}
