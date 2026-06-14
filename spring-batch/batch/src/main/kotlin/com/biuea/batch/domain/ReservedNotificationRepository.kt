package com.biuea.batch.domain

/**
 * 예약 알림 영속화 추상화. 구현체는 infrastructure 에 위치한다.
 * 배치 Reader/Writer 가 쓰는 grouped paging 쿼리·bulk update 는 infrastructure 전용 컴포넌트로 분리하고,
 * 이 interface 는 application(UseCase)이 필요로 하는 고수준 연산만 정의한다.
 */
interface ReservedNotificationRepository {
    /** 벤치마크 반복 사이에 sent 플래그를 false 로 복원한다. 갱신된 행 수를 반환한다. */
    fun resetSentFlags(): Int

    /** 전체 예약 알림(raw row) 건수. */
    fun countTotal(): Long

    /** sent=true 인 예약 알림(raw row) 건수. */
    fun countSent(): Long

    /** 파티셔닝 grid 분할에 쓰는 user_id 최소/최대 범위. 데이터가 없으면 null. */
    fun userIdRange(): UserIdRange?
}

/** user_id 범위(min, max). partitioning grid 분할에 사용한다. */
data class UserIdRange(
    val min: Long,
    val max: Long,
)
