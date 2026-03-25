# [알림 시스템 완전 리팩토링] Part 3b: Infrastructure 모듈 코드 설계

> 상위 문서: [Part 1 - ERD & 도메인](./tdd_part1_erd_domain.md) | [Part 2 - 아키텍처 & API](./tdd_part2_architecture_api.md)
> 작성일: 2026-03-17
> 목적: greeting-notification-service의 infrastructure 모듈 전체 구현 설계
> 기술 스택: Kotlin, Spring Boot 3.x, JPA/Hibernate, Kafka (Confluent Cloud SASL_SSL), Redis (Lettuce TLS), netty-socketio

---

## 1. JPA Entity 전체

### 1.1 패키지 구조

```
infrastructure/
  persistence/
    entity/
      NotificationEntity.kt
      NotificationSettingEntity.kt
      NotificationSettingOverrideEntity.kt
      NotificationSubscriptionEntity.kt
      NotificationTemplateEntity.kt
      NotificationScheduleEntity.kt
      NotificationLogEntity.kt
      NotificationProcessedEventEntity.kt
    converter/
      JsonMapConverter.kt
      TemplateVariableListConverter.kt
    repository/
      *JpaRepository.kt (Spring Data JPA interfaces)
    mapper/
      *Mapper.kt (domain <-> entity 변환)
    adapter/
      *RepositoryAdapter.kt (Port Out 구현체)
    config/
      DataSourceConfig.kt
      ReadWriteRoutingDataSource.kt
```

### 1.2 NotificationEntity

```kotlin
@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(
            name = "idx_notifications_recipient_status",
            columnList = "workspace_id, recipient_user_id, status, created_at DESC"
        ),
        Index(
            name = "idx_notifications_category",
            columnList = "workspace_id, category, created_at DESC"
        ),
        Index(
            name = "idx_notifications_source",
            columnList = "source_type, source_id"
        ),
        Index(
            name = "idx_notifications_expired_at",
            columnList = "expired_at"
        ),
        Index(
            name = "idx_notifications_opening",
            columnList = "opening_id"
        )
    ]
)
class NotificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: Long,

    @Column(name = "recipient_user_id", nullable = false)
    val recipientUserId: Long,

    @Column(name = "notification_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val notificationType: NotificationType,

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val category: NotificationCategory,

    @Column(name = "priority", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val priority: NotificationPriority = NotificationPriority.NORMAL,

    @Column(name = "title", nullable = false, length = 255)
    val title: String,

    @Column(name = "body", columnDefinition = "TEXT")
    val body: String? = null,

    @Column(name = "source_type", length = 50)
    @Enumerated(EnumType.STRING)
    val sourceType: SourceType? = null,

    @Column(name = "source_id")
    val sourceId: Long? = null,

    @Column(name = "opening_id")
    val openingId: Long? = null,

    @Column(name = "process_on_opening_id")
    val processOnOpeningId: Long? = null,

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: NotificationStatus = NotificationStatus.UNREAD,

    @Column(name = "action_url", length = 500)
    val actionUrl: String? = null,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,

    @Column(name = "expired_at")
    val expiredAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 읽음 처리
     */
    fun markAsRead(now: LocalDateTime = LocalDateTime.now()) {
        if (this.status == NotificationStatus.UNREAD) {
            this.status = NotificationStatus.READ
            this.readAt = now
        }
    }

    /**
     * 아카이브 처리
     */
    fun archive() {
        this.status = NotificationStatus.ARCHIVED
    }
}
```

### 1.3 NotificationSettingEntity

