# [GRT-4005] Kafka Consumer/Producer 구현

## 개요
- PRD: https://doodlin.atlassian.net/wiki/x/SICjdg
- Phase: 1 (서비스 구축)
- 예상 공수: 4d
- 의존성: GRT-4003, GRT-4004

**범위:** Kafka Consumer로 알림 이벤트 토픽 소비 + Producer로 채널별 발송 큐 발행 + consumed_events 기반 멱등성 보장

## 작업 내용

### 1. Kafka Consumer: event.notification.* 토픽 소비

#### 소비 토픽 목록

| 토픽 | 키 | 페이로드 | 처리 |
|------|-----|---------|------|
| event.notification.evaluation-completed.v1 | applicantId | { eventId, workspaceId, applicantId, applicantName, stageName, completedCount, completedAt } | 평가 완료 알림 생성 |
| event.notification.evaluation-submitted.v1 | applicantId | { eventId, workspaceId, applicantId, evaluatorUserId, evaluationId, stageName, submittedAt } | 개별 평가 등록 알림 생성 |
| event.notification.stage-entry.v1 | applicantId | { eventId, workspaceId, applicantId, applicantName, fromStageId, toStageId, toStageName, enteredAt } | 전형 진입 알림 생성 |
| event.notification.interview-remind.v1 | meetingId | { eventId, workspaceId, meetingId, scheduledAt, action } | 면접 리마인드 스케줄 등록/취소/갱신 |
| event.notification.evaluation-remind.v1 | evaluationId | { eventId, workspaceId, evaluationId, scheduledAt, action } | 평가 리마인드 스케줄 등록/취소/갱신 |

#### Consumer 구현

```kotlin
@Component
class NotificationEventConsumer(
    private val notificationProcessor: NotificationProcessor,
    private val consumedEventRepository: ConsumedEventRepository
) {
    @KafkaListener(
        topics = ["event.notification.evaluation-completed.v1"],
        groupId = "notification-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeEvaluationCompleted(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment
    ) {
        val event = objectMapper.readValue(record.value(), EvaluationCompletedEvent::class.java)
        processWithIdempotency(event.eventId, record.topic()) {
            notificationProcessor.processEvaluationCompleted(event)
        }
        ack.acknowledge()
    }

    // 나머지 4개 토픽도 동일 패턴

    private fun processWithIdempotency(eventId: String, topic: String, block: () -> Unit) {
        if (consumedEventRepository.existsByEventId(eventId)) {
            log.info("Duplicate event skipped: eventId=$eventId, topic=$topic")
            return
        }
        block()
        consumedEventRepository.save(eventId, topic)
    }
}
```

### 2. NotificationProcessor: 알림 생성 → 설정 체크 → 채널별 분기

```kotlin
@Service
class NotificationProcessor(
    private val settingRepository: NotificationSettingRepository,
    private val subscriptionRepository: NotificationSubscriptionRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val notificationRepository: NotificationRepository,
    private val channelSenderFactory: NotificationChannelSenderFactory,
    private val resolveSettingUseCase: ResolveNotificationSettingUseCase
) {
    fun processEvaluationCompleted(event: EvaluationCompletedEvent) {
        val type = NotificationType.EVALUATION_COMPLETED

        // 1. 워크스페이스 설정 확인
        val setting = settingRepository.findByWorkspaceAndType(event.workspaceId, type, NotificationChannel.IN_APP)
        if (setting?.enabled != true) return

        // 2. 수신자 목록 조회 (구독 + 설정 resolve)
        val recipients = resolveRecipients(event.workspaceId, type)

        // 3. 채널별 알림 생성 및 발송
        for (recipient in recipients) {
            for (channel in NotificationChannel.values()) {
                if (!resolveSettingUseCase.resolve(event.workspaceId, recipient.userId, type, channel)) continue

                val template = templateRepository.findByWorkspaceAndType(event.workspaceId, type, channel)
                    ?: templateRepository.findDefault(type, channel)
                    ?: continue

                val rendered = template.render(mapOf(
                    "applicantName" to event.applicantName,
                    "stageName" to event.stageName
                ))

                val notification = notificationRepository.save(Notification(
                    workspaceId = event.workspaceId,
                    recipientUserId = recipient.userId,
                    type = type,
                    category = NotificationCategory.EVALUATION,
                    channel = channel,
                    title = rendered.subject ?: rendered.body.take(100),
                    content = rendered.body,
                    metadata = mapOf("applicantId" to event.applicantId, "stageName" to event.stageName),
                    sourceType = SourceType.APPLICANT,
                    sourceId = event.applicantId
                ))

                channelSenderFactory.getSender(channel).send(notification)
            }
        }
    }
}
```

### 3. Kafka Producer: queue.notification.send.v1 발행

