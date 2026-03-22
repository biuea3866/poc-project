package com.closet.notification.presentation.dto

import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationTemplate
import com.closet.notification.domain.NotificationType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/** 템플릿 생성 요청 */
data class CreateTemplateRequest(
    @field:NotNull(message = "알림 유형은 필수입니다")
    val type: NotificationType,

    @field:NotNull(message = "알림 채널은 필수입니다")
    val channel: NotificationChannel,

    @field:NotBlank(message = "제목 템플릿은 필수입니다")
    val titleTemplate: String,

    @field:NotBlank(message = "내용 템플릿은 필수입니다")
    val contentTemplate: String,
)

/** 템플릿 수정 요청 */
data class UpdateTemplateRequest(
    @field:NotBlank(message = "제목 템플릿은 필수입니다")
    val titleTemplate: String,

    @field:NotBlank(message = "내용 템플릿은 필수입니다")
    val contentTemplate: String,

    val isActive: Boolean = true,
)

/** 템플릿 기반 알림 발송 요청 */
data class RenderAndSendRequest(
    @field:NotNull(message = "알림 유형은 필수입니다")
    val type: NotificationType,

    @field:NotNull(message = "알림 채널은 필수입니다")
    val channel: NotificationChannel,

    @field:NotNull(message = "회원 ID는 필수입니다")
    val memberId: Long,

    val variables: Map<String, String> = emptyMap(),
)

/** 템플릿 응답 */
data class TemplateResponse(
    val id: Long,
    val type: NotificationType,
    val channel: NotificationChannel,
    val titleTemplate: String,
    val contentTemplate: String,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(template: NotificationTemplate): TemplateResponse = TemplateResponse(
            id = template.id,
            type = template.type,
            channel = template.channel,
            titleTemplate = template.titleTemplate,
            contentTemplate = template.contentTemplate,
            isActive = template.isActive,
            createdAt = template.createdAt,
            updatedAt = template.updatedAt,
        )
    }
}
