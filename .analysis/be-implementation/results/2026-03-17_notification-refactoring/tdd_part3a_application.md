# [알림 시스템 리팩토링] Part 3a - Application 모듈 코드 설계

> 작성일: 2026-03-17
> 상위 문서: [Part 1 - ERD & Domain](tdd_part1_erd_domain.md), [Part 2 - Architecture & API](tdd_part2_architecture_api.md)
> 원칙: domain 모듈만 의존. Spring 의존 없음. 순수 Kotlin.

---

## 1. build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":notification-domain"))

    // 테스트
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.10")
}
```

---

## 2. Port In - UseCase 인터페이스 (9개)

```kotlin
package com.doodlin.greeting.notification.application.port.`in`

// ── 알림 생성 ──
interface CreateNotificationUseCase {
    fun create(command: CreateNotificationCommand): Notification
}

// ── 알림 조회 ──
interface QueryNotificationUseCase {
    fun getList(query: NotificationListQuery): NotificationPage
    fun getUnreadCount(userId: Long, workspaceId: Long): UnreadCount
}

// ── 읽음 처리 ──
interface ReadNotificationUseCase {
    fun markAsRead(id: Long, userId: Long)
    fun markAllAsRead(userId: Long, workspaceId: Long, type: NotificationType?)
}

// ── 개인 설정 ──
interface ManageSettingUseCase {
    fun getSetting(workspaceId: Long, userId: Long, type: NotificationType): ResolvedSettingResult
    fun updateSetting(command: UpsertSettingCommand)
}

// ── 관리자 설정 ──
interface AdminSettingUseCase {
    fun getWorkspaceSetting(workspaceId: Long, type: NotificationType): NotificationSetting
    fun upsertWorkspaceSetting(command: UpsertSettingCommand)
    fun upsertOverride(command: UpsertOverrideCommand)
    fun removeOverride(workspaceId: Long, settingId: Long, targetUserId: Long)
}

// ── 구독 관리 ──
interface ManageSubscriptionUseCase {
    fun getSubscriptions(workspaceId: Long, userId: Long): List<NotificationSubscription>
    fun subscribe(command: SubscribeCommand)
    fun unsubscribe(workspaceId: Long, userId: Long, type: NotificationType, scope: SettingScope, scopeRefId: Long?)
}

// ── 템플릿 관리 ──
interface ManageTemplateUseCase {
    fun getTemplate(id: Long): NotificationTemplate
    fun listTemplates(workspaceId: Long?, type: NotificationType?, channelType: ChannelType?): List<NotificationTemplate>
    fun upsert(command: UpsertTemplateCommand): NotificationTemplate
    fun delete(id: Long)
}

// ── 리마인드 스케줄 등록 ──
interface ScheduleRemindUseCase {
    fun register(command: RegisterScheduleCommand): NotificationSchedule
    fun cancel(command: CancelScheduleCommand)
    fun cancelBySource(sourceType: String, sourceId: Long)
    fun reschedule(scheduleId: Long, newScheduledAt: LocalDateTime)
}

// ── 스케줄 실행 (배치) ──
interface ExecuteScheduleUseCase {
    fun triggerDueSchedules(now: LocalDateTime, batchSize: Int): Int
}
```

---

## 3. Port Out - Repository 인터페이스 (7개)

```kotlin
package com.doodlin.greeting.notification.application.port.out

// ── 알림 저장/조회 ──
interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun findById(id: Long): Notification?
    fun findByUserIdAndWorkspaceId(
        userId: Long, workspaceId: Long,
        type: NotificationType?, category: NotificationCategory?,
        limit: Int, offset: Int
    ): List<Notification>
    fun countByUserIdAndWorkspaceId(
        userId: Long, workspaceId: Long,
        type: NotificationType?, category: NotificationCategory?
    ): Long
    fun countUnread(userId: Long, workspaceId: Long): UnreadCount
    fun markAsRead(id: Long, userId: Long)
    fun markAllAsRead(userId: Long, workspaceId: Long, type: NotificationType?)
}

// ── 설정 저장/조회 ──
interface NotificationSettingRepository {
    fun findByWorkspaceAndType(
        workspaceId: Long, type: NotificationType, scope: SettingScope, scopeRefId: Long?
    ): NotificationSetting?
    fun findAllByWorkspaceAndType(workspaceId: Long, type: NotificationType): List<NotificationSetting>
    fun save(setting: NotificationSetting): NotificationSetting
    fun delete(id: Long)
}

