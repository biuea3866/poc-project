package com.hrplatform.employee

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class EmployeeServiceApplicationTest(
    private val applicationContext: ApplicationContext,
) : BehaviorSpec({

    given("EmployeeServiceApplication") {
        `when`("Spring 컨텍스트가 로드되면") {
            then("ApplicationContext 가 null 이 아니다") {
                applicationContext shouldNotBe null
            }
        }
    }
})
