# [GRT-4007] REST API 전체 구현

## 개요
- PRD: https://doodlin.atlassian.net/wiki/x/SICjdg
- Phase: 1 (서비스 구축)
- 예상 공수: 4d
- 의존성: GRT-4004

**범위:** presentation 모듈 — 알림 조회/설정/구독/리마인드/템플릿 관리 REST API 구현

## 작업 내용

### 1. 알림 조회 API

#### GET /api/v1/notifications
알림 목록 조회 (페이징)

```
Request:
  Headers: X-Workspace-Id, X-User-Id
  Params: page, size, category(optional), isRead(optional)

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "type": "EVALUATION_COMPLETED",
      "category": "EVALUATION",
      "channel": "IN_APP",
      "title": "홍길동님의 평가가 완료되었습니다",
      "content": "...",
      "metadata": { "applicantId": 123 },
      "isRead": false,
      "createdAt": "2026-03-17T10:00:00"
    }
  ],
  "page": 0, "size": 20, "totalElements": 50
}
```

#### GET /api/v1/notifications/unread-count
```
Response: 200 OK
{ "count": 5 }
```

#### PATCH /api/v1/notifications/{id}/read
```
Response: 204 No Content
```

#### PATCH /api/v1/notifications/read-all
```
Response: 204 No Content
```

### 2. 알림 설정 API

#### GET /api/v1/notification-settings
```
Response: 200 OK
{
  "settings": [
    {
      "id": 1,
      "notificationType": "EVALUATION_COMPLETED",
      "channel": "IN_APP",
      "enabled": true,
      "config": { "targetRoleIds": [1, 2] }
    }
  ]
}
```

#### PUT /api/v1/notification-settings
```
Request:
{
  "notificationType": "EVALUATION_COMPLETED",
  "channel": "IN_APP",
  "enabled": true,
  "config": { "targetRoleIds": [1, 2] }
}
Response: 200 OK
```

### 3. 개인 구독 설정 API

#### GET /api/v1/notification-subscriptions
```
Response: 200 OK
{
  "subscriptions": [
    {
      "notificationType": "EVALUATION_COMPLETED",
      "channel": "IN_APP",
      "enabled": true,
      "overrideByAdmin": false
    }
  ]
}
```

#### PUT /api/v1/notification-subscriptions
```
Request:
{
  "notificationType": "EVALUATION_COMPLETED",
  "channel": "IN_APP",
  "enabled": false
}
Response: 200 OK
```

### 4. 리마인드 설정 API

#### GET /api/v1/remind-settings
```
Response: 200 OK
{
  "interviewRemind": {
    "enabled": true,
    "remindBeforeMinutes": 60
  },
  "evaluationRemind": {
    "enabled": true,
    "remindBeforeHours": 24,
    "repeatIntervalDays": 3,
    "maxCount": 3
  }
}
```

- `PUT /api/v1/remind-settings/interview` — 면접 리마인드 설정 변경
- `PUT /api/v1/remind-settings/evaluation` — 평가 리마인드 설정 변경

### 5. 템플릿 관리 API

#### GET /api/v1/notification-templates/{type}
```
Response: 200 OK
{
  "notificationType": "INTERVIEW_REMIND",
  "channel": "EMAIL",
  "subjectTemplate": "면접 리마인드: ${applicantName}님 면접이 ${remainTime} 남았습니다",
  "bodyTemplate": "...",
  "variables": [
    { "name": "applicantName", "description": "지원자명", "required": true }
  ],
  "isDefault": false
}
```

- `PUT /api/v1/notification-templates/{type}` — 템플릿 저장/수정
- `DELETE /api/v1/notification-templates/{type}` — 템플릿 초기화 (기본 템플릿으로 복원)

### 6. Controller 구현

```kotlin
@RestController
@RequestMapping("/api/v1")
class NotificationController(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val markReadUseCase: MarkNotificationReadUseCase
) {
    @GetMapping("/notifications")
    fun getNotifications(
        @RequestHeader("X-Workspace-Id") workspaceId: Long,
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) category: NotificationCategory?
    ): ResponseEntity<Page<NotificationResponse>> {
        val result = getNotificationsUseCase.getByRecipient(workspaceId, userId, page, size)
        return ResponseEntity.ok(result.map { NotificationResponse.from(it) })
    }

    @GetMapping("/notifications/unread-count")
    fun getUnreadCount(
        @RequestHeader("X-Workspace-Id") workspaceId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<UnreadCountResponse> {
        val count = getNotificationsUseCase.getUnreadCount(workspaceId, userId)
        return ResponseEntity.ok(UnreadCountResponse(count))
    }

    @PatchMapping("/notifications/{id}/read")
    fun markAsRead(
        @PathVariable id: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<Void> {
        markReadUseCase.markAsRead(id, userId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/notifications/read-all")
    fun markAllAsRead(
        @RequestHeader("X-Workspace-Id") workspaceId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<Void> {
        markReadUseCase.markAllAsRead(workspaceId, userId)
        return ResponseEntity.noContent().build()
    }
}

@RestController
@RequestMapping("/api/v1")
class NotificationSettingController(
    private val settingUseCase: ManageNotificationSettingUseCase
) { /* ... */ }

@RestController
@RequestMapping("/api/v1")
class NotificationSubscriptionController(
    private val subscriptionUseCase: ManageSubscriptionUseCase
) { /* ... */ }

@RestController
@RequestMapping("/api/v1")
class RemindSettingController(
    private val scheduleUseCase: ManageScheduleUseCase
) { /* ... */ }

@RestController
@RequestMapping("/api/v1")
class NotificationTemplateController(
    private val templateUseCase: ManageTemplateUseCase
) { /* ... */ }
```