```kotlin
@Entity
@Table(
    name = "notification_settings",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_settings_ws_type_scope",
            columnNames = ["workspace_id", "notification_type", "scope", "scope_ref_id"]
        )
    ]
)
class NotificationSettingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: Long,

    @Column(name = "notification_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val notificationType: NotificationType,

    @Column(name = "scope", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val scope: SettingScope,

    @Column(name = "scope_ref_id")
    val scopeRefId: Long? = null,

    @Column(name = "in_app_enabled", nullable = false)
    var inAppEnabled: Boolean = true,

    @Column(name = "email_enabled", nullable = false)
    var emailEnabled: Boolean = true,

    @Column(name = "slack_enabled", nullable = false)
    var slackEnabled: Boolean = false,

    @Column(name = "push_enabled", nullable = false)
    var pushEnabled: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### 1.4 NotificationSettingOverrideEntity

```kotlin
@Entity
@Table(
    name = "notification_setting_overrides",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_override_setting_user",
            columnNames = ["notification_setting_id", "target_user_id"]
        )
    ]
)
class NotificationSettingOverrideEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "notification_setting_id", nullable = false)
    val notificationSettingId: Long,

    @Column(name = "target_user_id", nullable = false)
    val targetUserId: Long,

    @Column(name = "overridden_by_user_id", nullable = false)
    val overriddenByUserId: Long,

    @Column(name = "override_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val overrideType: OverrideType,

    /** NULL = 상위 설정 상속 */
    @Column(name = "in_app_enabled")
    var inAppEnabled: Boolean? = null,

    @Column(name = "email_enabled")
    var emailEnabled: Boolean? = null,

    @Column(name = "slack_enabled")
    var slackEnabled: Boolean? = null,

    @Column(name = "push_enabled")
    var pushEnabled: Boolean? = null,

    @Column(name = "force_disabled", nullable = false)
    var forceDisabled: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### 1.5 NotificationSubscriptionEntity

```kotlin
@Entity
@Table(
    name = "notification_subscriptions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_subscription_user_type_scope",
            columnNames = ["workspace_id", "user_id", "notification_type", "scope", "scope_ref_id"]
        )
    ]
)
class NotificationSubscriptionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "notification_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val notificationType: NotificationType,

    @Column(name = "scope", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val scope: SettingScope,

    @Column(name = "scope_ref_id")
    val scopeRefId: Long? = null,

    @Column(name = "subscribed", nullable = false)
    var subscribed: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### 1.6 NotificationTemplateEntity

```kotlin
@Entity
@Table(
    name = "notification_templates",
    indexes = [
        Index(
            name = "idx_template_ws_type_channel",
            columnList = "workspace_id, notification_type, channel_type, locale"
        )
    ]
)
class NotificationTemplateEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** NULL = 시스템 기본 템플릿 */
    @Column(name = "workspace_id")
    val workspaceId: Long? = null,

    @Column(name = "notification_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val notificationType: NotificationType,

    @Column(name = "channel_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    val channelType: ChannelType,

    @Column(name = "locale", nullable = false, length = 20)
    val locale: String = "ko_KR",

    @Column(name = "subject_template", length = 255)
    val subjectTemplate: String? = null,

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    val bodyTemplate: String,

    /**
     * 사용 가능한 변수 스키마 정의 (JSON)
     * 예: [{"name":"applicantName","type":"string","required":true}]
     */
    @Column(name = "variable_schema_json", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = TemplateVariableListConverter::class)
    val variableSchema: List<TemplateVariable>,

    @Column(name = "is_default", nullable = false)
    val isDefault: Boolean = false,

    @Column(name = "version", nullable = false)
    val version: Int = 1,

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 템플릿 변수 스키마 VO
 */
data class TemplateVariable(
    val name: String,
    val type: String,       // string, number, boolean, date
    val required: Boolean,
    val defaultValue: String? = null,
    val description: String? = null
)
```

### 1.7 NotificationScheduleEntity

```kotlin
@Entity
@Table(
    name = "notification_schedules",
    indexes = [
        Index(
            name = "idx_schedule_status_time",
            columnList = "status, scheduled_at"
        ),
        Index(
            name = "idx_schedule_source",
            columnList = "workspace_id, source_type, source_id, status"
        )
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_schedule_idempotency",
            columnNames = ["idempotency_key"]
        )
    ]
)
class NotificationScheduleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: Long,

    @Column(name = "schedule_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val scheduleType: ScheduleType,

    @Column(name = "source_id", nullable = false)
    val sourceId: Long,

    @Column(name = "source_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val sourceType: SourceType,

    @Column(name = "target_user_id", nullable = false)
    val targetUserId: Long,

    @Column(name = "template_id")
    val templateId: Long? = null,

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: ScheduleStatus = ScheduleStatus.PENDING,

    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: LocalDateTime,

    @Column(name = "triggered_at")
    var triggeredAt: LocalDateTime? = null,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "max_retry", nullable = false)
    val maxRetry: Int = 3,

    @Column(name = "idempotency_key", nullable = false, length = 36)
    val idempotencyKey: String,

    @Column(name = "payload_json", columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter::class)
    val payloadJson: Map<String, Any>? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun trigger(now: LocalDateTime = LocalDateTime.now()) {
        this.status = ScheduleStatus.TRIGGERED
        this.triggeredAt = now
    }

    fun markSent() {
        this.status = ScheduleStatus.SENT
    }

    fun markFailed() {
        this.retryCount++
        if (this.retryCount >= this.maxRetry) {
            this.status = ScheduleStatus.FAILED
        }
    }

    fun cancel() {
        this.status = ScheduleStatus.CANCELLED
    }
}
```

### 1.8 NotificationLogEntity

```kotlin
@Entity
@Table(
    name = "notification_logs",
    indexes = [
        Index(
            name = "idx_log_workspace_created",
            columnList = "workspace_id, created_at DESC"
        )
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_log_idempotency",
            columnNames = ["idempotency_key"]
        )
    ]
)
class NotificationLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "notification_id")
    val notificationId: Long? = null,

    @Column(name = "schedule_id")
    val scheduleId: Long? = null,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: Long,

    @Column(name = "recipient_user_id", nullable = false)
    val recipientUserId: Long,

    @Column(name = "channel_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    val channelType: ChannelType,

    @Column(name = "notification_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val notificationType: NotificationType,

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: DeliveryStatus,

    @Column(name = "idempotency_key", nullable = false, length = 36)
    val idempotencyKey: String,

    @Column(name = "request_payload", columnDefinition = "TEXT")
    val requestPayload: String? = null,

    @Column(name = "response_payload", columnDefinition = "TEXT")
    var responsePayload: String? = null,

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    var failureReason: String? = null,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 1,

    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,

    @Column(name = "delivered_at")
    var deliveredAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### 1.9 NotificationProcessedEventEntity

```kotlin
@Entity
@Table(name = "notification_processed_events")
class NotificationProcessedEventEntity(
    @Id
    @Column(name = "event_id", length = 36)
    val eventId: String,

    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: ProcessedEventStatus = ProcessedEventStatus.PROCESSED,

    @Column(name = "processed_at", nullable = false)
    val processedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class ProcessedEventStatus {
    PROCESSED,
    FAILED
}
```

### 1.10 JSON Converter

```kotlin
@Converter
class JsonMapConverter : AttributeConverter<Map<String, Any>?, String?> {
    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())

    override fun convertToDatabaseColumn(attribute: Map<String, Any>?): String? =
        attribute?.let { objectMapper.writeValueAsString(it) }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Any>? =
        dbData?.let {
            objectMapper.readValue(it, object : TypeReference<Map<String, Any>>() {})
        }
}

@Converter
class TemplateVariableListConverter : AttributeConverter<List<TemplateVariable>?, String?> {
    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())

    override fun convertToDatabaseColumn(attribute: List<TemplateVariable>?): String? =
        attribute?.let { objectMapper.writeValueAsString(it) }

    override fun convertToEntityAttribute(dbData: String?): List<TemplateVariable>? =
        dbData?.let {
            objectMapper.readValue(it, object : TypeReference<List<TemplateVariable>>() {})
        }
}
```

---

## 2. Repository Adapter

### 2.1 JPA Repository 인터페이스 (Spring Data JPA)

#### NotificationJpaRepository

```kotlin
interface NotificationJpaRepository : JpaRepository<NotificationEntity, Long> {

    /**
     * 사용자별 알림 목록 (메인 쿼리 - idx_notifications_recipient_status 활용)
     */
    fun findByWorkspaceIdAndRecipientUserIdAndStatusInOrderByCreatedAtDesc(
        workspaceId: Long,
        recipientUserId: Long,
        statuses: List<NotificationStatus>,
        pageable: Pageable
    ): Page<NotificationEntity>

    /**
     * 카테고리 필터링 (idx_notifications_category 활용)
     */
    fun findByWorkspaceIdAndRecipientUserIdAndCategoryAndStatusInOrderByCreatedAtDesc(
        workspaceId: Long,
        recipientUserId: Long,
        category: NotificationCategory,
        statuses: List<NotificationStatus>,
        pageable: Pageable
    ): Page<NotificationEntity>

    /**
     * 미읽음 카운트
     */
    fun countByWorkspaceIdAndRecipientUserIdAndStatus(
        workspaceId: Long,
        recipientUserId: Long,
        status: NotificationStatus
    ): Long

    /**
     * 단건 읽음 처리
     */
    @Modifying
    @Query("""
        UPDATE NotificationEntity n
        SET n.status = 'READ', n.readAt = :now
        WHERE n.id = :id AND n.status = 'UNREAD'
    """)
    fun markAsRead(@Param("id") id: Long, @Param("now") now: LocalDateTime): Int

    /**
     * 전체 읽음 처리
     */
    @Modifying
    @Query("""
        UPDATE NotificationEntity n
        SET n.status = 'READ', n.readAt = :now
        WHERE n.workspaceId = :workspaceId
          AND n.recipientUserId = :userId
          AND n.status = 'UNREAD'
    """)
    fun markAllAsRead(
        @Param("workspaceId") workspaceId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime
    ): Int

    /**
     * 소스 기반 역추적 (idx_notifications_source 활용)
     */
    fun findBySourceTypeAndSourceId(sourceType: SourceType, sourceId: Long): List<NotificationEntity>

    /**
     * 만료 알림 정리 배치 (idx_notifications_expired_at 활용)
     */
    @Modifying
    @Query("""
        UPDATE NotificationEntity n
        SET n.status = 'ARCHIVED'
        WHERE n.expiredAt IS NOT NULL AND n.expiredAt <= :now AND n.status != 'ARCHIVED'
    """)
    fun archiveExpired(@Param("now") now: LocalDateTime): Int
}
```

#### NotificationSettingJpaRepository

```kotlin
interface NotificationSettingJpaRepository : JpaRepository<NotificationSettingEntity, Long> {

    /**
     * 워크스페이스 전체 설정 조회
     */
    fun findAllByWorkspaceId(workspaceId: Long): List<NotificationSettingEntity>

    /**
     * 계층화 설정 resolve 쿼리: WORKSPACE -> OPENING -> PROCESS 순서로 조회
     */
    @Query("""
        SELECT s FROM NotificationSettingEntity s
        WHERE s.workspaceId = :workspaceId
          AND s.notificationType = :type
          AND (
            (s.scope = 'WORKSPACE' AND s.scopeRefId IS NULL)
            OR (s.scope = 'OPENING' AND s.scopeRefId = :openingId)
            OR (s.scope = 'PROCESS' AND s.scopeRefId = :processId)
          )
        ORDER BY
          CASE s.scope
            WHEN 'PROCESS' THEN 1
            WHEN 'OPENING' THEN 2
            WHEN 'WORKSPACE' THEN 3
          END
    """)
    fun findSettingHierarchy(
        @Param("workspaceId") workspaceId: Long,
        @Param("type") type: NotificationType,
        @Param("openingId") openingId: Long?,
        @Param("processId") processId: Long?
    ): List<NotificationSettingEntity>

    /**
     * 특정 스코프 설정 조회 (UPSERT용)
     */
    fun findByWorkspaceIdAndNotificationTypeAndScopeAndScopeRefId(
        workspaceId: Long,
        notificationType: NotificationType,
        scope: SettingScope,
        scopeRefId: Long?
    ): NotificationSettingEntity?
}
```

#### NotificationSettingOverrideJpaRepository

```kotlin
interface NotificationSettingOverrideJpaRepository
    : JpaRepository<NotificationSettingOverrideEntity, Long> {

    fun findByNotificationSettingIdAndTargetUserId(
        notificationSettingId: Long,
        targetUserId: Long
    ): NotificationSettingOverrideEntity?

    fun findAllByNotificationSettingId(
        notificationSettingId: Long
    ): List<NotificationSettingOverrideEntity>

    /**
     * 특정 사용자의 모든 오버라이드 조회 (개인 설정 화면)
     */
    fun findAllByTargetUserId(targetUserId: Long): List<NotificationSettingOverrideEntity>

    /**
     * 관리자 강제 비활성화 조회
     */
    fun findByNotificationSettingIdAndTargetUserIdAndOverrideType(
        notificationSettingId: Long,
        targetUserId: Long,
        overrideType: OverrideType
    ): NotificationSettingOverrideEntity?
}
```

#### NotificationSubscriptionJpaRepository

```kotlin
interface NotificationSubscriptionJpaRepository
    : JpaRepository<NotificationSubscriptionEntity, Long> {

    /**
     * 사용자별 구독 목록
     */
    fun findByWorkspaceIdAndUserId(
        workspaceId: Long,
        userId: Long
    ): List<NotificationSubscriptionEntity>

    /**
     * 특정 타입 + 스코프의 활성 구독자 조회 (수신자 목록 resolve)
     */
    fun findByWorkspaceIdAndNotificationTypeAndScopeAndScopeRefIdAndSubscribedTrue(
        workspaceId: Long,
        notificationType: NotificationType,
        scope: SettingScope,
        scopeRefId: Long?
    ): List<NotificationSubscriptionEntity>

    /**
     * UPSERT용
     */
    fun findByWorkspaceIdAndUserIdAndNotificationTypeAndScopeAndScopeRefId(
        workspaceId: Long,
        userId: Long,
        notificationType: NotificationType,
        scope: SettingScope,
        scopeRefId: Long?
    ): NotificationSubscriptionEntity?
}
```

#### NotificationTemplateJpaRepository

```kotlin
interface NotificationTemplateJpaRepository
    : JpaRepository<NotificationTemplateEntity, Long> {

    /**
     * 워크스페이스 커스텀 템플릿 조회
     */
    fun findByWorkspaceIdAndNotificationTypeAndChannelTypeAndLocale(
        workspaceId: Long,
        notificationType: NotificationType,
        channelType: ChannelType,
        locale: String
    ): NotificationTemplateEntity?

    /**
     * 시스템 기본 템플릿 조회 (workspace_id IS NULL)
     */
    @Query("""
        SELECT t FROM NotificationTemplateEntity t
        WHERE t.workspaceId IS NULL
          AND t.notificationType = :type
          AND t.channelType = :channel
          AND t.locale = :locale
          AND t.isDefault = true
        ORDER BY t.version DESC
    """)
    fun findDefaultTemplate(
        @Param("type") type: NotificationType,
        @Param("channel") channel: ChannelType,
        @Param("locale") locale: String
    ): NotificationTemplateEntity?

    /**
     * 워크스페이스 전체 템플릿 목록 (관리자용)
     */
    fun findAllByWorkspaceId(workspaceId: Long): List<NotificationTemplateEntity>
}
```

#### NotificationScheduleJpaRepository

```kotlin
interface NotificationScheduleJpaRepository
    : JpaRepository<NotificationScheduleEntity, Long> {

    /**
     * Scheduler 폴링: PENDING 상태 + 발송 시점 도래 (idx_schedule_status_time 활용)
     */
    @Query("""
        SELECT s FROM NotificationScheduleEntity s
        WHERE s.status = 'PENDING'
          AND s.scheduledAt <= :now
        ORDER BY s.scheduledAt ASC
    """)
    fun findPendingDue(
        @Param("now") now: LocalDateTime,
        pageable: Pageable
    ): List<NotificationScheduleEntity>

    /**
     * 소스별 스케줄 조회 (idx_schedule_source 활용)
     */
    fun findByWorkspaceIdAndSourceTypeAndSourceIdAndStatus(
        workspaceId: Long,
        sourceType: SourceType,
        sourceId: Long,
        status: ScheduleStatus
    ): List<NotificationScheduleEntity>

    /**
     * 소스 변경 시 cascade 취소 (면접 시간 변경 -> 기존 리마인드 취소)
     */
    @Modifying
    @Query("""
        UPDATE NotificationScheduleEntity s
        SET s.status = 'CANCELLED', s.updatedAt = :now
        WHERE s.workspaceId = :workspaceId
          AND s.sourceType = :sourceType
          AND s.sourceId = :sourceId
          AND s.status = 'PENDING'
    """)
    fun cancelBySource(
        @Param("workspaceId") workspaceId: Long,
        @Param("sourceType") sourceType: SourceType,
        @Param("sourceId") sourceId: Long,
        @Param("now") now: LocalDateTime
    ): Int

    /**
     * 멱등성 키 기반 존재 확인
     */
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}
```

#### NotificationLogJpaRepository

```kotlin
interface NotificationLogJpaRepository
    : JpaRepository<NotificationLogEntity, Long> {

    /**
     * 발송 이력 조회 (관리자용, idx_log_workspace_created 활용)
     */
    fun findByWorkspaceIdOrderByCreatedAtDesc(
        workspaceId: Long,
        pageable: Pageable
    ): Page<NotificationLogEntity>

    /**
     * 알림별 발송 이력
     */
    fun findByNotificationId(notificationId: Long): List<NotificationLogEntity>

    /**
     * 스케줄별 발송 이력
     */
    fun findByScheduleId(scheduleId: Long): List<NotificationLogEntity>

    /**
     * 멱등성 키 기반 존재 확인 (중복 발송 방지)
     */
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean

    /**
     * 채널별 발송 통계 (대시보드용)
     */
    @Query("""
        SELECT l.channelType, l.status, COUNT(l)
        FROM NotificationLogEntity l
        WHERE l.workspaceId = :workspaceId
          AND l.createdAt >= :from
          AND l.createdAt < :to
        GROUP BY l.channelType, l.status
    """)
    fun countByChannelAndStatus(
        @Param("workspaceId") workspaceId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): List<Array<Any>>
}
```

#### NotificationProcessedEventJpaRepository

```kotlin
interface NotificationProcessedEventJpaRepository
    : JpaRepository<NotificationProcessedEventEntity, String> {

    fun existsByEventId(eventId: String): Boolean

    /**
     * 오래된 처리 이벤트 정리 (30일 이상)
     */
    @Modifying
    @Query("""
        DELETE FROM NotificationProcessedEventEntity e
        WHERE e.createdAt < :before
    """)
    fun deleteOlderThan(@Param("before") before: LocalDateTime): Int
}
```

### 2.2 Domain Port Out <-> JPA Adapter 매핑

| Domain Port (out) | JPA Adapter | JPA Repository |
|---|---|---|
| `NotificationRepository` | `NotificationRepositoryAdapter` | `NotificationJpaRepository` |
| `NotificationSettingRepository` | `NotificationSettingRepositoryAdapter` | `NotificationSettingJpaRepository` |
| `NotificationOverrideRepository` | `NotificationOverrideRepositoryAdapter` | `NotificationSettingOverrideJpaRepository` |
| `NotificationTemplateRepository` | `NotificationTemplateRepositoryAdapter` | `NotificationTemplateJpaRepository` |
| `NotificationScheduleRepository` | `NotificationScheduleRepositoryAdapter` | `NotificationScheduleJpaRepository` |
| `NotificationSubscriptionRepository` | `NotificationSubscriptionRepositoryAdapter` | `NotificationSubscriptionJpaRepository` |
| `NotificationLogRepository` | `NotificationLogRepositoryAdapter` | `NotificationLogJpaRepository` |
| `ProcessedEventRepository` | `ProcessedEventRepositoryAdapter` | `NotificationProcessedEventJpaRepository` |

### 2.3 Adapter 구현 (대표: NotificationRepositoryAdapter)

```kotlin
@Repository
class NotificationRepositoryAdapter(
    private val jpaRepository: NotificationJpaRepository
) : NotificationRepository {

    override fun save(notification: Notification): Notification {
        val entity = NotificationMapper.toEntity(notification)
        val saved = jpaRepository.save(entity)
        return NotificationMapper.toDomain(saved)
    }

    override fun findById(id: Long): Notification? {
        return jpaRepository.findById(id)
            .orElse(null)
            ?.let { NotificationMapper.toDomain(it) }
    }

    override fun findByRecipient(
        workspaceId: Long,
        userId: Long,
        statuses: List<NotificationStatus>,
        pageable: Pageable
    ): Page<Notification> {
        return jpaRepository
            .findByWorkspaceIdAndRecipientUserIdAndStatusInOrderByCreatedAtDesc(
                workspaceId, userId, statuses, pageable
            )
            .map { NotificationMapper.toDomain(it) }
    }

    override fun findByRecipientAndCategory(
        workspaceId: Long,
        userId: Long,
        category: NotificationCategory,
        statuses: List<NotificationStatus>,
        pageable: Pageable
    ): Page<Notification> {
        return jpaRepository
            .findByWorkspaceIdAndRecipientUserIdAndCategoryAndStatusInOrderByCreatedAtDesc(
                workspaceId, userId, category, statuses, pageable
            )
            .map { NotificationMapper.toDomain(it) }
    }

    override fun countUnread(workspaceId: Long, userId: Long): Long {
        return jpaRepository.countByWorkspaceIdAndRecipientUserIdAndStatus(
            workspaceId, userId, NotificationStatus.UNREAD
        )
    }

    override fun markAsRead(id: Long): Int {
        return jpaRepository.markAsRead(id, LocalDateTime.now())
    }

    override fun markAllAsRead(workspaceId: Long, userId: Long): Int {
        return jpaRepository.markAllAsRead(workspaceId, userId, LocalDateTime.now())
    }

    override fun findBySource(sourceType: SourceType, sourceId: Long): List<Notification> {
        return jpaRepository.findBySourceTypeAndSourceId(sourceType, sourceId)
            .map { NotificationMapper.toDomain(it) }
    }

    override fun archiveExpired(): Int {
        return jpaRepository.archiveExpired(LocalDateTime.now())
    }
}
```

### 2.4 Mapper (대표: NotificationMapper)

```kotlin
object NotificationMapper {

    fun toDomain(entity: NotificationEntity): Notification = Notification(
        id = entity.id,
        workspaceId = entity.workspaceId,
        recipientUserId = entity.recipientUserId,
        type = entity.notificationType,
        category = entity.category,
        priority = entity.priority,
        title = entity.title,
        body = entity.body,
        sourceReference = if (entity.sourceType != null) {
            SourceReference(entity.sourceType, entity.sourceId!!)
        } else null,
        openingId = entity.openingId,
        processOnOpeningId = entity.processOnOpeningId,
        status = entity.status,
        actionUrl = entity.actionUrl,
        readAt = entity.readAt,
        expiredAt = entity.expiredAt,
        createdAt = entity.createdAt
    )

    fun toEntity(domain: Notification): NotificationEntity = NotificationEntity(
        id = domain.id,
        workspaceId = domain.workspaceId,
        recipientUserId = domain.recipientUserId,
        notificationType = domain.type,
        category = domain.category,
        priority = domain.priority,
        title = domain.title,
        body = domain.body,
        sourceType = domain.sourceReference?.sourceType,
        sourceId = domain.sourceReference?.sourceId,
        openingId = domain.openingId,
        processOnOpeningId = domain.processOnOpeningId,
        status = domain.status,
        actionUrl = domain.actionUrl,
        readAt = domain.readAt,
        expiredAt = domain.expiredAt,
        createdAt = domain.createdAt
    )
}

// NotificationSettingMapper, NotificationScheduleMapper 등 7개 동일 패턴
```

### 2.5 Read/Write DataSource 분리

```kotlin
@Configuration
class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.write")
    fun writeDataSource(): DataSource = DataSourceBuilder.create().build()

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.read")
    fun readDataSource(): DataSource = DataSourceBuilder.create().build()

    @Bean
    @Primary
    fun routingDataSource(
        @Qualifier("writeDataSource") write: DataSource,
        @Qualifier("readDataSource") read: DataSource
    ): DataSource = ReadWriteRoutingDataSource(write, read)
}

class ReadWriteRoutingDataSource(
    private val writeDataSource: DataSource,
    private val readDataSource: DataSource
) : AbstractRoutingDataSource() {

    init {
        setTargetDataSources(mapOf<Any, Any>(
            "write" to writeDataSource,
            "read" to readDataSource
        ))
        setDefaultTargetDataSource(writeDataSource)
    }

    override fun determineCurrentLookupKey(): Any {
        return if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            "read"
        } else {
            "write"
        }
    }
}
```

---

## 3. Kafka Consumer / Producer

### 3.1 Consumer: event.notification.* 토픽 소비

#### KafkaConfig (Confluent Cloud SASL_SSL)

```kotlin
@Configuration
@EnableKafka
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
    @Value("\${spring.kafka.properties.sasl.jaas.config}") private val saslJaasConfig: String
) {

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 50,
            // SASL_SSL (Confluent Cloud)
            "security.protocol" to "SASL_SSL",
            "sasl.mechanism" to "PLAIN",
            "sasl.jaas.config" to saslJaasConfig
        )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setConcurrency(3)
        factory.setCommonErrorHandler(
            DefaultErrorHandler(
                DeadLetterPublishingRecoverer(kafkaTemplate()),
                FixedBackOff(1000L, 3)  // 1초 간격 3회 재시도 후 DLQ
            )
        )
        return factory
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        val props = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            // SASL_SSL
            "security.protocol" to "SASL_SSL",
            "sasl.mechanism" to "PLAIN",
            "sasl.jaas.config" to saslJaasConfig
        )
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }
}
```

#### NotificationEventConsumer

```kotlin
@Component
class NotificationEventConsumer(
    private val notificationProcessor: NotificationProcessor,
    private val processedEventRepository: ProcessedEventRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // --------------------------------------------------
    // event.notification.evaluation-submitted.v1
    // --------------------------------------------------
    @KafkaListener(
        topics = ["event.notification.evaluation-submitted.v1"],
        groupId = "notification-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeEvaluationSubmitted(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment
    ) {
        processWithIdempotency(record) { envelope ->
            val payload = objectMapper.convertValue(
                envelope.payload, EvaluationSubmittedPayload::class.java
            )
            notificationProcessor.processEvaluationSubmitted(payload)
        }
        ack.acknowledge()
    }

    // --------------------------------------------------
    // event.notification.evaluation-completed.v1
    // --------------------------------------------------
    @KafkaListener(
        topics = ["event.notification.evaluation-completed.v1"],
        groupId = "notification-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeEvaluationCompleted(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment
    ) {
        processWithIdempotency(record) { envelope ->
            val payload = objectMapper.convertValue(
                envelope.payload, EvaluationCompletedPayload::class.java
            )
            notificationProcessor.processEvaluationCompleted(payload)
        }
        ack.acknowledge()
    }

    // --------------------------------------------------
    // event.notification.stage-entered.v1
    // --------------------------------------------------
    @KafkaListener(
        topics = ["event.notification.stage-entered.v1"],
        groupId = "notification-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeStageEntered(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment
    ) {
        processWithIdempotency(record) { envelope ->
            val payload = objectMapper.convertValue(
                envelope.payload, StageEnteredPayload::class.java
            )
            notificationProcessor.processStageEntered(payload)
        }
        ack.acknowledge()
    }

    // --------------------------------------------------
    // event.notification.remind-schedule.v1
    // --------------------------------------------------
    @KafkaListener(
        topics = ["event.notification.remind-schedule.v1"],
        groupId = "notification-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeRemindSchedule(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment
    ) {
        processWithIdempotency(record) { envelope ->
            val payload = objectMapper.convertValue(
                envelope.payload, RemindSchedulePayload::class.java
            )
            notificationProcessor.processRemindSchedule(payload)
        }
        ack.acknowledge()
    }

    // --------------------------------------------------
    // queue.notification.send.v1 (자기 자신이 Produce한 발송 명령)
    // --------------------------------------------------
    @KafkaListener(
        topics = ["queue.notification.send.v1"],
        groupId = "notification-sender",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeSendCommand(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment
    ) {
        processWithIdempotency(record) { envelope ->
            val payload = objectMapper.convertValue(
                envelope.payload, SendNotificationCommandPayload::class.java
            )
            notificationProcessor.executeSend(payload)
        }
        ack.acknowledge()
    }

    // --------------------------------------------------
    // 멱등성 보장: notification_processed_events 테이블 기반
    // --------------------------------------------------
    private fun processWithIdempotency(
        record: ConsumerRecord<String, String>,
        handler: (NotificationEventEnvelope) -> Unit
    ) {
        val envelope = objectMapper.readValue(record.value(), NotificationEventEnvelope::class.java)
        val eventId = envelope.eventId

        // 1차: DB 테이블 중복 체크
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("[Idempotency] Duplicate event skipped: eventId={}, topic={}", eventId, record.topic())
            return
        }

        try {
            handler(envelope)
            processedEventRepository.save(eventId, record.topic(), ProcessedEventStatus.PROCESSED)
        } catch (e: Exception) {
            processedEventRepository.save(eventId, record.topic(), ProcessedEventStatus.FAILED)
            throw e  // ErrorHandler가 DLQ로 전송
        }
    }
}
```

#### 이벤트 Envelope DTO

```kotlin
/**
 * 모든 Kafka 메시지의 공통 Envelope
 */
data class NotificationEventEnvelope(
    val eventId: String,
    val eventType: String,
    val version: String,
    val timestamp: String,
    val source: String,
    val payload: Map<String, Any>
)

// --- 개별 Payload DTO ---

data class EvaluationSubmittedPayload(
    val workspaceId: Long,
    val applicantId: Long,
    val applicantName: String,
    val evaluatorUserId: Long,
    val evaluatorName: String,
    val evaluationId: Long,
    val stageId: Long,
    val stageName: String,
    val jobPostingId: Long,
    val jobPostingTitle: String,
    val submittedAt: String
)

data class EvaluationCompletedPayload(
    val workspaceId: Long,
    val applicantId: Long,
    val applicantName: String,
    val stageId: Long,
    val stageName: String,
    val jobPostingId: Long,
    val jobPostingTitle: String,
    val totalEvaluatorCount: Int,
    val completedEvaluatorCount: Int,
    val averageScore: Double,
    val completedAt: String
)

data class StageEnteredPayload(
    val workspaceId: Long,
    val applicantId: Long,
    val applicantName: String,
    val jobPostingId: Long,
    val jobPostingTitle: String,
    val fromStageId: Long,
    val fromStageName: String,
    val toStageId: Long,
    val toStageName: String,
    val movedByUserId: Long,
    val movedByUserName: String,
    val enteredAt: String
)

data class RemindSchedulePayload(
    val scheduleId: Long,
    val workspaceId: Long,
    val remindType: String,
    val targetId: Long,
    val targetType: String,
    val scheduledAt: String,
    val templateId: String?,
    val remindCount: Int,
    val maxRemindCount: Int,
    val recipientUserIds: List<Long>
)

data class SendNotificationCommandPayload(
    val notificationId: Long,
    val workspaceId: Long,
    val recipientUserId: Long,
    val recipientEmail: String?,
    val channels: List<String>,
    val notificationType: String,
    val category: String,
    val title: String,
    val content: String?,
    val metadata: Map<String, Any>?,
    val templateId: String?,
    val templateVariables: Map<String, String>?
)
```

### 3.2 Producer: 발송 큐 토픽 발행

```kotlin
@Component
class NotificationKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 내부 발송 큐: queue.notification.send.v1
     * NotificationEngine -> ChannelSender 분기를 위한 중간 큐
     */
    fun publishSendCommand(command: SendNotificationCommandPayload) {
        val envelope = NotificationEventEnvelope(
            eventId = UUID.randomUUID().toString(),
            eventType = "SEND_NOTIFICATION",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "notification-service",
            payload = objectMapper.convertValue(command, object : TypeReference<Map<String, Any>>() {})
        )
        kafkaTemplate.send(
            "queue.notification.send.v1",
            command.recipientUserId.toString(),
            objectMapper.writeValueAsString(envelope)
        ).whenComplete { result, ex ->
            if (ex != null) {
                log.error("[KafkaProducer] Failed to send command: notificationId={}", command.notificationId, ex)
            }
        }
    }

    /**
     * 이메일 발송 큐 -> doodlin-communication
     */
    fun publishMailSend(event: MailSendEvent) {
        kafkaTemplate.send(
            "queue.mail.send",
            event.recipientEmail,
            objectMapper.writeValueAsString(event)
        )
    }

    /**
     * Slack 발송 큐 -> doodlin-communication
     */
    fun publishSlackSend(event: SlackSendEvent) {
        kafkaTemplate.send(
            "queue.slack.send",
            event.channelId,
            objectMapper.writeValueAsString(event)
        )
    }
}

// --- Producer Event DTO ---

data class MailSendEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val workspaceId: Long,
    val notificationId: Long?,
    val recipientUserId: Long,
    val recipientEmail: String,
    val subject: String,
    val body: String,
    val notificationType: String,
    val timestamp: String = Instant.now().toString()
)

data class SlackSendEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val workspaceId: Long,
    val notificationId: Long?,
    val recipientUserId: Long,
    val channelId: String,
    val message: String,
    val blocks: List<Map<String, Any>>? = null,
    val notificationType: String,
    val timestamp: String = Instant.now().toString()
)
```

---

## 4. Redis Pub/Sub

### 4.1 AS-IS vs TO-BE

| 항목 | AS-IS (greeting-notification-server) | TO-BE (greeting-notification-service) |
|------|--------------------------------------|---------------------------------------|
| 라이브러리 | `@socket.io/redis-emitter` | Spring `RedisTemplate` 표준 Pub/Sub |
| 프로토콜 | Socket.io 전용 바이너리 프로토콜 (msgpack) | JSON 직렬화 표준 메시지 |
| 채널 패턴 | `socket.io#/{namespace}#` | `notification:{workspaceId}-{userId}` |
| 멀티인스턴스 | Socket.io Redis Adapter 자동 | 명시적 Redis Pub/Sub + 로컬 세션 관리 |

### 4.2 Redis 설정 (LettuceConnectionFactory + TLS)

```kotlin
@Configuration
class RedisConfig(
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port}") private val port: Int,
    @Value("\${spring.data.redis.password}") private val password: String,
    @Value("\${spring.data.redis.ssl.enabled:true}") private val sslEnabled: Boolean
) {

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val config = RedisStandaloneConfiguration(host, port).apply {
            setPassword(RedisPassword.of(password))
        }

        val clientConfig = LettuceClientConfiguration.builder().apply {
            if (sslEnabled) {
                useSsl().disablePeerVerification()  // AWS ElastiCache TLS
            }
            commandTimeout(Duration.ofSeconds(5))
        }.build()

        return LettuceConnectionFactory(config, clientConfig)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): StringRedisTemplate {
        return StringRedisTemplate(connectionFactory)
    }
}
```

### 4.3 Pub/Sub 채널 설계

```
채널 패턴: notification:{workspaceId}-{userId}

예시:
  - notification:12345-111   (workspace 12345의 user 111)
  - notification:12345-222   (workspace 12345의 user 222)
```

### 4.4 Publisher (알림 발송 시)

```kotlin
@Component
class RedisNotificationPublisher(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val localSessionRegistry: LocalWebSocketSessionRegistry
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 알림을 Redis Pub/Sub로 발행.
     * 같은 인스턴스에 해당 사용자 세션이 있으면 직접 전달 (Redis 경유 생략).
     * 없으면 Redis Pub/Sub로 다른 인스턴스가 전달하도록 위임.
     */
    fun publish(message: SocketNotificationMessage) {
        val channel = "notification:${message.workspaceId}-${message.userId}"

        // 1. 로컬 세션 우선 전달 시도
        val deliveredLocally = localSessionRegistry.sendToLocalSession(
            message.workspaceId, message.userId, message
        )

        // 2. 로컬에 없거나 멀티 세션 가능성 -> Redis 경유
        if (!deliveredLocally) {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message))
            log.debug("[RedisPub] Published to channel={}", channel)
        }
    }
}

/**
 * Redis Pub/Sub 메시지 DTO (JSON 직렬화)
 */
data class SocketNotificationMessage(
    val workspaceId: Long,
    val userId: Long,
    val eventName: String,      // "AlertAdded", "AlertRead", "msgToClient"
    val payload: Any            // 이벤트별 페이로드
)
```

### 4.5 Subscriber (다른 인스턴스 메시지 수신)

```kotlin
@Component
class RedisNotificationSubscriber(
    private val objectMapper: ObjectMapper,
    private val socketIoServer: SocketIOServer
) : MessageListener {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun onMessage(message: Message, pattern: ByteArray?) {
        try {
            val notification = objectMapper.readValue(
                message.body, SocketNotificationMessage::class.java
            )
            val room = "workspace:${notification.workspaceId}:user:${notification.userId}"

            // 이 인스턴스에 연결된 해당 room의 클라이언트들에게 전달
            socketIoServer.getRoomOperations(room)
                .sendEvent(notification.eventName, notification.payload)

            log.debug("[RedisSub] Delivered to room={}, event={}", room, notification.eventName)
        } catch (e: Exception) {
            log.error("[RedisSub] Failed to process message", e)
        }
    }
}
```

### 4.6 동적 채널 구독 관리

```kotlin
@Component
class RedisSubscriptionManager(
    private val redisMessageListenerContainer: RedisMessageListenerContainer,
    private val redisNotificationSubscriber: RedisNotificationSubscriber
) {
    private val activeSubscriptions = ConcurrentHashMap<String, ChannelTopic>()

    /**
     * 사용자가 WebSocket 연결 시 해당 채널 구독
     */
    fun subscribe(workspaceId: Long, userId: Long) {
        val channelName = "notification:$workspaceId-$userId"
        activeSubscriptions.computeIfAbsent(channelName) { name ->
            val topic = ChannelTopic(name)
            redisMessageListenerContainer.addMessageListener(redisNotificationSubscriber, topic)
            topic
        }
    }

    /**
     * 사용자가 WebSocket 연결 해제 시 해당 채널 구독 해제
     * (같은 사용자의 다른 세션이 없을 때만)
     */
    fun unsubscribe(workspaceId: Long, userId: Long) {
        val channelName = "notification:$workspaceId-$userId"
        activeSubscriptions.remove(channelName)?.let { topic ->
            redisMessageListenerContainer.removeMessageListener(redisNotificationSubscriber, topic)
        }
    }
}
```

---

## 5. WebSocket (netty-socketio)

### 5.1 AS-IS vs TO-BE

| 항목 | AS-IS (greeting-alert-server) | TO-BE (greeting-notification-service) |
|------|-------------------------------|---------------------------------------|
| 런타임 | NestJS + Socket.io v4 | Spring Boot + netty-socketio |
| 인증 | JWT handshake query param | JWT handshake query param (동일) |
| Transport | WebSocket + Polling | WEBSOCKET only (성능 최적화) |
| Redis 연동 | @socket.io/redis-adapter | Spring RedisTemplate Pub/Sub |
| 클라이언트 이벤트 | joinAlertChannel, leaveAlertChannel | 동일 (FE 변경 0) |
| 서버 이벤트 | AlertAdded, AlertRead, msgToClient | 동일 (FE 변경 0) |

### 5.2 SocketIOServer 설정

```kotlin
@Configuration
class SocketIoConfig(
    @Value("\${socketio.host:0.0.0.0}") private val host: String,
    @Value("\${socketio.port:9092}") private val port: Int,
    @Value("\${socketio.ping-timeout:60000}") private val pingTimeout: Int,
    @Value("\${socketio.ping-interval:25000}") private val pingInterval: Int
) {

    @Bean
    fun socketIoServer(authHandler: SocketIoAuthHandler): SocketIOServer {
        val config = com.corundumstudio.socketio.Configuration().apply {
            hostname = host
            this.port = port
            setOrigin("*")
            pingTimeout = this@SocketIoConfig.pingTimeout
            pingInterval = this@SocketIoConfig.pingInterval
            // Transport: WEBSOCKET only (Polling 비활성화로 성능 최적화)
            setTransports(Transport.WEBSOCKET)
            // JWT 인증
            authorizationListener = authHandler
            // Netty 워커 스레드
            workerThreads = Runtime.getRuntime().availableProcessors() * 2
        }
        return SocketIOServer(config)
    }

    @Bean
    fun socketIoServerLifecycle(server: SocketIOServer): SmartLifecycle {
        return object : SmartLifecycle {
            private var running = false
            override fun start() { server.start(); running = true }
            override fun stop() { server.stop(); running = false }
            override fun isRunning() = running
            override fun getPhase() = Int.MAX_VALUE  // Spring 컨텍스트 마지막에 시작
        }
    }
}
```

### 5.3 JWT 인증 (HS256, base64 디코딩 키)

```kotlin
@Component
class SocketIoAuthHandler(
    private val jwtProvider: JwtProvider
) : AuthorizationListener {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun isAuthorized(data: HandshakeData): Boolean {
        // FE 호환: query param "token" 으로 JWT 전달
        val token = data.getSingleUrlParam("token")
            ?: data.httpHeaders.get("Authorization")?.removePrefix("Bearer ")
            ?: run {
                log.warn("[WS Auth] No token provided")
                return false
            }

        return try {
            val claims = jwtProvider.validateAndParse(token)
            // handshake 데이터에 userId, workspaceId 저장 (이후 이벤트에서 사용)
            data.urlParams["userId"] = listOf(claims.userId.toString())
            data.urlParams["workspaceId"] = listOf(claims.workspaceId.toString())
            true
        } catch (e: ExpiredJwtException) {
            log.warn("[WS Auth] Token expired: {}", e.message)
            false
        } catch (e: Exception) {
            log.warn("[WS Auth] Invalid token: {}", e.message)
            false
        }
    }
}

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String
) {
    /**
     * jwt.secret: base64-encoded HS256 키
     * claim: "greetinghr.com/user_id" -> userId 추출
     */
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
    }

    fun validateAndParse(token: String): JwtClaims {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        val userId = (claims["greetinghr.com/user_id"] as Number).toLong()
        val workspaceId = (claims["greetinghr.com/workspace_id"] as Number?)?.toLong()

        return JwtClaims(
            userId = userId,
            workspaceId = workspaceId
        )
    }
}

data class JwtClaims(
    val userId: Long,
    val workspaceId: Long?
)
```

### 5.4 이벤트 핸들러

```kotlin
@Component
class SocketIoEventHandler(
    private val socketIoServer: SocketIOServer,
    private val redisSubscriptionManager: RedisSubscriptionManager,
    private val notificationQueryUseCase: QueryNotificationUseCase,
    private val localSessionRegistry: LocalWebSocketSessionRegistry
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        // --------------------------------------------------
        // Connect: 자동 Room 조인 + Redis 구독
        // --------------------------------------------------
        socketIoServer.addConnectListener { client ->
            val userId = client.handshakeData.getSingleUrlParam("userId")?.toLong()
                ?: return@addConnectListener
            val workspaceId = client.handshakeData.getSingleUrlParam("workspaceId")?.toLong()
                ?: return@addConnectListener

            val room = "workspace:$workspaceId:user:$userId"
            client.joinRoom(room)
            localSessionRegistry.register(workspaceId, userId, client.sessionId)
            redisSubscriptionManager.subscribe(workspaceId, userId)

            log.info("[WS] Connected: sessionId={}, room={}", client.sessionId, room)
        }

        // --------------------------------------------------
        // Disconnect: Room 떠나기 + Redis 구독 해제
        // --------------------------------------------------
        socketIoServer.addDisconnectListener { client ->
            val userId = client.handshakeData.getSingleUrlParam("userId")?.toLong()
            val workspaceId = client.handshakeData.getSingleUrlParam("workspaceId")?.toLong()

            if (userId != null && workspaceId != null) {
                localSessionRegistry.unregister(workspaceId, userId, client.sessionId)

                // 같은 사용자의 다른 세션이 없을 때만 Redis 구독 해제
                if (!localSessionRegistry.hasSession(workspaceId, userId)) {
                    redisSubscriptionManager.unsubscribe(workspaceId, userId)
                }
            }

            log.info("[WS] Disconnected: sessionId={}", client.sessionId)
        }

        // --------------------------------------------------
        // joinAlertChannel (FE 호환)
        // 기존 FE: socket.emit('joinAlertChannel', { groupId, lastAlertId })
        // --------------------------------------------------
        socketIoServer.addEventListener(
            "joinAlertChannel",
            JoinAlertChannelRequest::class.java
        ) { client, data, ackRequest ->
            val userId = client.handshakeData.getSingleUrlParam("userId")?.toLong()
                ?: return@addEventListener
            val workspaceId = data.groupId  // FE에서 groupId = workspaceId

            val room = "workspace:$workspaceId:user:$userId"
            client.joinRoom(room)
            redisSubscriptionManager.subscribe(workspaceId, userId)

            // lastAlertId 이후의 알림이 있으면 즉시 전달 (오프라인 동안 놓친 알림)
            if (data.lastAlertId != null) {
                // 미읽음 카운트 전달
                val unreadCount = notificationQueryUseCase.countUnread(workspaceId, userId)
                client.sendEvent("msgToClient", mapOf("unreadCount" to unreadCount))
            }

            ackRequest?.sendAckData("joined")
            log.info("[WS] joinAlertChannel: room={}, lastAlertId={}", room, data.lastAlertId)
        }

        // --------------------------------------------------
        // leaveAlertChannel (FE 호환)
        // 기존 FE: socket.emit('leaveAlertChannel', { groupId })
        // --------------------------------------------------
        socketIoServer.addEventListener(
            "leaveAlertChannel",
            LeaveAlertChannelRequest::class.java
        ) { client, data, ackRequest ->
            val userId = client.handshakeData.getSingleUrlParam("userId")?.toLong()
                ?: return@addEventListener
            val workspaceId = data.groupId

            val room = "workspace:$workspaceId:user:$userId"
            client.leaveRoom(room)

            if (!localSessionRegistry.hasSession(workspaceId, userId)) {
                redisSubscriptionManager.unsubscribe(workspaceId, userId)
            }

            ackRequest?.sendAckData("left")
            log.info("[WS] leaveAlertChannel: room={}", room)
        }
    }
}

// --- FE 호환 DTO ---

data class JoinAlertChannelRequest(
    val groupId: Long,          // workspaceId
    val lastAlertId: Long?      // 마지막으로 받은 알림 ID (오프라인 동안 놓친 알림 복구용)
)

data class LeaveAlertChannelRequest(
    val groupId: Long           // workspaceId
)
```

### 5.5 서버 -> 클라이언트 이벤트 (FE 변경 0)

```kotlin
/**
 * WebSocket을 통해 클라이언트에 전달하는 이벤트 목록.
 * 기존 greeting-alert-server와 동일한 이벤트명 + 페이로드 구조 유지.
 */
object SocketEvents {
    /**
     * 새 알림 추가됨
     * FE: socket.on('AlertAdded', (data) => { ... })
     */
    const val ALERT_ADDED = "AlertAdded"

    /**
     * 알림 읽음 처리됨
     * FE: socket.on('AlertRead', (data) => { ... })
     */
    const val ALERT_READ = "AlertRead"

    /**
     * 범용 메시지 (미읽음 카운트 등)
     * FE: socket.on('msgToClient', (data) => { ... })
     */
    const val MSG_TO_CLIENT = "msgToClient"
}

// --- AlertAdded 페이로드 (기존과 동일 구조) ---
data class AlertAddedPayload(
    val id: Long,
    val type: String,           // NotificationType
    val category: String,       // NotificationCategory
    val title: String,
    val body: String?,
    val actionUrl: String?,
    val metadata: Map<String, Any>?,
    val createdAt: String       // ISO-8601
)

// --- AlertRead 페이로드 ---
data class AlertReadPayload(
    val id: Long,
    val readAt: String
)

// --- msgToClient 페이로드 ---
data class MsgToClientPayload(
    val unreadCount: Long
)
```

### 5.6 LocalWebSocketSessionRegistry

```kotlin
@Component
class LocalWebSocketSessionRegistry {
    /**
     * key: "workspaceId:userId"
     * value: 해당 사용자의 이 인스턴스 내 활성 세션 ID 목록
     */
    private val sessions = ConcurrentHashMap<String, MutableSet<UUID>>()

    fun register(workspaceId: Long, userId: Long, sessionId: UUID) {
        val key = "$workspaceId:$userId"
        sessions.computeIfAbsent(key) { ConcurrentHashMap.newKeySet() }.add(sessionId)
    }

    fun unregister(workspaceId: Long, userId: Long, sessionId: UUID) {
        val key = "$workspaceId:$userId"
        sessions[key]?.let {
            it.remove(sessionId)
            if (it.isEmpty()) sessions.remove(key)
        }
    }

    fun hasSession(workspaceId: Long, userId: Long): Boolean {
        val key = "$workspaceId:$userId"
        return sessions[key]?.isNotEmpty() == true
    }

    /**
     * 로컬 세션에 직접 메시지 전달 (Redis 경유 없이)
     * @return 전달 성공 여부
     */
    fun sendToLocalSession(
        workspaceId: Long,
        userId: Long,
        message: SocketNotificationMessage
    ): Boolean {
        return hasSession(workspaceId, userId)
        // 실제 전달은 SocketIOServer.getRoomOperations()로 수행
        // 여기서는 세션 존재 여부만 확인
    }
}
```

---

## 6. Channel Sender 구현체

### 6.1 NotificationChannelSender 인터페이스

```kotlin
/**
 * domain/port/out/ChannelSender.kt
 *
 * 채널별 알림 발송 추상화.
 * supports() 패턴으로 Factory가 적절한 Sender를 선택한다.
 */
interface NotificationChannelSender {

    /**
     * 이 Sender가 지원하는 채널인지 확인
     */
    fun supports(channel: ChannelType): Boolean

    /**
     * 알림 발송 실행
     */
    fun send(notification: Notification, recipient: NotificationRecipient): SendResult

    /**
     * 채널 발송 우선순위 (낮을수록 먼저)
     */
    fun priority(): Int = 100
}

data class SendResult(
    val success: Boolean,
    val channel: ChannelType,
    val sentAt: Instant? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

data class NotificationRecipient(
    val userId: Long,
    val email: String?,
    val slackUserId: String?,
    val slackChannelId: String?,
    val name: String
)
```

### 6.2 InAppChannelSender

```kotlin
@Component
class InAppChannelSender(
    private val notificationRepository: NotificationRepository,
    private val redisNotificationPublisher: RedisNotificationPublisher,
    private val logRepository: NotificationLogRepository,
    private val redisTemplate: StringRedisTemplate
) : NotificationChannelSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(channel: ChannelType) = channel == ChannelType.IN_APP

    override fun priority() = 10  // 최우선 발송 (DB 저장 + 실시간 WebSocket)

    override fun send(notification: Notification, recipient: NotificationRecipient): SendResult {
        val idempotencyKey = "${notification.id}:${ChannelType.IN_APP}"

        try {
            // 1. DB 저장 (notifications 테이블)
            val saved = notificationRepository.save(notification)

            // 2. Redis 미읽음 카운트 증가
            redisTemplate.opsForValue().increment(
                "noti:unread:${saved.workspaceId}:${recipient.userId}"
            )

            // 3. Redis Pub/Sub -> WebSocket 실시간 전달
            redisNotificationPublisher.publish(
                SocketNotificationMessage(
                    workspaceId = saved.workspaceId,
                    userId = recipient.userId,
                    eventName = SocketEvents.ALERT_ADDED,
                    payload = AlertAddedPayload(
                        id = saved.id!!,
                        type = saved.type.name,
                        category = saved.category.name,
                        title = saved.title,
                        body = saved.body,
                        actionUrl = saved.actionUrl,
                        metadata = notification.metadata,
                        createdAt = saved.createdAt.toString()
                    )
                )
            )

            // 4. 발송 로그 기록
            logRepository.save(NotificationLog(
                notificationId = saved.id,
                workspaceId = saved.workspaceId,
                notificationType = saved.type,
                channelType = ChannelType.IN_APP,
                recipientUserId = recipient.userId,
                status = DeliveryStatus.SENT,
                idempotencyKey = idempotencyKey
            ))

            return SendResult(success = true, channel = ChannelType.IN_APP, sentAt = Instant.now())

        } catch (e: Exception) {
            log.error("[InApp] Send failed: notificationId={}", notification.id, e)
            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channelType = ChannelType.IN_APP,
                recipientUserId = recipient.userId,
                status = DeliveryStatus.FAILED,
                idempotencyKey = idempotencyKey,
                failureReason = e.message
            ))
            return SendResult(
                success = false, channel = ChannelType.IN_APP,
                errorCode = "INAPP_SEND_FAILED", errorMessage = e.message
            )
        }
    }
}
```

### 6.3 EmailChannelSender

```kotlin
@Component
class EmailChannelSender(
    private val kafkaProducer: NotificationKafkaProducer,
    private val templateRepository: NotificationTemplateRepository,
    private val logRepository: NotificationLogRepository
) : NotificationChannelSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(channel: ChannelType) = channel == ChannelType.EMAIL

    override fun priority() = 50

    override fun send(notification: Notification, recipient: NotificationRecipient): SendResult {
        val idempotencyKey = "${notification.id}:${ChannelType.EMAIL}"

        if (recipient.email == null) {
            return SendResult(
                success = false, channel = ChannelType.EMAIL,
                errorCode = "NO_EMAIL", errorMessage = "수신자 이메일 없음"
            )
        }

        try {
            // 1. 템플릿 조회 및 렌더링
            val template = templateRepository.findByWorkspaceAndType(
                notification.workspaceId, notification.type, ChannelType.EMAIL
            ) ?: templateRepository.findDefault(notification.type, ChannelType.EMAIL)

            val subject: String
            val body: String
            if (template != null && notification.metadata != null) {
                val variables = notification.metadata.mapValues { it.value.toString() }
                subject = renderTemplate(template.subjectTemplate, variables) ?: notification.title
                body = renderTemplate(template.bodyTemplate, variables) ?: notification.body ?: ""
            } else {
                subject = notification.title
                body = notification.body ?: ""
            }

            // 2. Kafka queue.mail.send -> doodlin-communication
            kafkaProducer.publishMailSend(MailSendEvent(
                workspaceId = notification.workspaceId,
                notificationId = notification.id,
                recipientUserId = recipient.userId,
                recipientEmail = recipient.email,
                subject = subject,
                body = body,
                notificationType = notification.type.name
            ))

            // 3. 발송 로그 (PENDING: Kafka 전송 성공, 실제 메일 발송은 doodlin-communication)
            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channelType = ChannelType.EMAIL,
                recipientUserId = recipient.userId,
                status = DeliveryStatus.PENDING,
                idempotencyKey = idempotencyKey
            ))

            return SendResult(success = true, channel = ChannelType.EMAIL, sentAt = Instant.now())

        } catch (e: Exception) {
            log.error("[Email] Send failed: notificationId={}", notification.id, e)
            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channelType = ChannelType.EMAIL,
                recipientUserId = recipient.userId,
                status = DeliveryStatus.FAILED,
                idempotencyKey = idempotencyKey,
                failureReason = e.message
            ))
            return SendResult(
                success = false, channel = ChannelType.EMAIL,
                errorCode = "EMAIL_SEND_FAILED", errorMessage = e.message
            )
        }
    }

    private fun renderTemplate(template: String?, variables: Map<String, String>): String? {
        if (template == null) return null
        var result = template
        variables.forEach { (key, value) ->
            result = result!!.replace("{{$key}}", value)
        }
        return result
    }
}
```

### 6.4 SlackChannelSender

```kotlin
@Component
class SlackChannelSender(
    private val kafkaProducer: NotificationKafkaProducer,
    private val logRepository: NotificationLogRepository
) : NotificationChannelSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(channel: ChannelType) = channel == ChannelType.SLACK

    override fun priority() = 60

    override fun send(notification: Notification, recipient: NotificationRecipient): SendResult {
        val idempotencyKey = "${notification.id}:${ChannelType.SLACK}"

        if (recipient.slackChannelId == null) {
            return SendResult(
                success = false, channel = ChannelType.SLACK,
                errorCode = "NO_SLACK_CHANNEL", errorMessage = "Slack 채널 미설정"
            )
        }

        try {
            // Kafka queue.slack.send -> doodlin-communication
            kafkaProducer.publishSlackSend(SlackSendEvent(
                workspaceId = notification.workspaceId,
                notificationId = notification.id,
                recipientUserId = recipient.userId,
                channelId = recipient.slackChannelId,
                message = notification.body ?: notification.title,
                notificationType = notification.type.name
            ))

            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channelType = ChannelType.SLACK,
                recipientUserId = recipient.userId,
                status = DeliveryStatus.PENDING,
                idempotencyKey = idempotencyKey
            ))

            return SendResult(success = true, channel = ChannelType.SLACK, sentAt = Instant.now())

        } catch (e: Exception) {
            log.error("[Slack] Send failed: notificationId={}", notification.id, e)
            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channelType = ChannelType.SLACK,
                recipientUserId = recipient.userId,
                status = DeliveryStatus.FAILED,
                idempotencyKey = idempotencyKey,
                failureReason = e.message
            ))
            return SendResult(
                success = false, channel = ChannelType.SLACK,
                errorCode = "SLACK_SEND_FAILED", errorMessage = e.message
            )
        }
    }
}
```

### 6.5 NotificationChannelSenderFactory

```kotlin
@Component
class NotificationChannelSenderFactory(
    private val senders: List<NotificationChannelSender>
) {

    /**
     * supports() 패턴으로 채널에 매칭되는 Sender 조회
     */
    fun getSender(channel: ChannelType): NotificationChannelSender {
        return senders.find { it.supports(channel) }
            ?: throw IllegalArgumentException("No sender registered for channel: $channel")
    }

    /**
     * 활성화된 채널 목록에 대해 priority 순으로 정렬된 Sender 목록 반환
     */
    fun getSenders(channels: List<ChannelType>): List<NotificationChannelSender> {
        return senders
            .filter { sender -> channels.any { sender.supports(it) } }
            .sortedBy { it.priority() }
    }
}
```

---

## 7. application.yml 주요 설정

```yaml
# ============================================================
# Server
# ============================================================
server:
  port: 8080
  shutdown: graceful

spring:
  application:
    name: greeting-notification-service

  # ============================================================
  # DataSource (Write / Read 분리)
  # ============================================================
  datasource:
    write:
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://${DB_WRITE_HOST:localhost}:${DB_WRITE_PORT:3306}/${DB_NAME:greeting_notification}?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      username: ${DB_WRITE_USERNAME:root}
      password: ${DB_WRITE_PASSWORD:}
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 5000
        idle-timeout: 300000
        max-lifetime: 600000
    read:
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://${DB_READ_HOST:localhost}:${DB_READ_PORT:3306}/${DB_NAME:greeting_notification}?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      username: ${DB_READ_USERNAME:root}
      password: ${DB_READ_PASSWORD:}
      hikari:
        maximum-pool-size: 30
        minimum-idle: 10
        connection-timeout: 5000

  # ============================================================
  # JPA
  # ============================================================
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway 마이그레이션 사용, 자동 DDL 비활성화
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
        default_batch_fetch_size: 100
        jdbc:
          batch_size: 50
    open-in-view: false

  # ============================================================
  # Kafka (Confluent Cloud, SASL_SSL)
  # ============================================================
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:pkc-xxxxx.ap-northeast-2.aws.confluent.cloud:9092}
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: >-
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_API_KEY}"
        password="${KAFKA_API_SECRET}";
    consumer:
      group-id: notification-consumer
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      properties:
        max.poll.records: 50
        session.timeout.ms: 30000
        heartbeat.interval.ms: 10000
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
        retries: 3

  # ============================================================
  # Redis (ElastiCache, TLS)
  # ============================================================
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      ssl:
        enabled: ${REDIS_SSL_ENABLED:true}
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 3000ms

# ============================================================
# Socket.io (netty-socketio)
# ============================================================
socketio:
  host: 0.0.0.0
  port: ${SOCKETIO_PORT:9092}
  ping-timeout: 60000
  ping-interval: 25000

# ============================================================
# JWT
# ============================================================
jwt:
  secret: ${JWT_SECRET}  # base64-encoded HS256 key

# ============================================================
# Logging
# ============================================================
logging:
  level:
    com.doodlin.greeting.notification: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.apache.kafka: WARN
    io.lettuce.core: WARN

# ============================================================
# Actuator
# ============================================================
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
  health:
    kafka:
      enabled: true
    redis:
      enabled: true
    db:
      enabled: true
```

---

## 부록: 전체 infrastructure 모듈 파일 목록

| 패키지 | 파일 | 설명 |
|--------|------|------|
| persistence/entity | NotificationEntity.kt | 알림 본체 JPA Entity |
| persistence/entity | NotificationSettingEntity.kt | 알림 설정 |
| persistence/entity | NotificationSettingOverrideEntity.kt | 설정 오버라이드 |
| persistence/entity | NotificationSubscriptionEntity.kt | 구독 |
| persistence/entity | NotificationTemplateEntity.kt | 템플릿 |
| persistence/entity | NotificationScheduleEntity.kt | 스케줄 |
| persistence/entity | NotificationLogEntity.kt | 발송 로그 |
| persistence/entity | NotificationProcessedEventEntity.kt | 멱등성 이벤트 |
| persistence/converter | JsonMapConverter.kt | JSON Map 변환 |
| persistence/converter | TemplateVariableListConverter.kt | 템플릿 변수 변환 |
| persistence/repository | NotificationJpaRepository.kt | 알림 JPA 인터페이스 |
| persistence/repository | NotificationSettingJpaRepository.kt | 설정 JPA |
| persistence/repository | NotificationSettingOverrideJpaRepository.kt | 오버라이드 JPA |
| persistence/repository | NotificationSubscriptionJpaRepository.kt | 구독 JPA |
| persistence/repository | NotificationTemplateJpaRepository.kt | 템플릿 JPA |
| persistence/repository | NotificationScheduleJpaRepository.kt | 스케줄 JPA |
| persistence/repository | NotificationLogJpaRepository.kt | 로그 JPA |
| persistence/repository | NotificationProcessedEventJpaRepository.kt | 멱등성 JPA |
| persistence/mapper | NotificationMapper.kt | 알림 domain <-> entity 변환 |
| persistence/mapper | (6개 추가 Mapper) | 각 도메인 모델별 Mapper |
| persistence/adapter | NotificationRepositoryAdapter.kt | Port Out 구현체 |
| persistence/adapter | (7개 추가 Adapter) | 각 Port Out 구현체 |
| persistence/config | DataSourceConfig.kt | Write/Read DataSource 분리 |
| persistence/config | ReadWriteRoutingDataSource.kt | 라우팅 DataSource |
| kafka/config | KafkaConfig.kt | SASL_SSL Consumer/Producer 설정 |
| kafka/consumer | NotificationEventConsumer.kt | 5개 토픽 Consumer |
| kafka/producer | NotificationKafkaProducer.kt | 발송 큐 Producer |
| kafka/event | NotificationEventEnvelope.kt | 공통 Envelope DTO |
| kafka/event | *Payload.kt | 개별 이벤트 Payload DTO (5개) |
| kafka/event | MailSendEvent.kt | 메일 발송 이벤트 |
| kafka/event | SlackSendEvent.kt | Slack 발송 이벤트 |
| redis/config | RedisConfig.kt | Lettuce + TLS 설정 |
| redis | RedisNotificationPublisher.kt | Pub/Sub Publisher |
| redis | RedisNotificationSubscriber.kt | Pub/Sub Subscriber |
| redis | RedisSubscriptionManager.kt | 동적 채널 구독 관리 |
| websocket/config | SocketIoConfig.kt | netty-socketio 서버 설정 |
| websocket/auth | SocketIoAuthHandler.kt | WebSocket JWT 인증 |
| websocket/auth | JwtProvider.kt | HS256 JWT 파서 |
| websocket/handler | SocketIoEventHandler.kt | 이벤트 핸들러 |
| websocket/handler | LocalWebSocketSessionRegistry.kt | 로컬 세션 관리 |
| websocket/dto | SocketNotificationMessage.kt | Redis Pub/Sub 메시지 |
| websocket/dto | SocketEvents.kt | 이벤트명 상수 |
| websocket/dto | AlertAddedPayload.kt | AlertAdded 페이로드 |
| websocket/dto | JoinAlertChannelRequest.kt | FE 호환 요청 DTO |
| sender | InAppChannelSender.kt | InApp 채널 구현체 |
| sender | EmailChannelSender.kt | Email 채널 구현체 |
| sender | SlackChannelSender.kt | Slack 채널 구현체 |
| sender | NotificationChannelSenderFactory.kt | Sender Factory |