```kotlin
@Component
class NotificationKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    fun publishMailSend(event: MailSendEvent) {
        kafkaTemplate.send("queue.mail.send", event.recipientEmail, objectMapper.writeValueAsString(event))
    }

    fun publishSlackSend(event: SlackSendEvent) {
        kafkaTemplate.send("queue.slack.send", event.channelId, objectMapper.writeValueAsString(event))
    }

    fun publishInAppNotification(event: InAppNotificationEvent) {
        kafkaTemplate.send("queue.notification.inapp", event.userId.toString(), objectMapper.writeValueAsString(event))
    }
}
```

### 4. 멱등성 보장 (consumed_events 테이블)

- 모든 이벤트 메시지에 UUID 기반 `eventId` 포함
- Consumer 처리 전 `consumed_events` 테이블에서 `eventId` 존재 여부 확인
- 존재하면 skip, 없으면 처리 후 INSERT
- `enable.auto.commit=false`, 수동 `ack.acknowledge()` 호출

### 5. Kafka 설정

```kotlin
@Configuration
class KafkaConfig {
    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setConcurrency(3)
        factory.setCommonErrorHandler(DefaultErrorHandler(
            DeadLetterPublishingRecoverer(kafkaTemplate()),
            FixedBackOff(1000L, 3)
        ))
        return factory
    }
}
```

### 6. DLQ (Dead Letter Queue) 처리

- 3회 재시도 후 실패 시 `event.notification.*.v1.DLT` 토픽으로 전송
- DLQ 모니터링 알림 설정 (Datadog)

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-notification-service | infrastructure | src/.../infrastructure/kafka/consumer/NotificationEventConsumer.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/kafka/producer/NotificationKafkaProducer.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/kafka/config/KafkaConfig.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/kafka/event/*.kt | 신규 (이벤트 DTO 5~6개) |
| greeting-notification-service | application | src/.../application/service/NotificationProcessor.kt | 신규 |
| greeting-notification-service | application | src/.../application/service/NotificationChannelSenderFactory.kt | 신규 |
| greeting-topic | - | event.notification.evaluation-completed.v1 | 신규 토픽 |
| greeting-topic | - | event.notification.evaluation-submitted.v1 | 신규 토픽 |
| greeting-topic | - | event.notification.stage-entry.v1 | 신규 토픽 |
| greeting-topic | - | event.notification.interview-remind.v1 | 신규 토픽 |
| greeting-topic | - | event.notification.evaluation-remind.v1 | 신규 토픽 |

## 영향 범위

- greeting-topic: 5개 신규 Kafka 토픽 등록 필요
- greeting-new-back: Phase 2에서 이벤트 발행 추가 시 연동 (이 티켓에서는 Consumer만)
- doodlin-communication: queue.mail.send, queue.slack.send 토픽 소비 (기존 토픽 재사용)

## 테스트 케이스

| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-05-01 | 평가 완료 이벤트 소비 | evaluation-completed 메시지 발행 | Consumer 소비 | Notification 생성, 채널별 발송 |
| TC-05-02 | 멱등성 - 중복 이벤트 | 동일 eventId 메시지 2회 발행 | Consumer 2회 소비 | Notification 1건만 생성 |
| TC-05-03 | 설정 비활성화 시 skip | setting.enabled=false | 이벤트 소비 | Notification 생성 안 됨 |
| TC-05-04 | DLQ 전송 | 처리 중 예외 3회 | 3회 재시도 실패 | DLT 토픽으로 메시지 전송 |
| TC-05-05 | 수동 오프셋 커밋 | 정상 처리 | ack.acknowledge() 호출 | 오프셋 커밋 완료 |
| TC-05-06 | 채널별 분기 | IN_APP + EMAIL 활성 | 이벤트 소비 | 2개 채널에 각각 알림 생성 |
| TC-05-07 | Mail Producer 발행 | 이메일 채널 알림 | publishMailSend() | queue.mail.send 토픽에 메시지 |

## 기대 결과 (AC)

- [ ] 5개 토픽 Consumer 정상 소비
- [ ] 멱등성 보장 (동일 eventId 중복 처리 방지)
- [ ] 알림 생성 → 설정 체크 → 채널별 분기 로직 동작
- [ ] DLQ 설정 완료 (3회 재시도 후 DLT 전송)
- [ ] Kafka 연동 테스트 통과 (EmbeddedKafka 또는 Testcontainers)

## 체크리스트

- [ ] Consumer group-id: `notification-consumer` (Phase 2 병렬 배포 시 `notification-consumer-new` 사용)
- [ ] enable.auto.commit=false 확인
- [ ] DLQ 모니터링 Datadog 알림 설정
- [ ] 메시지 직렬화: JSON (Jackson)
- [ ] 대량 메시지 처리 시 concurrency 튜닝 고려