### 7. UseCase Service 구현

```kotlin
@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository
) : GetNotificationsUseCase, MarkNotificationReadUseCase {
    // 구현
}

@Service
@Transactional
class NotificationSettingService(
    private val settingRepository: NotificationSettingRepository
) : ManageNotificationSettingUseCase {
    // 구현
}
// ... 나머지 UseCase 서비스
```

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-notification-service | presentation | src/.../presentation/controller/NotificationController.kt | 신규 |
| greeting-notification-service | presentation | src/.../presentation/controller/NotificationSettingController.kt | 신규 |
| greeting-notification-service | presentation | src/.../presentation/controller/NotificationSubscriptionController.kt | 신규 |
| greeting-notification-service | presentation | src/.../presentation/controller/RemindSettingController.kt | 신규 |
| greeting-notification-service | presentation | src/.../presentation/controller/NotificationTemplateController.kt | 신규 |
| greeting-notification-service | presentation | src/.../presentation/dto/request/*.kt | 신규 (10+개 Request DTO) |
| greeting-notification-service | presentation | src/.../presentation/dto/response/*.kt | 신규 (10+개 Response DTO) |
| greeting-notification-service | application | src/.../application/service/NotificationService.kt | 신규 |
| greeting-notification-service | application | src/.../application/service/NotificationSettingService.kt | 신규 |
| greeting-notification-service | application | src/.../application/service/NotificationSubscriptionService.kt | 신규 |
| greeting-notification-service | application | src/.../application/service/RemindSettingService.kt | 신규 |
| greeting-notification-service | application | src/.../application/service/NotificationTemplateService.kt | 신규 |

## 영향 범위

- greeting-api-gateway: 신규 서비스로 라우팅 규칙 추가 필요 (Phase 3)
- FE: 기존 API 경로와 다름. Phase 3에서 Ingress로 v2 경로 라우팅.

## 테스트 케이스

| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-07-01 | 알림 목록 조회 | 10건 알림 존재 | GET /notifications?page=0&size=5 | 5건 반환, 페이징 정보 포함 |
| TC-07-02 | 안읽은 카운트 | 3건 unread | GET /notifications/unread-count | { count: 3 } |
| TC-07-03 | 단건 읽음 처리 | unread 알림 1건 | PATCH /notifications/1/read | 204, isRead=true |
| TC-07-04 | 전체 읽음 처리 | 5건 unread | PATCH /notifications/read-all | 204, 전체 isRead=true |
| TC-07-05 | 설정 조회 | 설정 3건 | GET /notification-settings | 3건 반환 |
| TC-07-06 | 설정 변경 | - | PUT /notification-settings | 200, 설정 저장됨 |
| TC-07-07 | 개인 구독 조회 | 구독 2건 | GET /notification-subscriptions | 2건 반환 |
| TC-07-08 | 개인 구독 변경 | - | PUT /notification-subscriptions | 200, 변경됨 |
| TC-07-09 | 관리자 override된 구독 변경 시도 | overrideByAdmin=true | PUT /notification-subscriptions | 403 Forbidden |
| TC-07-10 | 템플릿 조회 | 커스텀 템플릿 존재 | GET /notification-templates/INTERVIEW_REMIND | 커스텀 템플릿 반환 |
| TC-07-11 | 템플릿 없을 때 기본값 | 커스텀 없음 | GET /notification-templates/INTERVIEW_REMIND | 기본 템플릿 반환, isDefault=true |
| TC-07-12 | 템플릿 초기화 | 커스텀 존재 | DELETE /notification-templates/INTERVIEW_REMIND | 커스텀 삭제, 기본으로 복원 |
| TC-07-13 | X-Workspace-Id 누락 | 헤더 없음 | 모든 API 호출 | 400 Bad Request |

## 기대 결과 (AC)

- [ ] 알림 조회 API 4개 정상 동작 (목록, 카운트, 단건 읽음, 전체 읽음)
- [ ] 알림 설정 API 2개 정상 동작 (조회, 변경)
- [ ] 개인 구독 API 2개 정상 동작 (조회, 변경)
- [ ] 리마인드 설정 API 3개 정상 동작 (조회, 면접 변경, 평가 변경)
- [ ] 템플릿 API 3개 정상 동작 (조회, 저장, 초기화)
- [ ] 관리자 override된 구독은 개인이 변경 불가
- [ ] 모든 API에 X-Workspace-Id, X-User-Id 헤더 검증

## 체크리스트

- [ ] DTO validation (@Valid, @NotNull, @NotBlank)
- [ ] GlobalExceptionHandler 구현 (표준 에러 응답 포맷)
- [ ] Swagger/OpenAPI 문서 자동 생성 (springdoc-openapi)
- [ ] API 버전: /api/v1
- [ ] 페이징 기본값: page=0, size=20, maxSize=100
