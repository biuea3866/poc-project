package com.example.notification.application.service

import com.example.notification.application.port.NotificationRuleReader
import com.example.notification.application.port.NotificationRuleWriter
import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.enums.ScopeType
import com.example.notification.domain.model.NotificationRule
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class NotificationRuleInitializerTest : BehaviorSpec({

    Given("신규 유저 (규칙 없음, 스토어 오너)") {
        val ruleReader = mockk<NotificationRuleReader>()
        val ruleWriter = mockk<NotificationRuleWriter>()
        val initializer = NotificationRuleInitializer(ruleReader, ruleWriter)

        every { ruleReader.existsByUserAndStore(10L, 1L) } returns false
        val rulesSlot = slot<List<NotificationRule>>()
        every { ruleWriter.saveAll(capture(rulesSlot)) } answers { rulesSlot.captured }

        When("initializeIfAbsent 호출") {
            initializer.initializeIfAbsent(userId = 10L, storeId = 1L, isStoreOwner = true)

            Then("GLOBAL 규칙이 생성된다") {
                val savedRules = rulesSlot.captured
                savedRules.all { it.scopeType == ScopeType.GLOBAL } shouldBe true
                savedRules.all { it.userId == 10L } shouldBe true
                savedRules.all { it.storeId == 1L } shouldBe true
            }

            Then("스토어 오너이므로 모든 규칙이 활성화된다") {
                val savedRules = rulesSlot.captured
                savedRules.all { it.enabled } shouldBe true
            }

            Then("모든 트리거 x 채널 조합이 생성된다") {
                val savedRules = rulesSlot.captured
                val expectedCount = NotificationTriggerType.entries.size * NotificationChannel.entries.size
                savedRules shouldHaveSize expectedCount
            }
        }
    }

    Given("기존 유저 (규칙 있음)") {
        val ruleReader = mockk<NotificationRuleReader>()
        val ruleWriter = mockk<NotificationRuleWriter>()
        val initializer = NotificationRuleInitializer(ruleReader, ruleWriter)

        every { ruleReader.existsByUserAndStore(10L, 1L) } returns true

        When("initializeIfAbsent 호출") {
            initializer.initializeIfAbsent(userId = 10L, storeId = 1L, isStoreOwner = true)

            Then("규칙 생성이 스킵된다") {
                verify(exactly = 0) { ruleWriter.saveAll(any()) }
            }
        }
    }

    Given("스토어 오너 PRODUCT 스코프") {
        val ruleReader = mockk<NotificationRuleReader>()
        val ruleWriter = mockk<NotificationRuleWriter>()
        val initializer = NotificationRuleInitializer(ruleReader, ruleWriter)

        val rulesSlot = slot<List<NotificationRule>>()
        every { ruleWriter.saveAll(capture(rulesSlot)) } answers { rulesSlot.captured }

        When("initializeProcessRules 호출 (isStoreOwner=true)") {
            initializer.initializeProcessRules(userId = 10L, storeId = 1L, productId = 100L, isStoreOwner = true)

            Then("PRODUCT 스코프 규칙이 생성된다") {
                val savedRules = rulesSlot.captured
                savedRules.all { it.scopeType == ScopeType.PRODUCT } shouldBe true
                savedRules.all { it.scopeId == 100L } shouldBe true
            }

            Then("모든 상품 알림이 활성화된다") {
                val savedRules = rulesSlot.captured
                savedRules.all { it.enabled } shouldBe true
            }

            Then("PRODUCT 스코프를 지원하는 카테고리의 트리거만 생성된다") {
                val savedRules = rulesSlot.captured
                val productTriggers = NotificationTriggerType.entries
                    .filter { it.category.supportsScopeType(ScopeType.PRODUCT) }
                val expectedCount = productTriggers.size * NotificationChannel.entries.size
                savedRules shouldHaveSize expectedCount
            }
        }
    }

    Given("비오너 PRODUCT 스코프") {
        val ruleReader = mockk<NotificationRuleReader>()
        val ruleWriter = mockk<NotificationRuleWriter>()
        val initializer = NotificationRuleInitializer(ruleReader, ruleWriter)

        val rulesSlot = slot<List<NotificationRule>>()
        every { ruleWriter.saveAll(capture(rulesSlot)) } answers { rulesSlot.captured }

        When("initializeProcessRules 호출 (isStoreOwner=false)") {
            initializer.initializeProcessRules(userId = 20L, storeId = 1L, productId = 200L, isStoreOwner = false)

            Then("모든 규칙이 비활성화된다") {
                val savedRules = rulesSlot.captured
                savedRules.all { !it.enabled } shouldBe true
            }
        }
    }
})
