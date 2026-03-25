# [GRT-4003] 도메인 모델 + Port 인터페이스 정의

## 개요
- PRD: https://doodlin.atlassian.net/wiki/x/SICjdg
- Phase: 1 (서비스 구축)
- 예상 공수: 3d
- 의존성: GRT-4001

**범위:** domain 및 application 모듈에 알림 도메인 모델, Enum, Value Object, Port 인터페이스(UseCase + Repository) 정의

## 작업 내용

### 1. 도메인 모델 (domain 모듈)

#### Notification
```kotlin
// domain/src/main/kotlin/.../domain/model/Notification.kt
data class Notification(
    val id: Long? = null,
    val workspaceId: Long,
    val recipientUserId: Long,
    val type: NotificationType,
    val category: NotificationCategory,
    val channel: NotificationChannel,
    val title: String,
    val content: String?,
    val metadata: Map<String, Any>?,
    val sourceType: SourceType?,
    val sourceId: Long?,
    val isRead: Boolean = false,
    val readAt: LocalDateTime? = null,
    val expireAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun markAsRead(): Notification = copy(isRead = true, readAt = LocalDateTime.now())
    fun isExpired(): Boolean = expireAt?.isBefore(LocalDateTime.now()) ?: false
}
```

#### NotificationSetting
```kotlin
data class NotificationSetting(
    val id: Long? = null,
    val workspaceId: Long,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val enabled: Boolean = true,
    val config: Map<String, Any>? = null
)
```

#### NotificationSubscription
```kotlin
data class NotificationSubscription(
    val id: Long? = null,
    val workspaceId: Long,
    val userId: Long,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val enabled: Boolean = true,
    val overrideByAdmin: Boolean = false
)
```

#### NotificationTemplate
```kotlin
data class NotificationTemplate(
    val id: Long? = null,
    val workspaceId: Long?,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val subjectTemplate: String?,
    val bodyTemplate: String,
    val variables: List<TemplateVariable>,
    val isDefault: Boolean = false
) {
    fun render(bindings: Map<String, String>): RenderedTemplate {
        var renderedSubject = subjectTemplate
        var renderedBody = bodyTemplate
        bindings.forEach { (key, value) ->
            renderedSubject = renderedSubject?.replace("\${$key}", value)
            renderedBody = renderedBody.replace("\${$key}", value)
        }
        return RenderedTemplate(renderedSubject, renderedBody)
    }
}
```

#### NotificationSchedule
```kotlin
data class NotificationSchedule(
    val id: Long? = null,
    val workspaceId: Long,
    val notificationType: NotificationType,
    val targetId: Long,
    val targetType: TargetType,
    val scheduledAt: LocalDateTime,
    val status: ScheduleStatus = ScheduleStatus.PENDING,
    val retryCount: Int = 0,
    val maxRetry: Int = 3
) {
    fun cancel(reason: String): NotificationSchedule =
        copy(status = ScheduleStatus.CANCELLED)

    fun markSent(): NotificationSchedule =
        copy(status = ScheduleStatus.SENT)

    fun markFailed(): NotificationSchedule =
        if (retryCount < maxRetry) copy(retryCount = retryCount + 1)
        else copy(status = ScheduleStatus.FAILED)

    fun isDue(): Boolean = scheduledAt.isBefore(LocalDateTime.now()) && status == ScheduleStatus.PENDING
}
```

### 2. Enum / Value Object (domain 모듈)

```kotlin
enum class NotificationType {
    EVALUATION_COMPLETED,
    EVALUATION_SUBMITTED,
    STAGE_ENTRY,
    INTERVIEW_REMIND,
    EVALUATION_REMIND,
    SYSTEM
}

enum class NotificationCategory {
    EVALUATION, STAGE, REMIND, SYSTEM
}

enum class NotificationChannel {
    IN_APP, EMAIL, SLACK
}

enum class SourceType {
    APPLICANT, MEETING, EVALUATION
}

enum class TargetType {
    MEETING, EVALUATION
}

enum class ScheduleStatus {
    PENDING, SENT, CANCELLED, FAILED
}

data class TemplateVariable(
    val name: String,
    val description: String,
    val required: Boolean
)

data class RenderedTemplate(
    val subject: String?,
    val body: String
)
```

### 3. Port 인터페이스 - UseCase (application 모듈)

