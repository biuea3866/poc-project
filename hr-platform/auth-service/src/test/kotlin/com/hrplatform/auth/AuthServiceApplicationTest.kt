package com.hrplatform.auth

import com.hrplatform.auth.infrastructure.persistence.account.UserAccountJpaRepository
import com.hrplatform.auth.infrastructure.persistence.login.LoginAttemptJpaRepository
import com.hrplatform.auth.infrastructure.persistence.role.RoleJpaRepository
import com.hrplatform.auth.infrastructure.persistence.role.UserAccountRoleJpaRepository
import com.hrplatform.auth.infrastructure.persistence.token.ApiTokenJpaRepository
import com.hrplatform.auth.infrastructure.persistence.token.RefreshTokenJpaRepository
import com.hrplatform.auth.infrastructure.persistence.twofactor.TwoFactorBackupCodeJpaRepository
import com.hrplatform.core.event.DomainEventPublisher
import com.ninjasquad.springmockk.MockkBean
import com.querydsl.jpa.impl.JPAQueryFactory
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTest(
    private val applicationContext: ApplicationContext,
) : BehaviorSpec({

    given("AuthServiceApplication") {
        `when`("Spring 컨텍스트가 로드되면") {
            then("ApplicationContext 가 null 이 아니다") {
                applicationContext shouldNotBe null
            }
        }
    }
}) {
    @MockkBean
    lateinit var jpaQueryFactory: JPAQueryFactory

    @MockkBean
    lateinit var domainEventPublisher: DomainEventPublisher

    @MockkBean
    lateinit var userAccountJpaRepository: UserAccountJpaRepository

    @MockkBean
    lateinit var roleJpaRepository: RoleJpaRepository

    @MockkBean
    lateinit var userAccountRoleJpaRepository: UserAccountRoleJpaRepository

    @MockkBean
    lateinit var refreshTokenJpaRepository: RefreshTokenJpaRepository

    @MockkBean
    lateinit var apiTokenJpaRepository: ApiTokenJpaRepository

    @MockkBean
    lateinit var twoFactorBackupCodeJpaRepository: TwoFactorBackupCodeJpaRepository

    @MockkBean
    lateinit var loginAttemptJpaRepository: LoginAttemptJpaRepository
}
