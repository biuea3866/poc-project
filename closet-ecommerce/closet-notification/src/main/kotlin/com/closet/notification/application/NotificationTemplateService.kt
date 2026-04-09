package com.closet.notification.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationTemplate
import com.closet.notification.domain.NotificationType
import com.closet.notification.domain.repository.NotificationRepository
import com.closet.notification.domain.repository.NotificationTemplateRepository
import com.closet.notification.presentation.dto.CreateTemplateRequest
import com.closet.notification.presentation.dto.NotificationResponse
import com.closet.notification.presentation.dto.TemplateResponse
import com.closet.notification.presentation.dto.UpdateTemplateRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationTemplateService(
    private val templateRepository: NotificationTemplateRepository,
    private val notificationRepository: NotificationRepository,
) {
    /** 유형+채널로 템플릿 조회 */
    fun findByTypeAndChannel(
        type: NotificationType,
        channel: NotificationChannel,
    ): List<TemplateResponse> {
        return templateRepository
            .findByTypeAndChannelAndDeletedAtIsNull(type, channel)
            .map { TemplateResponse.from(it) }
    }

    /** 유형+채널로 활성화된 템플릿 조회 (없으면 null) */
    fun findActiveTemplate(
        type: NotificationType,
        channel: NotificationChannel,
    ): NotificationTemplate? {
        return templateRepository.findByTypeAndChannelAndIsActiveTrueAndDeletedAtIsNull(type, channel)
    }

    /** 템플릿 생성 */
    @Transactional
    fun create(request: CreateTemplateRequest): TemplateResponse {
        val template =
            NotificationTemplate(
                type = request.type,
                channel = request.channel,
                titleTemplate = request.titleTemplate,
                contentTemplate = request.contentTemplate,
            )

        val saved = templateRepository.save(template)
        return TemplateResponse.from(saved)
    }

    /** 템플릿 수정 */
    @Transactional
    fun update(
        templateId: Long,
        request: UpdateTemplateRequest,
    ): TemplateResponse {
        val template =
            templateRepository.findByIdAndDeletedAtIsNull(templateId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "템플릿을 찾을 수 없습니다")

        template.update(
            titleTemplate = request.titleTemplate,
            contentTemplate = request.contentTemplate,
            isActive = request.isActive,
        )

        return TemplateResponse.from(template)
    }

    /** 템플릿 렌더링 후 알림 발송 */
    @Transactional
    fun renderAndSend(
        type: NotificationType,
        channel: NotificationChannel,
        memberId: Long,
        variables: Map<String, String>,
    ): NotificationResponse {
        val template =
            templateRepository.findByTypeAndChannelAndIsActiveTrueAndDeletedAtIsNull(type, channel)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "활성화된 템플릿을 찾을 수 없습니다")

        val notification = template.render(memberId, variables)
        val saved = notificationRepository.save(notification)
        return NotificationResponse.from(saved)
    }
}