// ── 오버라이드 저장/조회 ──
interface SettingOverrideRepository {
    fun findBySettingIdAndUserId(settingId: Long, userId: Long, type: OverrideType): NotificationSettingOverride?
    fun findAllBySettingIdAndUserId(settingId: Long, userId: Long): List<NotificationSettingOverride>
    fun save(override: NotificationSettingOverride): NotificationSettingOverride
    fun delete(id: Long)
}

// ── 구독 저장/조회 ──
interface SubscriptionRepository {
    fun findByUserAndType(
        workspaceId: Long, userId: Long, type: NotificationType, scope: SettingScope, scopeRefId: Long?
    ): NotificationSubscription?
    fun findAllByUser(workspaceId: Long, userId: Long): List<NotificationSubscription>
    fun save(subscription: NotificationSubscription): NotificationSubscription
    fun delete(id: Long)
}

// ── 템플릿 저장/조회 ──
interface TemplateRepository {
    fun findById(id: Long): NotificationTemplate?
    fun findByTypeAndChannel(
        workspaceId: Long?, type: NotificationType, channelType: ChannelType, locale: String
    ): NotificationTemplate?
    fun findAll(workspaceId: Long?, type: NotificationType?, channelType: ChannelType?): List<NotificationTemplate>
    fun save(template: NotificationTemplate): NotificationTemplate
    fun delete(id: Long)
}

// ── 스케줄 저장/조회 ──
interface ScheduleRepository {
    fun findById(id: Long): NotificationSchedule?
    fun findDueSchedules(now: LocalDateTime, batchSize: Int): List<NotificationSchedule>
    fun findBySourceAndStatus(sourceType: String, sourceId: Long, status: ScheduleStatus): List<NotificationSchedule>
    fun save(schedule: NotificationSchedule): NotificationSchedule
    fun saveAll(schedules: List<NotificationSchedule>)
}

// ── 멱등성 이벤트 ──
interface ProcessedEventRepository {
    fun existsByEventId(eventId: String): Boolean
    fun save(eventId: String, eventType: String)
}
```

---

## 4. Port Out - 외부 인터페이스

```kotlin
package com.doodlin.greeting.notification.application.port.out

// ── 채널별 발송 ──
interface NotificationChannelSender {
    fun supports(channel: ChannelType): Boolean
    fun send(notification: Notification, recipient: NotificationRecipient): SendResult
    fun priority(): Int = 100
}

// ── 도메인 이벤트 발행 ──
interface DomainEventPublisher {
    fun publish(event: NotificationDomainEvent)
    fun publishAsync(event: NotificationDomainEvent)
}

// ── 템플릿 렌더링 ──
interface TemplateRenderer {
    fun render(template: NotificationTemplate, variables: Map<String, Any>): RenderedContent
    fun validate(templateBody: String, variableSchemaJson: String): ValidationResult
}

// ── 외부 사용자 조회 ──
interface UserQueryPort {
    fun findByIds(userIds: List<Long>): List<NotificationRecipient>
    fun findByWorkspaceAndRoles(workspaceId: Long, roleIds: List<Long>): List<NotificationRecipient>
}

// ── 외부 워크스페이스 조회 ──
interface WorkspaceQueryPort {
    fun findById(workspaceId: Long): WorkspaceInfo?
}
```

---

## 5. Command / Query DTO

```kotlin
package com.doodlin.greeting.notification.application.dto.command

data class CreateNotificationCommand(
    val workspaceId: Long,
    val recipientUserIds: List<Long>,
    val notificationType: NotificationType,
    val category: NotificationCategory,
    val priority: NotificationPriority,
    val title: String,
    val body: String?,
    val sourceType: String?,
    val sourceId: Long?,
    val openingId: Long?,
    val processOnOpeningId: Long?,
    val actionUrl: String?,
    val details: Map<String, String>,
    val templateId: Long?,
    val templateVariables: Map<String, String>,
    val channels: List<ChannelType>?,       // null이면 설정 resolve
    val eventId: String                      // 멱등성 키
)

data class UpsertSettingCommand(
    val workspaceId: Long,
    val notificationType: NotificationType,
    val scope: SettingScope,
    val scopeRefId: Long?,
    val channelPreference: ChannelPreference,
    val operatorUserId: Long
)

data class UpsertOverrideCommand(
    val workspaceId: Long,
    val settingId: Long,
    val targetUserId: Long,
    val overriddenByUserId: Long,
    val overrideType: OverrideType,
    val channelPreference: ChannelPreference,
    val forceDisabled: Boolean
)

