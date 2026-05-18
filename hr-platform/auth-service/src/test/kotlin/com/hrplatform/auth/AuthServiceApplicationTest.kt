package com.hrplatform.auth

import com.hrplatform.auth.infrastructure.config.QueryDslConfig
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
}
