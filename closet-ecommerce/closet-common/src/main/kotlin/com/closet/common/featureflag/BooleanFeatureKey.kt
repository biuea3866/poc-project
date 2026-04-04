package com.closet.common.featureflag

/**
 * 런타임 Feature Flag 키 인터페이스.
 *
 * SimpleRuntimeConfig 패턴에서 DB 기반 런타임 on/off를 지원한다.
 * 각 Phase의 Feature Key enum이 이 인터페이스를 구현한다.
 */
interface BooleanFeatureKey {

    /** DB에 저장되는 고유 키 문자열 */
    val key: String

    /** Feature Flag 설명 */
    val description: String

    /** 기본값 (DB에 없을 때 사용) */
    val defaultValue: Boolean
        get() = false
}