data class SubscribeCommand(
    val workspaceId: Long,
    val userId: Long,
    val notificationType: NotificationType,
    val scope: SettingScope,
    val scopeRefId: Long?,
    val subscribed: Boolean
)

data class UpsertTemplateCommand(
    val id: Long?,                           // null이면 신규
    val workspaceId: Long?,
    val notificationType: NotificationType,
    val channelType: ChannelType,
    val locale: String,
    val subjectTemplate: String?,
    val bodyTemplate: String,
    val variableSchemaJson: String,
    val isDefault: Boolean
)

data class RegisterScheduleCommand(
    val workspaceId: Long,
    val scheduleType: ScheduleType,
    val sourceId: Long,
    val sourceType: String,
    val targetUserId: Long,
    val templateId: Long?,
    val scheduledAt: LocalDateTime,
    val idempotencyKey: String,
    val payloadJson: String?
)

data class CancelScheduleCommand(
    val scheduleId: Long,
    val reason: String?
)
```

```kotlin
package com.doodlin.greeting.notification.application.dto.query

data class NotificationListQuery(
    val userId: Long,
    val workspaceId: Long,
    val type: NotificationType?,
    val category: NotificationCategory?,
    val limit: Int = 20,
    val offset: Int = 0
)
```

```kotlin
package com.doodlin.greeting.notification.application.dto.result

data class NotificationPage(
    val items: List<Notification>,
    val totalCount: Long,
    val hasNext: Boolean
)

data class UnreadCount(
    val count: Long
)

data class ResolvedSettingResult(
    val workspaceId: Long,
    val userId: Long,
    val notificationType: NotificationType,
    val channelPreference: ChannelPreference,
    val resolvedFrom: String                  // 어느 레벨에서 최종 결정되었는지 (디버그용)
)

data class RenderedContent(
    val subject: String?,
    val body: String
)

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String>
)

data class SendResult(
    val success: Boolean,
    val channel: ChannelType,
    val sentAt: Instant?,
    val errorCode: String?,
    val errorMessage: String?
)
```

---

## 6. 핵심 Service 구현체 (4개)

### 6.1 NotificationService

```kotlin
package com.doodlin.greeting.notification.application.service