```kotlin
// Inbound Ports (UseCase)
interface CreateNotificationUseCase {
    fun create(command: CreateNotificationCommand): Notification
}

interface GetNotificationsUseCase {
    fun getByRecipient(workspaceId: Long, userId: Long, page: Int, size: Int): Page<Notification>
    fun getUnreadCount(workspaceId: Long, userId: Long): Long
}

interface MarkNotificationReadUseCase {
    fun markAsRead(notificationId: Long, userId: Long)
    fun markAllAsRead(workspaceId: Long, userId: Long)
}

interface ManageNotificationSettingUseCase {
    fun getSetting(workspaceId: Long, type: NotificationType, channel: NotificationChannel): NotificationSetting?
    fun getSettings(workspaceId: Long): List<NotificationSetting>
    fun upsertSetting(command: UpsertSettingCommand): NotificationSetting
}

interface ManageSubscriptionUseCase {
    fun getSubscription(workspaceId: Long, userId: Long, type: NotificationType): List<NotificationSubscription>
    fun upsertSubscription(command: UpsertSubscriptionCommand): NotificationSubscription
    fun adminOverride(command: AdminOverrideCommand): NotificationSubscription
}

interface ManageTemplateUseCase {
    fun getTemplate(workspaceId: Long, type: NotificationType, channel: NotificationChannel): NotificationTemplate
    fun upsertTemplate(command: UpsertTemplateCommand): NotificationTemplate
    fun resetToDefault(workspaceId: Long, type: NotificationType, channel: NotificationChannel)
}

interface ManageScheduleUseCase {
    fun registerSchedule(command: RegisterScheduleCommand): NotificationSchedule
    fun cancelSchedule(scheduleId: Long, reason: String)
    fun cancelByTarget(targetType: TargetType, targetId: Long, reason: String)
    fun getDueSchedules(limit: Int): List<NotificationSchedule>
}

interface ResolveNotificationSettingUseCase {
    fun resolve(workspaceId: Long, userId: Long, type: NotificationType, channel: NotificationChannel): Boolean
}
```

### 4. Port 인터페이스 - Repository (application 모듈)

```kotlin
// Outbound Ports (Repository)
interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun findById(id: Long): Notification?
    fun findByRecipient(workspaceId: Long, userId: Long, pageable: Pageable): Page<Notification>
    fun countUnread(workspaceId: Long, userId: Long): Long
    fun markAsRead(id: Long)
    fun markAllAsRead(workspaceId: Long, userId: Long)
}

interface NotificationSettingRepository {
    fun save(setting: NotificationSetting): NotificationSetting
    fun findByWorkspaceAndType(workspaceId: Long, type: NotificationType, channel: NotificationChannel): NotificationSetting?
    fun findAllByWorkspace(workspaceId: Long): List<NotificationSetting>
    fun findEnabledByType(type: NotificationType): List<NotificationSetting>
}

interface NotificationSubscriptionRepository {
    fun save(subscription: NotificationSubscription): NotificationSubscription
    fun findByUser(workspaceId: Long, userId: Long, type: NotificationType): List<NotificationSubscription>
    fun findEnabledSubscribers(workspaceId: Long, type: NotificationType, channel: NotificationChannel): List<NotificationSubscription>
    fun findByWorkspaceAndType(workspaceId: Long, type: NotificationType): List<NotificationSubscription>
}

interface NotificationTemplateRepository {
    fun save(template: NotificationTemplate): NotificationTemplate
    fun findByWorkspaceAndType(workspaceId: Long, type: NotificationType, channel: NotificationChannel): NotificationTemplate?
    fun findDefault(type: NotificationType, channel: NotificationChannel): NotificationTemplate?
    fun deleteByWorkspaceAndType(workspaceId: Long, type: NotificationType, channel: NotificationChannel)
}

interface NotificationScheduleRepository {
    fun save(schedule: NotificationSchedule): NotificationSchedule
    fun findById(id: Long): NotificationSchedule?
    fun findPendingDue(now: LocalDateTime, limit: Int): List<NotificationSchedule>
    fun findByTarget(targetType: TargetType, targetId: Long): List<NotificationSchedule>
    fun cancelByTarget(targetType: TargetType, targetId: Long, reason: String): Int
}

interface NotificationLogRepository {
    fun save(log: NotificationLog): NotificationLog
}

interface ConsumedEventRepository {
    fun existsByEventId(eventId: String): Boolean
    fun save(eventId: String, topic: String)
}
```

### 5. Command 객체 (application 모듈)

