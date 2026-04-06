package com.closet.notification.domain

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * 알림 템플릿
 */
@Entity
@Table(name = "notification_template")
class NotificationTemplate(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val type: NotificationType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val channel: NotificationChannel,
    @Column(name = "title_template", nullable = false, length = 500)
    var titleTemplate: String,
    @Column(name = "content_template", nullable = false, columnDefinition = "TEXT")
    var contentTemplate: String,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
) : BaseEntity() {
    /**
     * 템플릿 변수를 치환하여 알림을 생성한다.
     * 변수 형식: {{variableName}}
     */
    fun render(
        memberId: Long,
        variables: Map<String, String>,
    ): Notification {
        var renderedTitle = titleTemplate
        var renderedContent = contentTemplate

        variables.forEach { (key, value) ->
            renderedTitle = renderedTitle.replace("{{$key}}", value)
            renderedContent = renderedContent.replace("{{$key}}", value)
        }

        return Notification.create(
            memberId = memberId,
            channel = channel,
            type = type,
            title = renderedTitle,
            content = renderedContent,
        )
    }

    /** 템플릿 수정 */
    fun update(
        titleTemplate: String,
        contentTemplate: String,
        isActive: Boolean,
    ) {
        this.titleTemplate = titleTemplate
        this.contentTemplate = contentTemplate
        this.isActive = isActive
    }
}