class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val settingResolver: NotificationSettingResolver,
    private val channelSenders: List<NotificationChannelSender>,
    private val userQueryPort: UserQueryPort,
    private val processedEventRepository: ProcessedEventRepository,
    private val eventPublisher: DomainEventPublisher
) : CreateNotificationUseCase, QueryNotificationUseCase, ReadNotificationUseCase {

    // ── CreateNotificationUseCase ──
    override fun create(command: CreateNotificationCommand): Notification {
        // 1. 멱등성 체크
        if (processedEventRepository.existsByEventId(command.eventId)) {
            throw DuplicateEventException(command.eventId)
        }

        // 2. 수신자 조회
        val recipients = userQueryPort.findByIds(command.recipientUserIds)

        // 3. 수신자별 알림 생성 + 채널 발송
        val notifications = recipients.map { recipient ->
            // 3a. 설정 resolve (채널 명시 없으면)
            val enabledChannels = command.channels ?: resolveChannels(
                command.workspaceId, recipient.userId,
                command.notificationType, command.openingId, command.processOnOpeningId
            )

            if (enabledChannels.isEmpty()) return@map null  // 모든 채널 비활성 -> skip

            // 3b. 도메인 객체 생성
            val notification = NotificationFactory.create(
                workspaceId = command.workspaceId,
                recipientUserId = recipient.userId,
                type = command.notificationType,
                category = command.category,
                priority = command.priority,
                title = command.title,
                body = command.body,
                source = command.sourceType?.let { SourceReference(it, command.sourceId!!) },
                openingId = command.openingId,
                processOnOpeningId = command.processOnOpeningId,
                actionUrl = command.actionUrl,
                details = command.details
            )

            // 3c. 저장
            val saved = notificationRepository.save(notification)

            // 3d. 채널별 발송
            channelSenders
                .filter { sender -> enabledChannels.any { sender.supports(it) } }
                .sortedBy { it.priority() }
                .forEach { sender ->
                    runCatching { sender.send(saved, recipient) }
                        .onFailure { /* 로깅, 개별 채널 실패가 다른 채널에 영향 없음 */ }
                }

            // 3e. 도메인 이벤트 발행
            eventPublisher.publishAsync(NotificationCreatedEvent(saved.id, saved.workspaceId, recipient.userId))

            saved
        }.filterNotNull()

        // 4. 멱등성 키 저장
        processedEventRepository.save(command.eventId, command.notificationType.name)

        return notifications.first()
    }

    private fun resolveChannels(
        workspaceId: Long, userId: Long, type: NotificationType,
        openingId: Long?, processOnOpeningId: Long?
    ): List<ChannelType> {
        val resolved = settingResolver.resolve(workspaceId, userId, type, openingId, processOnOpeningId)
        return resolved.channelPreference.enabledChannels()
    }

    // ── QueryNotificationUseCase ──
    override fun getList(query: NotificationListQuery): NotificationPage {
        val items = notificationRepository.findByUserIdAndWorkspaceId(
            query.userId, query.workspaceId, query.type, query.category, query.limit, query.offset
        )
        val totalCount = notificationRepository.countByUserIdAndWorkspaceId(
            query.userId, query.workspaceId, query.type, query.category
        )
        return NotificationPage(items, totalCount, hasNext = (query.offset + query.limit) < totalCount)
    }

    override fun getUnreadCount(userId: Long, workspaceId: Long): UnreadCount {
        return notificationRepository.countUnread(userId, workspaceId)
    }

    // ── ReadNotificationUseCase ──
    override fun markAsRead(id: Long, userId: Long) {
        notificationRepository.markAsRead(id, userId)
        eventPublisher.publishAsync(NotificationReadEvent(id, userId))
    }

    override fun markAllAsRead(userId: Long, workspaceId: Long, type: NotificationType?) {
        notificationRepository.markAllAsRead(userId, workspaceId, type)
    }
}
```

### 6.2 NotificationSettingService

```kotlin
class NotificationSettingService(
    private val settingRepository: NotificationSettingRepository,
    private val overrideRepository: SettingOverrideRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val settingResolver: NotificationSettingResolver
) : ManageSettingUseCase, AdminSettingUseCase {

    // ── ManageSettingUseCase (개인 설정) ──
    override fun getSetting(workspaceId: Long, userId: Long, type: NotificationType): ResolvedSettingResult {
        return settingResolver.resolve(workspaceId, userId, type, openingId = null, processOnOpeningId = null)
    }

    override fun updateSetting(command: UpsertSettingCommand) {
        // 개인 설정 = PERSONAL override로 저장
        val setting = settingRepository.findByWorkspaceAndType(
            command.workspaceId, command.notificationType, command.scope, command.scopeRefId
        ) ?: settingRepository.save(
            NotificationSetting.createDefault(command.workspaceId, command.notificationType, command.scope, command.scopeRefId)
        )

        val existing = overrideRepository.findBySettingIdAndUserId(setting.id, command.operatorUserId, OverrideType.PERSONAL)
        if (existing != null) {
            existing.updateChannelPreference(command.channelPreference)
            overrideRepository.save(existing)
        } else {
            overrideRepository.save(
                NotificationSettingOverride.createPersonal(
                    settingId = setting.id,
                    targetUserId = command.operatorUserId,
                    channelPreference = command.channelPreference
                )
            )
        }
    }

    // ── AdminSettingUseCase (관리자 설정) ──
    override fun getWorkspaceSetting(workspaceId: Long, type: NotificationType): NotificationSetting {
        return settingRepository.findByWorkspaceAndType(workspaceId, type, SettingScope.WORKSPACE, null)
            ?: NotificationSetting.systemDefault(workspaceId, type)
    }

    override fun upsertWorkspaceSetting(command: UpsertSettingCommand) {
        val existing = settingRepository.findByWorkspaceAndType(
            command.workspaceId, command.notificationType, command.scope, command.scopeRefId
        )
        if (existing != null) {
            existing.updateChannelPreference(command.channelPreference)
            settingRepository.save(existing)
        } else {
            settingRepository.save(
                NotificationSetting(
                    workspaceId = command.workspaceId,
                    notificationType = command.notificationType,
                    scope = command.scope,
                    scopeRefId = command.scopeRefId,
                    channelPreference = command.channelPreference
                )
            )
        }
    }

    override fun upsertOverride(command: UpsertOverrideCommand) {
        val existing = overrideRepository.findBySettingIdAndUserId(
            command.settingId, command.targetUserId, command.overrideType
        )
        if (existing != null) {
            existing.updateChannelPreference(command.channelPreference)
            existing.setForceDisabled(command.forceDisabled)
            overrideRepository.save(existing)
        } else {
            overrideRepository.save(
                NotificationSettingOverride(
                    notificationSettingId = command.settingId,
                    targetUserId = command.targetUserId,
                    overriddenByUserId = command.overriddenByUserId,
                    overrideType = command.overrideType,
                    channelPreference = command.channelPreference,
                    forceDisabled = command.forceDisabled
                )
            )
        }
    }

    override fun removeOverride(workspaceId: Long, settingId: Long, targetUserId: Long) {
        val overrides = overrideRepository.findAllBySettingIdAndUserId(settingId, targetUserId)
        overrides.filter { it.overrideType == OverrideType.ADMIN_FORCE }.forEach {
            overrideRepository.delete(it.id)
        }
    }
}
```

### 6.3 RemindScheduleService

```kotlin
class RemindScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val processedEventRepository: ProcessedEventRepository,
    private val createNotificationUseCase: CreateNotificationUseCase,
    private val eventPublisher: DomainEventPublisher
) : ScheduleRemindUseCase, ExecuteScheduleUseCase {

    // ── ScheduleRemindUseCase ──
    override fun register(command: RegisterScheduleCommand): NotificationSchedule {
        val schedule = NotificationSchedule(
            workspaceId = command.workspaceId,
            scheduleType = command.scheduleType,
            sourceId = command.sourceId,
            sourceType = command.sourceType,
            targetUserId = command.targetUserId,
            templateId = command.templateId,
            status = ScheduleStatus.PENDING,
            scheduledAt = command.scheduledAt,
            idempotencyKey = command.idempotencyKey,
            payloadJson = command.payloadJson
        )
        return scheduleRepository.save(schedule)
    }

    override fun cancel(command: CancelScheduleCommand) {
        val schedule = scheduleRepository.findById(command.scheduleId)
            ?: throw ScheduleNotFoundException(command.scheduleId)
        schedule.cancel()
        scheduleRepository.save(schedule)
        eventPublisher.publishAsync(ScheduleCancelledEvent(schedule.id, schedule.workspaceId))
    }

    override fun cancelBySource(sourceType: String, sourceId: Long) {
        val pendingSchedules = scheduleRepository.findBySourceAndStatus(sourceType, sourceId, ScheduleStatus.PENDING)
        pendingSchedules.forEach { it.cancel() }
        scheduleRepository.saveAll(pendingSchedules)
    }

    override fun reschedule(scheduleId: Long, newScheduledAt: LocalDateTime) {
        val schedule = scheduleRepository.findById(scheduleId)
            ?: throw ScheduleNotFoundException(scheduleId)
        schedule.reschedule(newScheduledAt)
        scheduleRepository.save(schedule)
    }

    // ── ExecuteScheduleUseCase ──
    override fun triggerDueSchedules(now: LocalDateTime, batchSize: Int): Int {
        val dueSchedules = scheduleRepository.findDueSchedules(now, batchSize)
        var triggered = 0

        dueSchedules.forEach { schedule ->
            // 멱등성 체크
            if (processedEventRepository.existsByEventId(schedule.idempotencyKey)) {
                schedule.markAsSent()
                scheduleRepository.save(schedule)
                return@forEach
            }

            // 트리거
            schedule.trigger()

            runCatching {
                // 알림 생성 (CreateNotificationUseCase 재사용)
                createNotificationUseCase.create(
                    CreateNotificationCommand(
                        workspaceId = schedule.workspaceId,
                        recipientUserIds = listOf(schedule.targetUserId),
                        notificationType = schedule.scheduleType.toNotificationType(),
                        category = schedule.scheduleType.toCategory(),
                        priority = NotificationPriority.NORMAL,
                        title = "",  // 템플릿에서 렌더링
                        body = null,
                        sourceType = schedule.sourceType,
                        sourceId = schedule.sourceId,
                        openingId = null,
                        processOnOpeningId = null,
                        actionUrl = null,
                        details = emptyMap(),
                        templateId = schedule.templateId,
                        templateVariables = schedule.parsePayload(),
                        channels = null,
                        eventId = schedule.idempotencyKey
                    )
                )
                schedule.markAsSent()
                triggered++
            }.onFailure {
                schedule.markAsFailed()
            }

            scheduleRepository.save(schedule)
            eventPublisher.publishAsync(ScheduleTriggeredEvent(schedule.id, schedule.workspaceId))
        }

        return triggered
    }
}
```

### 6.4 SubscriptionService

```kotlin
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val settingResolver: NotificationSettingResolver,
    private val userQueryPort: UserQueryPort
) : ManageSubscriptionUseCase {

    override fun getSubscriptions(workspaceId: Long, userId: Long): List<NotificationSubscription> {
        return subscriptionRepository.findAllByUser(workspaceId, userId)
    }

    override fun subscribe(command: SubscribeCommand) {
        val existing = subscriptionRepository.findByUserAndType(
            command.workspaceId, command.userId, command.notificationType, command.scope, command.scopeRefId
        )
        if (existing != null) {
            if (command.subscribed) existing.resubscribe() else existing.unsubscribe()
            subscriptionRepository.save(existing)
        } else {
            subscriptionRepository.save(
                NotificationSubscription(
                    workspaceId = command.workspaceId,
                    userId = command.userId,
                    notificationType = command.notificationType,
                    scope = command.scope,
                    scopeRefId = command.scopeRefId,
                    subscribed = command.subscribed
                )
            )
        }
    }

    override fun unsubscribe(workspaceId: Long, userId: Long, type: NotificationType, scope: SettingScope, scopeRefId: Long?) {
        val existing = subscriptionRepository.findByUserAndType(workspaceId, userId, type, scope, scopeRefId)
            ?: return
        existing.unsubscribe()
        subscriptionRepository.save(existing)
    }

    /**
     * 특정 프로세스의 구독자 중 활성 채널이 있는 사용자만 반환.
     * Kafka Consumer에서 수신자 결정 시 호출.
     */
    fun getActiveSubscribers(
        workspaceId: Long, notificationType: NotificationType,
        scope: SettingScope, scopeRefId: Long?,
        openingId: Long?, processOnOpeningId: Long?
    ): List<NotificationRecipient> {
        val subscriptions = subscriptionRepository.findAllByUser(workspaceId, userId = 0) // scope 기반 조회 필요
        // 구독 활성 사용자만 필터
        val subscribedUserIds = subscriptions
            .filter { it.subscribed }
            .map { it.userId }

        val recipients = userQueryPort.findByIds(subscribedUserIds)

        // 설정 resolve -> 활성 채널 있는 사용자만 필터
        return recipients.filter { recipient ->
            val resolved = settingResolver.resolve(
                workspaceId, recipient.userId, notificationType, openingId, processOnOpeningId
            )
            resolved.channelPreference.hasAnyEnabled()
        }
    }
}
```

---

## 7. 패키지 구조 요약

```
application/
├── port/
│   ├── in/
│   │   ├── CreateNotificationUseCase.kt
│   │   ├── QueryNotificationUseCase.kt
│   │   ├── ReadNotificationUseCase.kt
│   │   ├── ManageSettingUseCase.kt
│   │   ├── AdminSettingUseCase.kt
│   │   ├── ManageSubscriptionUseCase.kt
│   │   ├── ManageTemplateUseCase.kt
│   │   ├── ScheduleRemindUseCase.kt
│   │   └── ExecuteScheduleUseCase.kt
│   └── out/
│       ├── NotificationRepository.kt
│       ├── NotificationSettingRepository.kt
│       ├── SettingOverrideRepository.kt
│       ├── SubscriptionRepository.kt
│       ├── TemplateRepository.kt
│       ├── ScheduleRepository.kt
│       ├── ProcessedEventRepository.kt
│       ├── NotificationChannelSender.kt
│       ├── DomainEventPublisher.kt
│       ├── TemplateRenderer.kt
│       ├── UserQueryPort.kt
│       └── WorkspaceQueryPort.kt
├── dto/
│   ├── command/
│   │   ├── CreateNotificationCommand.kt
│   │   ├── UpsertSettingCommand.kt
│   │   ├── UpsertOverrideCommand.kt
│   │   ├── SubscribeCommand.kt
│   │   ├── UpsertTemplateCommand.kt
│   │   ├── RegisterScheduleCommand.kt
│   │   └── CancelScheduleCommand.kt
│   ├── query/
│   │   └── NotificationListQuery.kt
│   └── result/
│       ├── NotificationPage.kt
│       ├── UnreadCount.kt
│       ├── ResolvedSettingResult.kt
│       ├── RenderedContent.kt
│       ├── ValidationResult.kt
│       └── SendResult.kt
└── service/
    ├── NotificationService.kt          # Create + Query + Read
    ├── NotificationSettingService.kt   # ManageSetting + AdminSetting
    ├── RemindScheduleService.kt        # ScheduleRemind + ExecuteSchedule
    └── SubscriptionService.kt          # ManageSubscription + getActiveSubscribers
```
