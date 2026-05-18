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
            val testEmail = "repo-test@example.com"
            val testEmailHash = "a".repeat(64)
            val account = UserAccount.create(
                employmentId = 1001L,
                companyId = 10L,
                email = testEmail,
                emailHash = testEmailHash,
                passwordHash = "hashed-pw",
            )
            val saved = userAccountRepository.save(account)

            `when`("findByEmailHash 조회") {
                val found = userAccountRepository.findByEmailHash(testEmailHash)
                then("저장된 계정이 반환된다") {
                    found shouldNotBe null
                    found!!.emailHash shouldBe testEmailHash
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
