# [GRT-4004] JPA Entity + Repository Adapter 구현

## 개요
- PRD: https://doodlin.atlassian.net/wiki/x/SICjdg
- Phase: 1 (서비스 구축)
- 예상 공수: 3d
- 의존성: GRT-4002, GRT-4003

**범위:** infrastructure 모듈 — JPA Entity, Spring Data JPA Repository, 도메인-Entity 매퍼, Repository Adapter 구현

## 작업 내용

### 1. JPA Entity 전체 구현

#### NotificationEntity
```kotlin
@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val workspaceId: Long,
    val recipientUserId: Long,
    @Enumerated(EnumType.STRING) val notificationType: NotificationType,
    @Enumerated(EnumType.STRING) val notificationCategory: NotificationCategory,
    @Enumerated(EnumType.STRING) val channel: NotificationChannel,
    val title: String,
    @Column(columnDefinition = "TEXT") val content: String?,
    @Column(columnDefinition = "JSON") @Convert(converter = JsonMapConverter::class) val metadata: Map<String, Any>?,
    @Enumerated(EnumType.STRING) val sourceType: SourceType?,
    val sourceId: Long?,
    var isRead: Boolean = false,
    var readAt: LocalDateTime? = null,
    val expireAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

#### 나머지 Entity (동일 패턴)
- `NotificationSettingEntity`, `NotificationSubscriptionEntity`, `NotificationTemplateEntity`, `NotificationScheduleEntity`, `NotificationLogEntity`, `ConsumedEventEntity`
- JSON 컬럼: `@Convert(converter = JsonMapConverter::class)` 또는 `@Convert(converter = TemplateVariableListConverter::class)` 적용

### 2. Spring Data JPA Repository

```kotlin
interface NotificationJpaRepository : JpaRepository<NotificationEntity, Long> {
    fun findByWorkspaceIdAndRecipientUserIdOrderByCreatedAtDesc(
        workspaceId: Long, recipientUserId: Long, pageable: Pageable
    ): Page<NotificationEntity>

    fun countByWorkspaceIdAndRecipientUserIdAndIsReadFalse(
        workspaceId: Long, recipientUserId: Long
    ): Long

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :now WHERE n.id = :id")
    fun markAsRead(id: Long, now: LocalDateTime)

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :now WHERE n.workspaceId = :workspaceId AND n.recipientUserId = :userId AND n.isRead = false")
    fun markAllAsRead(workspaceId: Long, userId: Long, now: LocalDateTime)
}

interface NotificationSettingJpaRepository : JpaRepository<NotificationSettingEntity, Long> {
    fun findByWorkspaceIdAndNotificationTypeAndChannel(
        workspaceId: Long, type: NotificationType, channel: NotificationChannel
    ): NotificationSettingEntity?

    fun findAllByWorkspaceId(workspaceId: Long): List<NotificationSettingEntity>

    fun findByNotificationTypeAndEnabledTrue(type: NotificationType): List<NotificationSettingEntity>
}

interface NotificationSubscriptionJpaRepository : JpaRepository<NotificationSubscriptionEntity, Long> {
    fun findByWorkspaceIdAndUserIdAndNotificationType(
        workspaceId: Long, userId: Long, type: NotificationType
    ): List<NotificationSubscriptionEntity>

    fun findByWorkspaceIdAndNotificationTypeAndChannelAndEnabledTrue(
        workspaceId: Long, type: NotificationType, channel: NotificationChannel
    ): List<NotificationSubscriptionEntity>
}

interface NotificationScheduleJpaRepository : JpaRepository<NotificationScheduleEntity, Long> {
    @Query("SELECT s FROM NotificationScheduleEntity s WHERE s.status = 'PENDING' AND s.scheduledAt <= :now ORDER BY s.scheduledAt ASC")
    fun findPendingDue(now: LocalDateTime, pageable: Pageable): List<NotificationScheduleEntity>

    fun findByTargetTypeAndTargetId(targetType: TargetType, targetId: Long): List<NotificationScheduleEntity>

    @Modifying
    @Query("UPDATE NotificationScheduleEntity s SET s.status = 'CANCELLED', s.cancelledAt = :now, s.cancelReason = :reason WHERE s.targetType = :targetType AND s.targetId = :targetId AND s.status = 'PENDING'")
    fun cancelByTarget(targetType: TargetType, targetId: Long, reason: String, now: LocalDateTime): Int
}
```

### 3. 도메인-Entity 매퍼

```kotlin
object NotificationMapper {
    fun toDomain(entity: NotificationEntity): Notification = Notification(
        id = entity.id,
        workspaceId = entity.workspaceId,
        recipientUserId = entity.recipientUserId,
        type = entity.notificationType,
        category = entity.notificationCategory,
        channel = entity.channel,
        title = entity.title,
        content = entity.content,
        metadata = entity.metadata,
        sourceType = entity.sourceType,
        sourceId = entity.sourceId,
        isRead = entity.isRead,
        readAt = entity.readAt,
        expireAt = entity.expireAt,
        createdAt = entity.createdAt
    )

