# [알림 시스템 완전 리팩토링] Part 3a: Domain 모듈 코드 설계

> 상위 문서: [Part 1 - ERD & 도메인](./tdd_part1_erd_domain.md) | [Part 2 - 아키텍처 & API](./tdd_part2_architecture_api.md)
> 작성일: 2026-03-17
> 목적: greeting-notification-service의 domain 모듈 순수 코틀린 코드 설계
> 원칙: 외부 의존 0, 프레임워크 어노테이션 0, 순수 도메인 로직만

---

## 1. build.gradle.kts (domain)

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    // 외부 의존 없음. 순수 Kotlin only.
}
```

---

## 2. 패키지 구조

```
domain/
  model/
    Notification.kt
    NotificationSetting.kt
    NotificationSettingOverride.kt
    NotificationSubscription.kt
    NotificationTemplate.kt
    NotificationSchedule.kt
  enums/
    NotificationCategory.kt
    NotificationChannel.kt
    NotificationSourceType.kt
    ScheduleStatus.kt
    SettingScope.kt
  vo/
    ChannelPreference.kt
    RenderedContent.kt
    NotificationRecipient.kt
  service/
    NotificationSettingResolver.kt
  port/
    out/
      NotificationRepository.kt
      NotificationSettingRepository.kt
      NotificationOverrideRepository.kt
      NotificationSubscriptionRepository.kt
      NotificationTemplateRepository.kt
      NotificationScheduleRepository.kt
```

---

## 3. Enum (5개)

```kotlin
enum class NotificationCategory {
    EVALUATION,
    STAGE,
    MEETING,
    SYSTEM,
    MENTION,
    MAIL,
    APPLICANT,
    OPENING
}

enum class NotificationChannel {
    IN_APP, EMAIL, SLACK
}

enum class NotificationSourceType {
    APPLICANT, MEETING, EVALUATION, OPENING, SYSTEM
}

enum class ScheduleStatus {
    PENDING, SENT, CANCELLED, FAILED
}

enum class SettingScope(val priority: Int) {
    WORKSPACE(0),
    OPENING(1),
    PROCESS(2),
    USER(3);
}
```

---

## 4. Value Objects

```kotlin
data class ChannelPreference(
    val inApp: Boolean? = null,
    val email: Boolean? = null,
    val slack: Boolean? = null,
) {
    /** null = 상위 계층 상속. non-null = 해당 값 적용 */
    fun merge(parent: ChannelPreference): ChannelPreference =
        ChannelPreference(
            inApp = this.inApp ?: parent.inApp,
            email = this.email ?: parent.email,
            slack = this.slack ?: parent.slack,
        )

    fun isEnabled(channel: NotificationChannel): Boolean = when (channel) {
        NotificationChannel.IN_APP -> inApp ?: true
        NotificationChannel.EMAIL -> email ?: false
        NotificationChannel.SLACK -> slack ?: false
    }
}

data class RenderedContent(
    val subject: String,
    val body: String,
)

