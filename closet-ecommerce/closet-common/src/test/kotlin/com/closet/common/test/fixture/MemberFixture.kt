package com.closet.common.test.fixture

import java.time.ZonedDateTime

/**
 * Member 도메인 테스트 Fixture.
 *
 * 테스트에서 Member 관련 객체를 간편하게 생성한다.
 * 엔티티가 아직 구현되지 않은 시점에서 Map 기반으로 데이터를 표현하며,
 * 엔티티 구현 후 실제 타입으로 교체한다.
 */
object MemberFixture {
    fun createMember(
        email: String = "test@closet.com",
        password: String = "Password123!",
        nickname: String = "테스트유저",
        phone: String = "010-1234-5678",
        gender: String = "MALE",
        birthYear: Int = 1995,
    ): Map<String, Any?> =
        mapOf(
            "email" to email,
            "password" to password,
            "nickname" to nickname,
            "phone" to phone,
            "gender" to gender,
            "birthYear" to birthYear,
            "grade" to "NORMAL",
            "point" to 0L,
            "createdAt" to ZonedDateTime.now(),
            "deletedAt" to null,
        )

    fun createShippingAddress(
        memberId: Long = 1L,
        name: String = "집",
        recipientName: String = "홍길동",
        phone: String = "010-1234-5678",
        zipCode: String = "06234",
        address: String = "서울시 강남구 테헤란로 123",
        addressDetail: String = "4층 401호",
        isDefault: Boolean = true,
    ): Map<String, Any?> =
        mapOf(
            "memberId" to memberId,
            "name" to name,
            "recipientName" to recipientName,
            "phone" to phone,
            "zipCode" to zipCode,
            "address" to address,
            "addressDetail" to addressDetail,
            "isDefault" to isDefault,
            "createdAt" to ZonedDateTime.now(),
        )

    fun createLoginRequest(
        email: String = "test@closet.com",
        password: String = "Password123!",
    ): Map<String, String> =
        mapOf(
            "email" to email,
            "password" to password,
        )

    fun createSignUpRequest(
        email: String = "newuser@closet.com",
        password: String = "Password123!",
        nickname: String = "새유저",
        phone: String = "010-9999-8888",
    ): Map<String, String> =
        mapOf(
            "email" to email,
            "password" to password,
            "nickname" to nickname,
            "phone" to phone,
        )
}
