package com.closet.common.featureflag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class FeatureFlagServiceTest : BehaviorSpec({

    val repository = mockk<SimpleRuntimeConfigRepository>()
    val service = FeatureFlagService(repository)

    // save() 기본 stub: 입력값을 그대로 반환
    every { repository.save(any<SimpleRuntimeConfig>()) } answers { firstArg() }

    Given("Feature Flag가 DB에 존재할 때") {

        val config = SimpleRuntimeConfig(
            configKey = "SEARCH_INDEXING_ENABLED",
            configValue = "true",
            description = "검색 인덱싱 활성화",
        )

        every { repository.findByConfigKey("SEARCH_INDEXING_ENABLED") } returns Optional.of(config)

        When("isEnabled를 호출하면") {
            val result = service.isEnabled(Phase2FeatureKey.SEARCH_INDEXING_ENABLED)

            Then("DB 값을 기반으로 true를 반환한다") {
                result shouldBe true
            }
        }
    }

    Given("Feature Flag가 DB에 OFF로 존재할 때") {

        val config = SimpleRuntimeConfig(
            configKey = "INVENTORY_KAFKA_ENABLED",
            configValue = "false",
            description = "재고 Kafka 비활성화",
        )

        every { repository.findByConfigKey("INVENTORY_KAFKA_ENABLED") } returns Optional.of(config)

        When("isEnabled를 호출하면") {
            val result = service.isEnabled(Phase2FeatureKey.INVENTORY_KAFKA_ENABLED)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }
    }

    Given("Feature Flag가 DB에 존재하지 않을 때") {

        every { repository.findByConfigKey("POPULAR_KEYWORDS_ENABLED") } returns Optional.empty()

        When("isEnabled를 호출하면") {
            val result = service.isEnabled(Phase2FeatureKey.POPULAR_KEYWORDS_ENABLED)

            Then("기본값(false)을 반환한다") {
                result shouldBe false
            }
        }
    }

    Given("Feature Flag를 ON으로 변경할 때") {

        val config = SimpleRuntimeConfig(
            configKey = "SHIPPING_SERVICE_ENABLED",
            configValue = "false",
            description = "배송 서비스",
        )

        every { repository.findByConfigKey("SHIPPING_SERVICE_ENABLED") } returns Optional.of(config)

        When("setEnabled(true)를 호출하면") {
            service.setEnabled(Phase2FeatureKey.SHIPPING_SERVICE_ENABLED, true)

            Then("DB 값이 true로 업데이트된다") {
                config.configValue shouldBe "true"
                verify(exactly = 1) { repository.save(config) }
            }
        }
    }

    Given("DB에 없는 Feature Flag를 ON으로 변경할 때") {

        every { repository.findByConfigKey("EXCHANGE_REQUEST_ENABLED") } returns Optional.empty()

        When("setEnabled(true)를 호출하면") {
            service.setEnabled(Phase2FeatureKey.EXCHANGE_REQUEST_ENABLED, true)

            Then("새로운 Config가 생성된다") {
                verify(exactly = 1) {
                    repository.save(match {
                        it.configKey == "EXCHANGE_REQUEST_ENABLED" && it.configValue == "true"
                    })
                }
            }
        }
    }

    Given("Phase2FeatureKey enum") {

        When("전체 키를 조회하면") {
            val allKeys = Phase2FeatureKey.entries

            Then("13개의 Feature Flag가 정의되어 있다") {
                allKeys.size shouldBe 13
            }
        }

        When("키 문자열로 조회하면") {
            val key = Phase2FeatureKey.fromKey("SEARCH_INDEXING_ENABLED")

            Then("해당 enum 값이 반환된다") {
                key shouldBe Phase2FeatureKey.SEARCH_INDEXING_ENABLED
            }
        }

        When("존재하지 않는 키 문자열로 조회하면") {
            val key = Phase2FeatureKey.fromKey("NON_EXISTENT_KEY")

            Then("null이 반환된다") {
                key shouldBe null
            }
        }
    }
})