```kotlin
data class CreateNotificationCommand(
    val workspaceId: Long,
    val recipientUserId: Long,
    val type: NotificationType,
    val channel: NotificationChannel,
    val title: String,
    val content: String?,
    val metadata: Map<String, Any>? = null,
    val sourceType: SourceType? = null,
    val sourceId: Long? = null
)

data class UpsertSettingCommand(
    val workspaceId: Long,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val enabled: Boolean,
    val config: Map<String, Any>? = null
)

data class UpsertSubscriptionCommand(
    val workspaceId: Long,
    val userId: Long,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val enabled: Boolean
)

data class AdminOverrideCommand(
    val workspaceId: Long,
    val adminUserId: Long,
    val targetUserId: Long,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val enabled: Boolean
)

data class UpsertTemplateCommand(
    val workspaceId: Long,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val subjectTemplate: String?,
    val bodyTemplate: String
)

data class RegisterScheduleCommand(
    val workspaceId: Long,
    val notificationType: NotificationType,
    val targetId: Long,
    val targetType: TargetType,
    val scheduledAt: LocalDateTime
)
```

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-notification-service | domain | src/.../domain/model/Notification.kt | 신규 |
| greeting-notification-service | domain | src/.../domain/model/NotificationSetting.kt | 신규 |
| greeting-notification-service | domain | src/.../domain/model/NotificationSubscription.kt | 신규 |
| greeting-notification-service | domain | src/.../domain/model/NotificationTemplate.kt | 신규 |
| greeting-notification-service | domain | src/.../domain/model/NotificationSchedule.kt | 신규 |
| greeting-notification-service | domain | src/.../domain/model/NotificationLog.kt | 신규 |
| greeting-notification-service | domain | src/.../domain/enums/*.kt | 신규 (6개 Enum) |
| greeting-notification-service | domain | src/.../domain/vo/TemplateVariable.kt | 신규 |
| greeting-notification-service | domain | src/.../domain/vo/RenderedTemplate.kt | 신규 |
| greeting-notification-service | application | src/.../application/port/inbound/*.kt | 신규 (8개 UseCase) |
| greeting-notification-service | application | src/.../application/port/outbound/*.kt | 신규 (7개 Repository) |
| greeting-notification-service | application | src/.../application/command/*.kt | 신규 (6개 Command) |

## 영향 범위

- domain 모듈: 순수 Kotlin, 외부 의존성 없음
- application 모듈: domain에만 의존, 구현체 없음 (인터페이스만)
- 기존 서비스에 영향 없음

## 테스트 케이스

| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-03-01 | Notification 읽음 처리 | isRead=false인 Notification | markAsRead() 호출 | isRead=true, readAt 설정됨 |
| TC-03-02 | Notification 만료 확인 | expireAt이 과거 | isExpired() 호출 | true 반환 |
| TC-03-03 | NotificationSchedule 취소 | PENDING 상태 | cancel("면접 취소") | status=CANCELLED |
| TC-03-04 | NotificationSchedule 실패 재시도 | retryCount=0, maxRetry=3 | markFailed() 호출 | retryCount=1, status=PENDING 유지 |
| TC-03-05 | NotificationSchedule 최대 재시도 초과 | retryCount=3, maxRetry=3 | markFailed() 호출 | status=FAILED |
| TC-03-06 | NotificationTemplate 렌더링 | bodyTemplate에 ${applicantName} 포함 | render(mapOf("applicantName" to "홍길동")) | body에 "홍길동" 치환 |
| TC-03-07 | NotificationCategory 매핑 | EVALUATION_COMPLETED 타입 | category 확인 | EVALUATION |
| TC-03-08 | 설정 resolve 우선순위 | 관리자 override=OFF, 개인=ON | resolve() 호출 | false (관리자 우선) |

## 기대 결과 (AC)

- [ ] 도메인 모델 6개 정의 (Notification, Setting, Subscription, Template, Schedule, Log)
- [ ] Enum 6개, Value Object 2개 정의
- [ ] Inbound Port(UseCase) 8개 인터페이스 정의
- [ ] Outbound Port(Repository) 7개 인터페이스 정의
- [ ] Command 객체 6개 정의
- [ ] 도메인 로직 단위 테스트 전체 통과 (8개 이상)
- [ ] domain 모듈에 Spring 의존성 없음 확인

## 체크리스트

- [ ] domain 모듈 build.gradle.kts에 Spring 의존성 없는지 확인
- [ ] data class의 copy() 활용한 불변 패턴 적용
- [ ] null 안전성 (Kotlin nullable 타입 활용)
- [ ] 패키지 구조: domain/model, domain/enums, domain/vo, application/port/inbound, application/port/outbound, application/command
