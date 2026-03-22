package com.closet.member.domain

import com.closet.common.entity.BaseEntity
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * 회원 Aggregate Root
 */
@Entity
@Table(name = "member")
class Member(
    @Column(nullable = false, unique = true, length = 200)
    val email: String,

    @Column(name = "password_hash", length = 200)
    var passwordHash: String?,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(length = 20)
    var phone: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var grade: MemberGrade = MemberGrade.NORMAL,

    @Column(name = "point_balance", nullable = false)
    var pointBalance: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: MemberStatus = MemberStatus.ACTIVE,
) : BaseEntity() {

    companion object {
        /** 회원 가입 팩토리 메서드 */
        fun register(
            email: String,
            passwordHash: String,
            name: String,
            phone: String? = null,
        ): Member {
            return Member(
                email = email,
                passwordHash = passwordHash,
                name = name,
                phone = phone,
                grade = MemberGrade.NORMAL,
                pointBalance = 0,
                status = MemberStatus.ACTIVE,
            )
        }
    }

    /** 회원 탈퇴 */
    fun withdraw() {
        status.validateTransitionTo(MemberStatus.WITHDRAWN)
        this.status = MemberStatus.WITHDRAWN
        softDelete()
    }

    /** 등급 변경 */
    fun upgradeGrade(newGrade: MemberGrade) {
        grade.validateTransitionTo(newGrade)
        this.grade = newGrade
    }

    /** 포인트 적립 */
    fun earnPoints(amount: Int) {
        require(amount > 0) { "적립 포인트는 0보다 커야 합니다" }
        this.pointBalance += amount
    }

    /** 포인트 사용 */
    fun usePoints(amount: Int) {
        require(amount > 0) { "사용 포인트는 0보다 커야 합니다" }
        if (this.pointBalance < amount) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "포인트 잔액이 부족합니다")
        }
        this.pointBalance -= amount
    }
}