data class NotificationRecipient(
    val userId: Long,
    val channels: Set<NotificationChannel>,
)
```

---

## 5. 도메인 모델 (6개)

### 5.1 Notification

```kotlin
data class Notification(
    val id: Long = 0L,
    val recipientUserId: Long,
    val workspaceId: Long,
    val category: NotificationCategory,
    val sourceType: NotificationSourceType?,
    val sourceId: Long?,
    val title: String,
    val body: String?,
    val imageUrl: String? = null,
    val url: String? = null,
    val channel: NotificationChannel,
    val read: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val expiredAt: LocalDateTime? = null,
) {
    fun markRead(): Notification = copy(read = true)

    fun isExpired(now: LocalDateTime): Boolean =
        expiredAt != null && now.isAfter(expiredAt)
}
```

### 5.2 NotificationSetting

```kotlin
data class NotificationSetting(
    val id: Long = 0L,
    val workspaceId: Long,
    val scope: SettingScope,
    val scopeId: Long? = null,
    val userId: Long? = null,
    val category: NotificationCategory,
    val channelPreferences: ChannelPreference,
    val enabled: Boolean = true,
)
```

### 5.3 NotificationSettingOverride

```kotlin
data class NotificationSettingOverride(
    val id: Long = 0L,
    val workspaceId: Long,
    val targetUserId: Long,
    val configuredByUserId: Long,
    val category: NotificationCategory,
    val channelPreferences: ChannelPreference,
    val forceEnabled: Boolean? = null,
)
```

### 5.4 NotificationSubscription

```kotlin
data class NotificationSubscription(
    val id: Long = 0L,
    val workspaceId: Long,
    val userId: Long,
    val openingId: Long? = null,
    val processId: Long? = null,
    val category: NotificationCategory,
    val enabled: Boolean = true,
)
```

### 5.5 NotificationTemplate

```kotlin
data class NotificationTemplate(
    val id: Long = 0L,
    val workspaceId: Long?,
    val category: NotificationCategory,
    val channel: NotificationChannel,
    val name: String,
    val subject: String?,
    val body: String,
    val variables: List<String> = emptyList(),
    val isDefault: Boolean = false,
) {
    fun render(vars: Map<String, String>): RenderedContent {
        var renderedSubject = subject.orEmpty()
        var renderedBody = body
        vars.forEach { (key, value) ->
            renderedSubject = renderedSubject.replace("{{$key}}", value)
            renderedBody = renderedBody.replace("{{$key}}", value)
        }
        return RenderedContent(subject = renderedSubject, body = renderedBody)
    }
}
```

### 5.6 NotificationSchedule

```kotlin
data class NotificationSchedule(
    val id: Long = 0L,
    val workspaceId: Long,
    val sourceType: NotificationSourceType,
    val sourceId: Long,
    val scheduledAt: LocalDateTime,
    val status: ScheduleStatus = ScheduleStatus.PENDING,
    val idempotencyKey: String,
    val channel: NotificationChannel,
    val recipientUserId: Long,
    val maxRetry: Int = 3,
    val retryCount: Int = 0,
) {
    fun canExecute(now: LocalDateTime): Boolean =
        status == ScheduleStatus.PENDING && !now.isBefore(scheduledAt)

    fun markSent(): NotificationSchedule =
        copy(status = ScheduleStatus.SENT)

    fun markFailed(): NotificationSchedule =
        if (retryCount + 1 >= maxRetry) copy(status = ScheduleStatus.FAILED, retryCount = retryCount + 1)
        else copy(retryCount = retryCount + 1)

    fun markCancelled(): NotificationSchedule =
        copy(status = ScheduleStatus.CANCELLED)
}
```

---

## 6. 설정 Resolve 알고리즘 (도메인 서비스)

```kotlin
class NotificationSettingResolver(
    private val settingRepository: NotificationSettingRepository,
    private val overrideRepository: NotificationOverrideRepository,
) {
    /**
     * 계층 resolve 순서:
     *   WORKSPACE -> OPENING -> PROCESS -> ADMIN_OVERRIDE -> USER
     *
     * 각 계층의 ChannelPreference를 merge하며,
     * null 필드는 상위 값을 상속, non-null은 덮어씀.
     * ADMIN_OVERRIDE의 forceEnabled=true면 해당 채널 강제 ON.
     */
    fun resolveEffective(
        workspaceId: Long,
        openingId: Long?,
        processId: Long?,
        userId: Long,
        category: NotificationCategory,
    ): ChannelPreference {
        // 1) WORKSPACE 기본값
        val workspace = settingRepository
            .findByScope(workspaceId, SettingScope.WORKSPACE, null, category)
            ?.channelPreferences
            ?: ChannelPreference(inApp = true, email = true, slack = false)

        // 2) OPENING 계층
        val opening = openingId?.let {
            settingRepository
                .findByScope(workspaceId, SettingScope.OPENING, it, category)
                ?.channelPreferences
        }
        val afterOpening = opening?.merge(workspace) ?: workspace

        // 3) PROCESS 계층
        val process = processId?.let {
            settingRepository
                .findByScope(workspaceId, SettingScope.PROCESS, it, category)
                ?.channelPreferences
        }
        val afterProcess = process?.merge(afterOpening) ?: afterOpening

        // 4) ADMIN_OVERRIDE (관리자 강제 설정)
        val adminOverride = overrideRepository
            .findByTarget(workspaceId, userId, category)
        val afterAdmin = if (adminOverride != null) {
            val merged = adminOverride.channelPreferences.merge(afterProcess)
            if (adminOverride.forceEnabled == true) merged
            else merged
        } else afterProcess

        // 5) USER 개인 설정 (최종)
        val userSetting = settingRepository
            .findByScope(workspaceId, SettingScope.USER, userId, category)
            ?.channelPreferences
        val afterUser = userSetting?.merge(afterAdmin) ?: afterAdmin

        // 단, ADMIN forceEnabled=true인 채널은 USER가 끌 수 없음
        return if (adminOverride?.forceEnabled == true) {
            ChannelPreference(
                inApp = if (adminOverride.channelPreferences.inApp == true) true else afterUser.inApp,
                email = if (adminOverride.channelPreferences.email == true) true else afterUser.email,
                slack = if (adminOverride.channelPreferences.slack == true) true else afterUser.slack,
            )
        } else afterUser
    }
}
```

---

## 7. Port Out 인터페이스 (domain에 위치)

```kotlin
interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun findById(id: Long): Notification?
    fun findByRecipient(workspaceId: Long, userId: Long, readOnly: Boolean?, limit: Int, offset: Int): List<Notification>
    fun countUnread(workspaceId: Long, userId: Long): Long
}

interface NotificationSettingRepository {
    fun findByScope(workspaceId: Long, scope: SettingScope, scopeId: Long?, category: NotificationCategory): NotificationSetting?
    fun save(setting: NotificationSetting): NotificationSetting
}

interface NotificationOverrideRepository {
    fun findByTarget(workspaceId: Long, targetUserId: Long, category: NotificationCategory): NotificationSettingOverride?
    fun save(override: NotificationSettingOverride): NotificationSettingOverride
}

interface NotificationSubscriptionRepository {
    fun findByUser(workspaceId: Long, userId: Long, category: NotificationCategory): NotificationSubscription?
    fun save(subscription: NotificationSubscription): NotificationSubscription
}

interface NotificationTemplateRepository {
    fun findDefault(workspaceId: Long?, category: NotificationCategory, channel: NotificationChannel): NotificationTemplate?
    fun findById(id: Long): NotificationTemplate?
    fun save(template: NotificationTemplate): NotificationTemplate
}

interface NotificationScheduleRepository {
    fun save(schedule: NotificationSchedule): NotificationSchedule
    fun findPendingBefore(now: LocalDateTime, limit: Int): List<NotificationSchedule>
    fun findBySourceAndStatus(sourceType: NotificationSourceType, sourceId: Long, status: ScheduleStatus): List<NotificationSchedule>
}
```
