package com.hrplatform.auth.infrastructure.persistence

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.support.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired

class UserAccountRepositoryImplTest(
    @Autowired private val userAccountRepository: UserAccountRepository,
) : BaseIntegrationTest() {

    init {
        given("UserAccount 저장 후") {
            val account = UserAccount.create(
                employmentId = 1001L,
                companyId = 10L,
                email = "repo-test@example.com",
                emailHash = null,
                passwordHash = "hashed-pw",
            )
            val saved = userAccountRepository.save(account)

            `when`("findByEmail 조회") {
                val found = userAccountRepository.findByEmail("repo-test@example.com")
                then("저장된 계정이 반환된다") {
                    found shouldNotBe null
                    found!!.email shouldBe "repo-test@example.com"
                    found.employmentId shouldBe 1001L
                }
            }

            `when`("findByEmploymentId 조회") {
                val found = userAccountRepository.findByEmploymentId(1001L)
                then("저장된 계정이 반환된다") {
                    found shouldNotBe null
                    found!!.id shouldBe saved.id
                }
            }

            `when`("findById 조회") {
                val found = userAccountRepository.findById(saved.id!!)
                then("저장된 계정이 반환된다") {
                    found shouldNotBe null
                }
            }
        }
    }
}
