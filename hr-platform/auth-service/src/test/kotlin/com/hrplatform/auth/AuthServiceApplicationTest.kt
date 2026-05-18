package com.hrplatform.auth

<<<<<<< HEAD
import com.hrplatform.auth.infrastructure.persistence.account.UserAccountJpaRepository
import com.hrplatform.auth.infrastructure.persistence.login.LoginAttemptJpaRepository
import com.hrplatform.auth.infrastructure.persistence.role.RoleJpaRepository
import com.hrplatform.auth.infrastructure.persistence.role.UserAccountRoleJpaRepository
import com.hrplatform.auth.infrastructure.persistence.token.ApiTokenJpaRepository
import com.hrplatform.auth.infrastructure.persistence.token.RefreshTokenJpaRepository
import com.hrplatform.auth.infrastructure.persistence.twofactor.TwoFactorBackupCodeJpaRepository
=======
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.login.LoginAttemptRepository
import com.hrplatform.auth.domain.role.RoleRepository
import com.hrplatform.auth.domain.role.UserAccountRoleRepository
import com.hrplatform.auth.domain.token.ApiTokenRepository
import com.hrplatform.auth.domain.token.JtiBlacklist
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCodeRepository
>>>>>>> 6676c395 (feat(auth): Wave 4-5 — AT-EVT-PUB + AT-SVC-* + AT-WKR)
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
<<<<<<< HEAD
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
=======
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
>>>>>>> 6676c395 (feat(auth): Wave 4-5 — AT-EVT-PUB + AT-SVC-* + AT-WKR)
}
