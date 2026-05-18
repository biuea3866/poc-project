package com.hrplatform.auth.domain.role

import com.hrplatform.core.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class RoleTest : BehaviorSpec({

    given("시스템 역할") {
        val systemRole = Role(
            companyId = null,
            code = "ADMIN",
            name = "관리자",
            description = "전체 관리자",
            isSystemRole = true,
        )

        `when`("validateNotSystem 호출 시") {
            then("BusinessException 발생") {
                shouldThrow<BusinessException> {
                    systemRole.validateNotSystem()
                }
            }
        }

        `when`("updateInfo 호출 시") {
            then("BusinessException 발생") {
                shouldThrow<BusinessException> {
                    systemRole.updateInfo("변경", null)
                }
            }
        }
    }

    given("회사 커스텀 역할") {
        val customRole = Role(
            companyId = 1L,
            code = "CUSTOM",
            name = "커스텀 역할",
            description = "회사 전용 역할",
            isSystemRole = false,
        )

        `when`("updateInfo 호출 시") {
            customRole.updateInfo("수정된 이름", "새 설명")
            then("이름과 설명이 변경된다") {
                customRole.name shouldBe "수정된 이름"
                customRole.description shouldBe "새 설명"
            }
        }
    }
})
