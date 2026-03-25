# [GRT-4008] 채널별 Sender 구현 (InApp/Email/Slack)

## 개요
- PRD: https://doodlin.atlassian.net/wiki/x/SICjdg
- Phase: 1 (서비스 구축)
- 예상 공수: 3d
- 의존성: GRT-4004, GRT-4005

**범위:** `NotificationChannelSender` 인터페이스 + 3개 구현체(InApp/Email/Slack). 채널별 독립 실패 처리 + 발송 로그 기록.

## 작업 내용

### 1. NotificationChannelSender 인터페이스

```kotlin
// application 모듈
interface NotificationChannelSender {
    fun channel(): NotificationChannel
    fun send(notification: Notification)
    fun sendBatch(notifications: List<Notification>)
}
```

### 2. NotificationChannelSenderFactory

```kotlin
@Component
class NotificationChannelSenderFactory(
    private val senders: List<NotificationChannelSender>
) {
    private val senderMap: Map<NotificationChannel, NotificationChannelSender> by lazy {
        senders.associateBy { it.channel() }
    }

    fun getSender(channel: NotificationChannel): NotificationChannelSender {
        return senderMap[channel] ?: throw IllegalArgumentException("No sender for channel: $channel")
    }
}
```

### 3. InAppChannelSender

```kotlin
@Component
class InAppChannelSender(
    private val notificationRepository: NotificationRepository,
    private val redisNotificationPublisher: RedisNotificationPublisher,
    private val logRepository: NotificationLogRepository
) : NotificationChannelSender {

    override fun channel() = NotificationChannel.IN_APP

    override fun send(notification: Notification) {
        try {
            // 1. DB 저장 (이미 저장된 경우 skip)
            val saved = if (notification.id == null) {
                notificationRepository.save(notification)
            } else notification

            // 2. Redis Pub/Sub로 실시간 전달 (WebSocket)
            redisNotificationPublisher.publish(SocketNotificationMessage(
                workspaceId = saved.workspaceId,
                userId = saved.recipientUserId,
                payload = InAppNotificationPayload(
                    id = saved.id!!,
                    type = saved.type.name,
                    category = saved.category.name,
                    title = saved.title,
                    content = saved.content,
                    metadata = saved.metadata,
                    createdAt = saved.createdAt.toString()
                )
            ))

            // 3. 발송 로그
            logRepository.save(NotificationLog(
                notificationId = saved.id,
                workspaceId = saved.workspaceId,
                notificationType = saved.type,
                channel = NotificationChannel.IN_APP,
                recipientUserId = saved.recipientUserId,
                status = LogStatus.SUCCESS
            ))
        } catch (e: Exception) {
            log.error("InApp send failed: notificationId=${notification.id}", e)
            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channel = NotificationChannel.IN_APP,
                recipientUserId = notification.recipientUserId,
                status = LogStatus.FAILED,
                errorMessage = e.message
            ))
        }
    }

    override fun sendBatch(notifications: List<Notification>) {
        notifications.forEach { send(it) }
    }
}
```

### 4. EmailChannelSender

```kotlin
@Component
class EmailChannelSender(
    private val kafkaProducer: NotificationKafkaProducer,
    private val templateRepository: NotificationTemplateRepository,
    private val logRepository: NotificationLogRepository
) : NotificationChannelSender {

    override fun channel() = NotificationChannel.EMAIL

    override fun send(notification: Notification) {
        try {
            // 1. 템플릿 조회 및 렌더링
            val template = templateRepository.findByWorkspaceAndType(
                notification.workspaceId, notification.type, NotificationChannel.EMAIL
            ) ?: templateRepository.findDefault(notification.type, NotificationChannel.EMAIL)

            val rendered = template?.render(notification.metadata?.mapValues { it.value.toString() } ?: emptyMap())

            // 2. Kafka queue.mail.send -> doodlin-communication
            kafkaProducer.publishMailSend(MailSendEvent(
                workspaceId = notification.workspaceId,
                recipientUserId = notification.recipientUserId,
                subject = rendered?.subject ?: notification.title,
                body = rendered?.body ?: notification.content ?: "",
                notificationType = notification.type.name
            ))

            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channel = NotificationChannel.EMAIL,
                recipientUserId = notification.recipientUserId,
                status = LogStatus.SUCCESS
            ))
        } catch (e: Exception) {
            log.error("Email send failed: notificationId=${notification.id}", e)
            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channel = NotificationChannel.EMAIL,
                recipientUserId = notification.recipientUserId,
                status = LogStatus.FAILED,
                errorMessage = e.message
            ))
        }
    }

    override fun sendBatch(notifications: List<Notification>) {
        notifications.forEach { send(it) }
    }
}
```

