package com.biuea.delivery.domain.rider

/**
 * 라이더 이동 수단. 이동 속도가 다르므로 같은 배달 소요시간을 만족하는 탐색 반경도 다르다.
 */
enum class VehicleType(
    val defaultSearchRadiusMeters: Int,
) {
    WALK(1_500),
    BICYCLE(3_000),
    MOTORCYCLE(5_000),
    ;

    /**
     * 후보가 부족할 때 넓혀갈 반경 단계. 기본 반경에서 2km 씩 두 번 확장한다.
     * 자전거 기준 3km → 5km → 7km 이며, 무한 확장은 배달 시간 약속을 깨므로 상한을 둔다.
     */
    fun searchRadiusLadderMeters(): List<Int> =
        (0..EXPANSION_COUNT).map { step -> defaultSearchRadiusMeters + step * EXPANSION_STEP_METERS }

    companion object {
        private const val EXPANSION_STEP_METERS = 2_000
        private const val EXPANSION_COUNT = 2
    }
}