    fun toEntity(domain: Notification): NotificationEntity = NotificationEntity(
        id = domain.id,
        workspaceId = domain.workspaceId,
        recipientUserId = domain.recipientUserId,
        notificationType = domain.type,
        notificationCategory = domain.category,
        channel = domain.channel,
        title = domain.title,
        content = domain.content,
        metadata = domain.metadata,
        sourceType = domain.sourceType,
        sourceId = domain.sourceId,
        isRead = domain.isRead,
        readAt = domain.readAt,
        expireAt = domain.expireAt,
        createdAt = domain.createdAt
    )
}
// 나머지 5개 모델도 동일 패턴으로 Mapper 구현
```

### 4. Repository Adapter 구현

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
        return jpaRepository.findById(id).orElse(null)?.let { NotificationMapper.toDomain(it) }
    }

    override fun findByRecipient(workspaceId: Long, userId: Long, pageable: Pageable): Page<Notification> {
        return jpaRepository.findByWorkspaceIdAndRecipientUserIdOrderByCreatedAtDesc(workspaceId, userId, pageable)
            .map { NotificationMapper.toDomain(it) }
    }

    override fun countUnread(workspaceId: Long, userId: Long): Long {
        return jpaRepository.countByWorkspaceIdAndRecipientUserIdAndIsReadFalse(workspaceId, userId)
    }

    override fun markAsRead(id: Long) {
        jpaRepository.markAsRead(id, LocalDateTime.now())
    }

    override fun markAllAsRead(workspaceId: Long, userId: Long) {
        jpaRepository.markAllAsRead(workspaceId, userId, LocalDateTime.now())
    }
}
// 나머지 6개 Repository Adapter도 동일 패턴으로 구현
```

### 5. JSON Converter

```kotlin
@Converter
class JsonMapConverter : AttributeConverter<Map<String, Any>?, String?> {
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    override fun convertToDatabaseColumn(attribute: Map<String, Any>?): String? =
        attribute?.let { objectMapper.writeValueAsString(it) }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Any>? =
        dbData?.let { objectMapper.readValue(it, object : TypeReference<Map<String, Any>>() {}) }
}
```

### 6. Read/Write DataSource 설정

```kotlin
@Configuration
class DataSourceConfig {
    @Bean @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    fun writeDataSource(): DataSource = DataSourceBuilder.create().build()

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource-read")
    fun readDataSource(): DataSource = DataSourceBuilder.create().build()

    @Bean
    fun routingDataSource(
        @Qualifier("writeDataSource") write: DataSource,
        @Qualifier("readDataSource") read: DataSource
    ): DataSource = ReadWriteRoutingDataSource(write, read)
}
```

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-notification-service | infrastructure | src/.../infrastructure/persistence/entity/*.kt | 신규 (7개 Entity) |
| greeting-notification-service | infrastructure | src/.../infrastructure/persistence/repository/*JpaRepository.kt | 신규 (7개) |
| greeting-notification-service | infrastructure | src/.../infrastructure/persistence/mapper/*Mapper.kt | 신규 (7개) |
| greeting-notification-service | infrastructure | src/.../infrastructure/persistence/adapter/*RepositoryAdapter.kt | 신규 (7개) |
| greeting-notification-service | infrastructure | src/.../infrastructure/persistence/converter/JsonMapConverter.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/persistence/converter/TemplateVariableListConverter.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/config/DataSourceConfig.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/config/ReadWriteRoutingDataSource.kt | 신규 |

## 영향 범위

- infrastructure 모듈 내부 변경, 외부 서비스 영향 없음
- greeting-db 신규 테이블에 대한 JPA 매핑

## 테스트 케이스

| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-04-01 | Notification 저장/조회 | 빈 DB | save() 후 findById() | 동일 데이터 반환, ID 자동 생성 |
| TC-04-02 | 안읽은 알림 카운트 | 3건 저장 (2건 unread) | countUnread() | 2 반환 |
| TC-04-03 | 전체 읽음 처리 | 5건 unread | markAllAsRead() | 5건 모두 isRead=true |
| TC-04-04 | Setting UPSERT | 동일 (ws, type, channel) | 두 번 save | 유니크 제약 위반 없이 업데이트 |
| TC-04-05 | Schedule pending 조회 | PENDING 3건 (2건 due, 1건 미래) | findPendingDue(now) | 2건 반환 |
| TC-04-06 | Schedule cascade 취소 | MEETING/123 스케줄 2건 | cancelByTarget(MEETING, 123) | 2건 CANCELLED |
| TC-04-07 | ConsumedEvent 중복 체크 | eventId "abc" 저장됨 | existsByEventId("abc") | true |
| TC-04-08 | JSON 컬럼 매핑 | metadata = mapOf("key" to "value") | save 후 조회 | metadata 정상 역직렬화 |
| TC-04-09 | Read/Write 라우팅 | @Transactional(readOnly=true) | 쿼리 실행 | read DataSource 사용 |

## 기대 결과 (AC)

- [ ] 7개 JPA Entity 매핑 정상 (ddl-auto=validate 통과)
- [ ] 7개 Repository Adapter가 Port 인터페이스 구현
- [ ] 7개 Mapper의 domain ↔ entity 변환 정상
- [ ] JSON 컬럼 직렬화/역직렬화 정상
- [ ] Read/Write DataSource 라우팅 동작 확인
- [ ] Repository 통합 테스트 전체 통과 (@DataJpaTest + Testcontainers)

## 체크리스트

- [ ] @DataJpaTest + Testcontainers(MySQL) 기반 통합 테스트
- [ ] Entity에 @CreationTimestamp, @UpdateTimestamp 적용 검토
- [ ] N+1 문제 사전 방지 (fetch join 또는 @EntityGraph 검토)
- [ ] 대량 조회 시 Pageable 적용 확인
- [ ] 인덱스가 쿼리 플랜에 활용되는지 EXPLAIN 검증
