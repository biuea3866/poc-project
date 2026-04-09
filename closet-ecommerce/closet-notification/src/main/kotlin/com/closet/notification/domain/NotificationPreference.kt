package com.closet.notification.domain

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime

/**
 * 회원별 알림 수신 설정.
 *
 * 채널별(EMAIL/SMS/PUSH) 수신 동의, 마케팅 알림 동의, 야간 알림 동의를 관리한다.
 * 야간 알림 비동의 시 21:00~08:00(KST) 사이에는 알림을 발송하지 않는다.
 */
@Entity
@Table(name = "notification_preference")
class NotificationPreference(
    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,
    @Column(name = "email_enabled", nullable = false)
    var emailEnabled: Boolean = true,
    @Column(name = "sms_enabled", nullable = false)
    var smsEnabled: Boolean = true,
    @Column(name = "push_enabled", nullable = false)
    var pushEnabled: Boolean = true,
    @Column(name = "marketing_enabled", nullable = false)
    var marketingEnabled: Boolean = false,
    @Column(name = "night_enabled", nullable = false)
    var nightEnabled: Boolean = false,
) : BaseEntity() {
    companion object {
        private const val DND_START_HOUR = 21
        private const val DND_END_HOUR = 8

        /** 기본 설정으로 생성 (이메일/SMS/푸시 활성, 마케팅/야간 비활성) */
        fun createDefault(memberId: Long): NotificationPreference {
            return NotificationPreference(
                memberId = memberId,
                emailEnabled = true,
                smsEnabled = true,
                pushEnabled = true,
                marketingEnabled = false,
                nightEnabled = false,
            )
        }
    }

    /** 채널별 수신 설정 업데이트. null이면 기존값 유지 */
    fun updateChannelSetting(
        emailEnabled: Boolean? = null,
        smsEnabled: Boolean? = null,
        pushEnabled: Boolean? = null,
        marketingEnabled: Boolean? = null,
        nightEnabled: Boolean? = null,
    ) {
        emailEnabled?.let { this.emailEnabled = it }
        smsEnabled?.let { this.smsEnabled = it }
        pushEnabled?.let { this.pushEnabled = it }
        marketingEnabled?.let { this.marketingEnabled = it }
        nightEnabled?.let { this.nightEnabled = it }
    }

    /** 해당 채널이 활성화되어 있는지 확인 */
    fun isChannelEnabled(channel: NotificationChannel): Boolean {
        return when (channel) {
            NotificationChannel.EMAIL -> emailEnabled
            NotificationChannel.SMS -> smsEnabled
            NotificationChannel.PUSH -> pushEnabled
        }
    }

    /**
     * DND(방해금지) 시간인지 확인.
     *
     * 야간 알림이 비허용(nightEnabled=false)이고
     * 현재 시각이 21:00~08:00 사이면 DND로 판단한다.
     */
    fun isDndTime(now: ZonedDateTime): Boolean {
        if (nightEnabled) {
            return false
        }

        val hour = now.hour
        return hour >= DND_START_HOUR || hour < DND_END_HOUR
    }
}
