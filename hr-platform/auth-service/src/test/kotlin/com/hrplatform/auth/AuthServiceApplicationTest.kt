package com.hrplatform.auth

import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.login.LoginAttemptRepository
import com.hrplatform.auth.domain.role.RoleRepository
import com.hrplatform.auth.domain.role.UserAccountRoleRepository
import com.hrplatform.auth.domain.token.ApiTokenRepository
import com.hrplatform.auth.domain.token.JtiBlacklist
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCodeRepository
import com.hrplatform.core.event.DomainEventPublisher
import com.ninjasquad.springmockk.MockkBean
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.redis.core.StringRedisTemplate
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
    lateinit var userAccountRepository: UserAccountRepository

    @MockkBean
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockkBean
    lateinit var loginAttemptRepository: LoginAttemptRepository

    @MockkBean
    lateinit var roleRepository: RoleRepository

    @MockkBean
    lateinit var userAccountRoleRepository: UserAccountRoleRepository

    @MockkBean
    lateinit var twoFactorBackupCodeRepository: TwoFactorBackupCodeRepository

    @MockkBean
    lateinit var apiTokenRepository: ApiTokenRepository

    @MockkBean
    lateinit var jtiBlacklist: JtiBlacklist

    @MockkBean
    lateinit var stringRedisTemplate: StringRedisTemplate
}
