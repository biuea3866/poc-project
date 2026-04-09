package com.closet.notification.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationPreferenceTest : BehaviorSpec({

    Given("NotificationPreference 기본 생성 시") {
        When("회원 ID만으로 생성하면") {
            val preference = NotificationPreference.createDefault(memberId = 1L)

            Then("이메일, SMS, 푸시가 모두 활성화된다") {
                preference.memberId shouldBe 1L
                preference.emailEnabled shouldBe true
                preference.smsEnabled shouldBe true
                preference.pushEnabled shouldBe true
            }

            Then("마케팅 알림은 비활성화된다") {
                preference.marketingEnabled shouldBe false
            }

            Then("야간 알림은 비활성화된다") {
                preference.nightEnabled shouldBe false
            }
        }
    }

    Given("특정 채널을 옵트아웃 할 때") {
        val preference = NotificationPreference.createDefault(memberId = 1L)

        When("이메일 채널을 비활성화하면") {
            preference.updateChannelSetting(emailEnabled = false)

            Then("이메일만 비활성화되고 나머지는 유지된다") {
                preference.emailEnabled shouldBe false
                preference.smsEnabled shouldBe true
                preference.pushEnabled shouldBe true
            }
        }

        When("SMS 채널을 비활성화하면") {
            preference.updateChannelSetting(smsEnabled = false)

            Then("SMS만 비활성화된다") {
                preference.smsEnabled shouldBe false
            }
        }

        When("PUSH 채널을 비활성화하면") {
            preference.updateChannelSetting(pushEnabled = false)

            Then("PUSH만 비활성화된다") {
                preference.pushEnabled shouldBe false
            }
        }
    }

    Given("채널 활성화 여부를 NotificationChannel로 확인할 때") {
        val preference = NotificationPreference.createDefault(memberId = 1L)

        When("모든 채널이 활성화된 상태에서 EMAIL 채널을 확인하면") {
            val result = preference.isChannelEnabled(NotificationChannel.EMAIL)

            Then("true를 반환한다") {
                result shouldBe true
            }
        }

        When("EMAIL 채널을 비활성화 후 확인하면") {
            preference.updateChannelSetting(emailEnabled = false)
            val result = preference.isChannelEnabled(NotificationChannel.EMAIL)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }

        When("SMS 채널을 비활성화 후 확인하면") {
            preference.updateChannelSetting(smsEnabled = false)
            val result = preference.isChannelEnabled(NotificationChannel.SMS)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }

        When("PUSH 채널을 비활성화 후 확인하면") {
            preference.updateChannelSetting(pushEnabled = false)
            val result = preference.isChannelEnabled(NotificationChannel.PUSH)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }
    }

    Given("DND(방해금지) 시간 확인 시") {
        When("야간 알림이 허용된 회원이면") {
            val preference = NotificationPreference.createDefault(memberId = 1L)
            preference.updateChannelSetting(nightEnabled = true)

            val nightTime = ZonedDateTime.of(2026, 4, 6, 23, 0, 0, 0, ZoneId.of("Asia/Seoul"))
            val result = preference.isDndTime(nightTime)

            Then("DND가 아니다 (야간에도 발송 가능)") {
                result shouldBe false
            }
        }

        When("야간 알림이 비허용이고 21시~08시 사이면") {
            val preference = NotificationPreference.createDefault(memberId = 1L)
            // nightEnabled = false (기본값)

            val nightTime = ZonedDateTime.of(2026, 4, 6, 23, 0, 0, 0, ZoneId.of("Asia/Seoul"))
            val result = preference.isDndTime(nightTime)

            Then("DND이다 (발송 차단)") {
                result shouldBe true
            }
        }

        When("야간 알림이 비허용이고 08시~21시 사이면") {
            val preference = NotificationPreference.createDefault(memberId = 1L)

            val dayTime = ZonedDateTime.of(2026, 4, 6, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))
            val result = preference.isDndTime(dayTime)

            Then("DND가 아니다 (낮시간이므로 발송 가능)") {
                result shouldBe false
            }
        }

        When("야간 알림이 비허용이고 정확히 21시면") {
            val preference = NotificationPreference.createDefault(memberId = 1L)

            val exactNine = ZonedDateTime.of(2026, 4, 6, 21, 0, 0, 0, ZoneId.of("Asia/Seoul"))
            val result = preference.isDndTime(exactNine)

            Then("DND이다 (21시부터 차단 시작)") {
                result shouldBe true
            }
        }

        When("야간 알림이 비허용이고 정확히 08시면") {
            val preference = NotificationPreference.createDefault(memberId = 1L)

            val exactEight = ZonedDateTime.of(2026, 4, 6, 8, 0, 0, 0, ZoneId.of("Asia/Seoul"))
            val result = preference.isDndTime(exactEight)

            Then("DND가 아니다 (08시부터 발송 가능)") {
                result shouldBe false
            }
        }
    }

    Given("마케팅 알림 동의 변경 시") {
        val preference = NotificationPreference.createDefault(memberId = 1L)

        When("마케팅 알림을 활성화하면") {
            preference.updateChannelSetting(marketingEnabled = true)

            Then("마케팅 알림이 활성화된다") {
                preference.marketingEnabled shouldBe true
            }
        }

        When("마케팅 알림을 비활성화하면") {
            preference.updateChannelSetting(marketingEnabled = false)

            Then("마케팅 알림이 비활성화된다") {
                preference.marketingEnabled shouldBe false
            }
        }
    }

    Given("전체 설정 업데이트 시") {
        val preference = NotificationPreference.createDefault(memberId = 1L)

        When("모든 설정을 한번에 변경하면") {
            preference.updateChannelSetting(
                emailEnabled = false,
                smsEnabled = false,
                pushEnabled = true,
                marketingEnabled = true,
                nightEnabled = true,
            )

            Then("모든 설정이 반영된다") {
                preference.emailEnabled shouldBe false
                preference.smsEnabled shouldBe false
                preference.pushEnabled shouldBe true
                preference.marketingEnabled shouldBe true
                preference.nightEnabled shouldBe true
            }
        }
    }
})
