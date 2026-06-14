package com.biuea.batch.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain

class NotificationTypeTest : BehaviorSpec({

    Given("각 NotificationType") {
        When("단건 본문(singleContent)을 렌더링하면") {
            Then("[U-08] 유형 라벨·groupKey·payload 가 모두 포함된다") {
                NotificationType.entries.forEach { type ->
                    val content = type.singleContent(groupKey = "key-1", payload = "본문")
                    content shouldContain type.label
                    content shouldContain "key-1"
                    content shouldContain "본문"
                }
            }
        }

        When("묶음 본문(bundledContent)을 렌더링하면") {
            Then("[U-09] 유형 라벨·groupKey·건수가 포함된다") {
                NotificationType.entries.forEach { type ->
                    val content = type.bundledContent(groupKey = "key-1", count = 7)
                    content shouldContain type.label
                    content shouldContain "key-1"
                    content shouldContain "7건"
                }
            }
        }
    }
})
