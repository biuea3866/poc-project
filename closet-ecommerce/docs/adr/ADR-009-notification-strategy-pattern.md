# ADR-009: 알림 시스템 Strategy 패턴 + 알림 설정

**상태**: 확정
**날짜**: 2026-04-09
**결정자**: BE Senior

---

## 컨텍스트

closet-notification 모듈에 채널별 발송 로직과 회원별 알림 설정이 필요. 현재 3개 채널(EMAIL, SMS, PUSH)을 지원하며, 향후 카카오알림톡/앱내알림 등 추가 가능.

## 결정

### 1. 채널 디스패치: Strategy + Factory 패턴

```
NotificationSender (interface)           ← Strategy
├── EmailNotificationSender (@Component)
├── SmsNotificationSender (@Component)
├── PushNotificationSender (@Component)
└── (향후) KakaoAlimtalkSender, InAppSender

NotificationDispatcher (@Component)       ← Factory
├── senders: List<NotificationSender>
├── dispatch(notification) → sender.supports(channel) → sender.send()
└── 발송 전 NotificationPreference 확인
```

CarrierAdapterFactory, PaymentGatewayFactory와 동일한 패턴.

### 2. 알림 설정: 2-레이어 구조

**회원 레벨 (NotificationPreference)**
| 필드 | 설명 |
|------|------|
| emailEnabled | 이메일 수신 동의 |
| smsEnabled | SMS 수신 동의 |
| pushEnabled | 푸시 수신 동의 |
| marketingEnabled | 마케팅 알림 동의 |
| nightEnabled | 야간 알림 동의 (21:00~08:00) |

**토픽 레벨 (NotificationTopicSubscription)**
| 필드 | 설명 |
|------|------|
| topicType | PRODUCT / CATEGORY / BRAND / EVENT |
| topicId | 대상 ID (productId, categoryId 등) |
| isSubscribed | 구독 여부 |

### 3. 발송 흐름

```
1. NotificationService.send(memberId, channel, title, content)
2. → DB 저장 (Notification 엔티티)
3. → NotificationDispatcher.dispatch(notification)
   3-1. PreferenceService.isChannelEnabled(memberId, channel) 확인
   3-2. DND 시간대 확인 (nightEnabled false + 21:00~08:00 → 차단)
   3-3. 차단 시 → notification.status = BLOCKED
   3-4. 통과 시 → sender.send(notification)
```

### 4. Kafka Consumer

| 토픽 | 이벤트 | 처리 |
|------|--------|------|
| event.closet.inventory | RESTOCK_NOTIFICATION | 재입고 구독 회원에게 알림 발송 |

Consumer → Facade → Service 아키텍처 준수.

## 대안

1. **단일 NotificationService에 if-else 분기** — 채널 추가 시 Service 수정 필요, OCP 위반
2. **별도 microservice per channel** — 오버엔지니어링, 현재 규모에 불필요
3. **외부 서비스 (Firebase, AWS SNS)** — 향후 Sender 구현체 교체로 대응 가능

## 결과

- 새 채널 추가 시 Sender 구현체만 추가 (Service/Dispatcher 변경 없음)
- 회원별 채널 opt-out + DND + 토픽 구독으로 세밀한 알림 제어
- 마케팅 동의/야간 동의 분리로 법적 요건(정보통신망법) 준수 가능
