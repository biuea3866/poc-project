package com.hrplatform.core.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BusinessExceptionHierarchyTest : BehaviorSpec({

    given("BusinessException 5종 상속 계층") {
        `when`("NotFoundException 인스턴스를 catch BusinessException 으로 잡으면") {
            val e: Exception = NotFoundException("E_NOT_FOUND", "missing")
            then("BusinessException 으로 잡힌다 + errorCode 노출") {
                e.shouldBeInstanceOf<BusinessException>()
                (e as BusinessException).errorCode shouldBe "E_NOT_FOUND"
            }
        }

        `when`("UnauthorizedException 인스턴스를 catch BusinessException 으로 잡으면") {
            val e: Exception = UnauthorizedException("E_UNAUTH", "no token")
            then("BusinessException 으로 잡힌다") {
                e.shouldBeInstanceOf<BusinessException>()
            }
        }

        `when`("ForbiddenException 인스턴스를 catch BusinessException 으로 잡으면") {
            val e: Exception = ForbiddenException("E_FORBIDDEN", "no permission")
            then("BusinessException 으로 잡힌다") {
                e.shouldBeInstanceOf<BusinessException>()
            }
        }

        `when`("ConflictException 인스턴스를 catch BusinessException 으로 잡으면") {
            val e: Exception = ConflictException("E_CONFLICT", "duplicate")
            then("BusinessException 으로 잡힌다") {
                e.shouldBeInstanceOf<BusinessException>()
            }
        }

        `when`("BusinessException 인스턴스 그 자체를 사용하면") {
            val e = BusinessException("E_BIZ", "generic")
            then("errorCode 와 message 가 보존된다") {
                e.errorCode shouldBe "E_BIZ"
                e.message shouldBe "generic"
            }
        }
    }
})