### 5. SlackChannelSender

```kotlin
@Component
class SlackChannelSender(
    private val kafkaProducer: NotificationKafkaProducer,
    private val logRepository: NotificationLogRepository
) : NotificationChannelSender {

    override fun channel() = NotificationChannel.SLACK

    override fun send(notification: Notification) {
        try {
            // Kafka queue.slack.send -> doodlin-communication
            kafkaProducer.publishSlackSend(SlackSendEvent(
                workspaceId = notification.workspaceId,
                recipientUserId = notification.recipientUserId,
                message = notification.content ?: notification.title,
                notificationType = notification.type.name
            ))

            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channel = NotificationChannel.SLACK,
                recipientUserId = notification.recipientUserId,
                status = LogStatus.SUCCESS
            ))
        } catch (e: Exception) {
            log.error("Slack send failed: notificationId=${notification.id}", e)
            logRepository.save(NotificationLog(
                notificationId = notification.id,
                workspaceId = notification.workspaceId,
                notificationType = notification.type,
                channel = NotificationChannel.SLACK,
                recipientUserId = notification.recipientUserId,
                status = LogStatus.FAILED,
                errorMessage = e.message
            ))
        }
    }

    override fun sendBatch(notifications: List<Notification>) {
        notifications.forEach { send(it) }
    }
}
```

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-notification-service | application | src/.../application/port/outbound/NotificationChannelSender.kt | 신규 |
| greeting-notification-service | application | src/.../application/service/NotificationChannelSenderFactory.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/sender/InAppChannelSender.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/sender/EmailChannelSender.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/sender/SlackChannelSender.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/sender/dto/MailSendEvent.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/sender/dto/SlackSendEvent.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/sender/dto/InAppNotificationPayload.kt | 신규 |

## 영향 범위

- doodlin-communication: queue.mail.send, queue.slack.send 토픽 소비 (기존 인터페이스 재사용, 변경 없음)
- Redis: socketio:notification 채널에 InApp 알림 publish

## 테스트 케이스

| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-08-01 | InApp 발송 | Notification 생성 | InAppChannelSender.send() | DB 저장 + Redis publish + 로그 SUCCESS |
| TC-08-02 | Email 발송 | 이메일 채널 알림 | EmailChannelSender.send() | queue.mail.send 토픽 발행 + 로그 SUCCESS |
| TC-08-03 | Slack 발송 | 슬랙 채널 알림 | SlackChannelSender.send() | queue.slack.send 토픽 발행 + 로그 SUCCESS |
| TC-08-04 | InApp 발송 실패 | Redis 연결 끊김 | InAppChannelSender.send() | DB 저장 성공, Redis 실패, 로그 FAILED |
| TC-08-05 | Email 발송 실패 | Kafka 연결 끊김 | EmailChannelSender.send() | 로그 FAILED, 예외 전파 안 됨 |
| TC-08-06 | Factory 채널 조회 | 3개 Sender 등록 | getSender(EMAIL) | EmailChannelSender 반환 |
| TC-08-07 | 템플릿 렌더링 후 이메일 | 커스텀 템플릿 존재 | EmailChannelSender.send() | 템플릿 변수 치환된 내용으로 발송 |
| TC-08-08 | Batch 발송 | 10건 알림 | sendBatch() | 10건 모두 발송 + 10건 로그 |

## 기대 결과 (AC)

- [ ] NotificationChannelSender 인터페이스 정의
- [ ] InAppChannelSender: DB 저장 + Redis Pub/Sub 정상
- [ ] EmailChannelSender: Kafka queue.mail.send 발행 정상
- [ ] SlackChannelSender: Kafka queue.slack.send 발행 정상
- [ ] 각 채널 발송 실패 시 로그 기록, 예외 전파 안 됨 (다른 채널 발송에 영향 없음)
- [ ] notification_logs 테이블에 발송 이력 기록

## 체크리스트

- [ ] 채널 간 독립 실패 처리 (하나 실패해도 나머지 발송)
- [ ] NotificationLog의 status: SUCCESS, FAILED, SKIPPED
- [ ] doodlin-communication의 메일/슬랙 메시지 포맷 확인 (기존 인터페이스 재사용)
- [ ] 향후 채널 추가 시 Sender 구현체만 추가하면 되도록 OCP 준수
