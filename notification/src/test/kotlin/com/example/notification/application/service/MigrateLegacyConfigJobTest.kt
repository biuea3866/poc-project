package com.example.notification.application.service

import com.example.notification.application.port.NotificationRuleWriter
import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.ScopeType
import com.example.notification.domain.model.NotificationRule
import com.example.notification.legacy.LegacyNotificationConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class MigrateLegacyConfigJobTest : BehaviorSpec({

    Given("시스템 디폴트와 다른 레거시 설정이 있는 경우") {
        val ruleWriter = mockk<NotificationRuleWriter>()
        val job = MigrateLegacyConfigJob(ruleWriter)

        // EMAIL=false (ORDER의 디폴트는 EMAIL=true이므로 다름)
        val legacyConfig = LegacyNotificationConfig(
            userId = 10L,
            emailEnabled = false,
            pushEnabled = true,
            smsEnabled = true,
            inAppEnabled = true,
        )

        val rulesSlot = slot<List<NotificationRule>>()
        every { ruleWriter.saveAll(capture(rulesSlot)) } answers { rulesSlot.captured }

        When("migrate 호출 (dryRun=false)") {
            val result = job.migrate(listOf(legacyConfig), storeId = 1L, dryRun = false)

            Then("정상 이관된다") {
                result.totalConfigs shouldBe 1
                result.migratedCount shouldBe 1
                result.skippedCount shouldBe 0
            }

            Then("시스템 디폴트와 다른 채널만 규칙으로 생성된다") {
                val createdRules = result.createdRules
                createdRules.all { it.scopeType == ScopeType.GLOBAL } shouldBe true
                createdRules.all { it.userId == 10L } shouldBe true

                // EMAIL이 false인데 디폴트가 true인 카테고리의 규칙만 생성
                val emailRules = createdRules.filter { it.channel == NotificationChannel.EMAIL }
                emailRules.all { !it.enabled } shouldBe true
            }

            Then("saveAll이 호출된다") {
                verify(exactly = 1) { ruleWriter.saveAll(any()) }
            }
        }
    }

    Given("시스템 디폴트와 동일한 레거시 설정인 경우") {
        val ruleWriter = mockk<NotificationRuleWriter>()
        val job = MigrateLegacyConfigJob(ruleWriter)

        When("migrate 호출") {
            // SHIPMENT 카테고리만 모든 채널(EMAIL, PUSH, SMS, IN_APP) 디폴트 ON
            // 나머지 카테고리는 일부 채널만 디폴트 ON이므로 all-true는 디폴트와 다를 수 있음
            // -> 모두 디폴트와 동일한 설정은 존재하지 않으나,
            //    개별 트리거 기준으로 디폴트와 동일한 채널은 스킵됨을 검증
            val config = LegacyNotificationConfig(
                userId = 20L,
                emailEnabled = true,
                pushEnabled = true,
                smsEnabled = true,
                inAppEnabled = true,
            )

            val rules = job.convertToRules(config, storeId = 1L)

            Then("디폴트와 동일한 채널은 스킵되고, 다른 채널만 생성된다") {
                // 디폴트에 SMS가 없는 카테고리(ORDER, PAYMENT, REVIEW)의 SMS=true 규칙 등
                rules.all { rule ->
                    val category = rule.triggerType.category
                    val defaultEnabled = category.isChannelEnabledByDefault(rule.channel)
                    rule.enabled != defaultEnabled
                } shouldBe true
            }
        }
    }

    Given("dryRun 모드") {
        val ruleWriter = mockk<NotificationRuleWriter>()
        val job = MigrateLegacyConfigJob(ruleWriter)

        val legacyConfig = LegacyNotificationConfig(
            userId = 30L,
            emailEnabled = false,
            pushEnabled = false,
            smsEnabled = true,
            inAppEnabled = true,
        )

        When("migrate 호출 (dryRun=true)") {
            val result = job.migrate(listOf(legacyConfig), storeId = 1L, dryRun = true)

            Then("이관 대상이 반환되지만 저장되지 않는다") {
                result.createdRules.isNotEmpty() shouldBe true
                verify(exactly = 0) { ruleWriter.saveAll(any()) }
            }

            Then("이관 대상 규칙이 올바르게 계산된다") {
                result.totalConfigs shouldBe 1
                result.migratedCount shouldBe 1
                result.skippedCount shouldBe 0
            }
        }
    }
})
