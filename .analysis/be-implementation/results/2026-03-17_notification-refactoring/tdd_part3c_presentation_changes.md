# [알림 시스템 리팩토링] Part 3c: Presentation 모듈 + greeting-new-back 변경 설계

> 상위 문서: [Part 1 - ERD & 도메인](tdd_part1_erd_domain.md), [Part 2 - 아키텍처 & API](tdd_part2_architecture_api.md)
> 작성일: 2026-03-17
> 목적: greeting-notification-service의 REST Controller 전체 설계 + greeting-new-back 이관/변경 상세

---

## 1. Presentation 모듈 (greeting-notification-service)

### 1.1 모듈 구조

```
presentation/
├── controller/
│   ├── NotificationController.kt          # 알림 조회/관리 (기존 이관)
│   ├── NotificationSettingsController.kt   # 개인 알림 설정
│   ├── AdminSettingsController.kt          # 관리자 알림 설정
│   ├── RemindController.kt                # 리마인드 설정
│   └── TemplateController.kt              # 템플릿 관리
├── dto/
│   ├── request/
│   │   ├── ReadAllNotificationsRequest.kt
│   │   ├── UpdateMySettingsRequest.kt
│   │   ├── UpdateSubscriptionsRequest.kt
│   │   ├── UpdateAdminMemberSettingsRequest.kt
│   │   ├── UpdateEvaluationRemindRequest.kt
│   │   ├── UpdateMeetingRemindRequest.kt
│   │   └── UpdateTemplateRequest.kt
│   └── response/
│       ├── NotificationListResponse.kt
│       ├── UnreadCountResponse.kt
│       ├── ReadNotificationResponse.kt
│       ├── ReadAllNotificationsResponse.kt
│       ├── MySettingsResponse.kt
│       ├── SubscriptionListResponse.kt
│       ├── AdminMemberSettingsResponse.kt
│       ├── RemindSettingsResponse.kt
│       └── TemplateListResponse.kt
├── resolver/
│   └── AuthenticatedUserArgumentResolver.kt
├── interceptor/
│   └── WorkspaceContextInterceptor.kt
└── config/
    └── WebMvcConfig.kt
```

### 1.2 공통: 인증 컨텍스트

API Gateway가 JWT 검증 후 주입하는 헤더를 ArgumentResolver로 바인딩한다.

```kotlin
/**
 * Gateway가 JWT 검증 후 주입하는 헤더에서 인증 정보를 추출하는 ArgumentResolver.
 * 모든 Controller 메서드에서 @AuthenticatedUser 어노테이션으로 주입받는다.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthenticatedUser

data class UserContext(
    val userId: Long,
    val workspaceId: Long,
    val roles: List<String>,  // e.g., ["ADMIN", "RECRUITER"]
    val email: String?,
) {
    fun isAdmin(): Boolean = roles.contains("ADMIN")
}

@Component
class AuthenticatedUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(AuthenticatedUser::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): UserContext {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw UnauthorizedException("요청 컨텍스트를 확인할 수 없습니다")

        val userId = request.getHeader("X-User-Id")?.toLongOrNull()
            ?: throw UnauthorizedException("인증 정보가 없습니다 (X-User-Id)")
        val workspaceId = request.getHeader("X-Workspace-Id")?.toLongOrNull()
            ?: throw UnauthorizedException("워크스페이스 정보가 없습니다 (X-Workspace-Id)")
        val roles = request.getHeader("X-User-Roles")
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()
        val email = request.getHeader("X-User-Email")

        return UserContext(
            userId = userId,
            workspaceId = workspaceId,
            roles = roles,
            email = email,
        )
    }
}
```

### 1.3 공통: API 응답 래퍼

```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
        fun <T> error(code: String, message: String): ApiResponse<T> =
            ApiResponse(success = false, error = ErrorDetail(code, message))
    }
}

data class ErrorDetail(
    val code: String,
    val message: String,
)
```

---

### 1.4 NotificationController (알림 조회/관리 - 기존 이관)

> 기존 Node.js `greeting-notification-server`의 REST API를 Spring으로 이관

```kotlin
@RestController
@RequestMapping("/service/notification/api/v1.0/notifications")
class NotificationController(
    private val notificationQueryUseCase: NotificationQueryUseCase,
    private val notificationCommandUseCase: NotificationCommandUseCase,
) {

    /**
     * GET /notifications
     * 알림 목록 조회 (페이지네이션, 필터)
     *
     * 기존 Node.js: GET /api/v1.0/alerts?groupId={}&type={}&limit={}&offset={}
     * → 신규: 표준 Page/Size 페이지네이션 + category/isRead 필터 추가
     */
    @GetMapping
    fun getNotifications(
        @AuthenticatedUser user: UserContext,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) category: NotificationCategory?,
        @RequestParam(required = false) isRead: Boolean?,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
    ): ApiResponse<PagedResponse<NotificationItemResponse>> {
        val result = notificationQueryUseCase.getNotifications(
            GetNotificationsQuery(
                workspaceId = user.workspaceId,
                userId = user.userId,
                page = page,
                size = size,
                category = category,
                isRead = isRead,
                fromDate = fromDate,
                toDate = toDate,
            )
        )
        return ApiResponse.ok(result)
    }

    /**
     * GET /notifications/count
     * 미읽음 알림 카운트 (Redis 캐시 활용, TTL 1min)
     *
     * 기존 Node.js: 별도 count API 없었음 (프론트에서 목록 조회 후 클라이언트 카운트)
     * → 신규: 서버 사이드 카운트 + 카테고리별 분리
     */
    @GetMapping("/count")
    fun getUnreadCount(
        @AuthenticatedUser user: UserContext,
    ): ApiResponse<UnreadCountResponse> {
        val result = notificationQueryUseCase.getUnreadCount(
            workspaceId = user.workspaceId,
            userId = user.userId,
        )
        return ApiResponse.ok(result)
    }

    /**
     * POST /notifications/{id}/read
     * 단건 알림 읽음 처리
     *
     * 기존 Node.js: PATCH /api/v1.0/alerts/{id}/read
     * → 신규: POST (멱등 보장, Redis 카운트 DECR 연동)
     */
    @PostMapping("/{id}/read")
    fun markAsRead(
        @AuthenticatedUser user: UserContext,
        @PathVariable id: Long,
    ): ApiResponse<ReadNotificationResponse> {
        val result = notificationCommandUseCase.markAsRead(
            MarkAsReadCommand(
                notificationId = id,
                workspaceId = user.workspaceId,
                userId = user.userId,
            )
        )
        return ApiResponse.ok(result)
    }

    /**
     * POST /notifications/read-all
     * 전체 알림 읽음 처리 (카테고리별 필터 가능)
     *
     * 기존 Node.js: PATCH /api/v1.0/alerts/read-all?type=Recruiting|System
     * → 신규: category 기반 + updatedCount 응답
     */
    @PostMapping("/read-all")
    fun markAllAsRead(
        @AuthenticatedUser user: UserContext,
        @RequestBody(required = false) request: ReadAllNotificationsRequest?,
    ): ApiResponse<ReadAllNotificationsResponse> {
        val result = notificationCommandUseCase.markAllAsRead(
            MarkAllAsReadCommand(
                workspaceId = user.workspaceId,
                userId = user.userId,
                category = request?.category,
            )
        )
        return ApiResponse.ok(result)
    }
}
```

#### Request/Response DTO

```kotlin
// ── Request ──

data class ReadAllNotificationsRequest(
    val category: NotificationCategory?,
)

// ── Response ──

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)

data class NotificationItemResponse(
    val id: Long,
    val type: String,              // e.g., "EVALUATION_COMPLETED"
    val category: String,          // e.g., "EVALUATION"
    val title: String,
    val content: String?,
    val isRead: Boolean,
    val metadata: Map<String, Any>,  // deepLink, applicantId 등
    val createdAt: OffsetDateTime,
)

data class UnreadCountResponse(
    val unreadCount: Int,
    val countByCategory: Map<String, Int>,  // {"EVALUATION": 3, "MEETING": 1, ...}
)

data class ReadNotificationResponse(
    val id: Long,
    val isRead: Boolean,
    val readAt: OffsetDateTime,
)

data class ReadAllNotificationsResponse(
    val updatedCount: Int,
    val readAt: OffsetDateTime,
)
```

---

### 1.5 NotificationSettingsController (개인 알림 설정 - 신규)

```kotlin
@RestController
@RequestMapping("/service/notification/api/v1.0/settings")
class NotificationSettingsController(
    private val settingsQueryUseCase: NotificationSettingsQueryUseCase,
    private val settingsCommandUseCase: NotificationSettingsCommandUseCase,
) {

    /**
     * GET /settings
     * 내 알림 설정 전체 조회
     *
     * 워크스페이스 기본값, 개인 오버라이드, 관리자 오버라이드, 최종 resolve 결과를 모두 반환.
     * 프론트엔드 설정 UI에서 각 항목의 출처(source)를 표시하기 위함.
     */
    @GetMapping
    fun getMySettings(
        @AuthenticatedUser user: UserContext,
    ): ApiResponse<MySettingsResponse> {
        val result = settingsQueryUseCase.getMySettings(
            workspaceId = user.workspaceId,
            userId = user.userId,
        )
        return ApiResponse.ok(result)
    }

    /**
     * PUT /settings
     * 내 알림 설정 변경
     *
     * 개인 설정에서는 채널만 변경 가능 (enabled는 관리자 전용).
     * 명시하지 않은 알림 유형은 변경하지 않음 (partial update).
     * 관리자 오버라이드로 비활성화된 항목은 개인 설정 변경 불가 → 409 Conflict.
     */
    @PutMapping
    fun updateMySettings(
        @AuthenticatedUser user: UserContext,
        @Valid @RequestBody request: UpdateMySettingsRequest,
    ): ApiResponse<UpdatedSettingsResponse> {
        val result = settingsCommandUseCase.updateMySettings(
            UpdateMySettingsCommand(
                workspaceId = user.workspaceId,
                userId = user.userId,
                settings = request.settings,
            )
        )
        return ApiResponse.ok(result)
    }

    /**
     * GET /settings/subscriptions
     * 내 구독 목록 조회
     *
     * 전형 진입 알림의 경우, 사용자가 특정 공고/전형에 대해 구독 여부를 선택한다.
     * 이 API는 현재 구독 중인 공고+전형 목록을 반환한다.
     */
    @GetMapping("/subscriptions")
    fun getMySubscriptions(
        @AuthenticatedUser user: UserContext,
    ): ApiResponse<SubscriptionListResponse> {
        val result = settingsQueryUseCase.getMySubscriptions(
            workspaceId = user.workspaceId,
            userId = user.userId,
        )
        return ApiResponse.ok(result)
    }

    /**
     * PUT /settings/subscriptions
     * 내 구독 변경
     *
     * 전형 진입 알림 구독 대상 공고/전형을 변경한다.
     * subscriptions 배열을 전체 교체 (replace all) 방식.
     */
    @PutMapping("/subscriptions")
    fun updateMySubscriptions(
        @AuthenticatedUser user: UserContext,
        @Valid @RequestBody request: UpdateSubscriptionsRequest,
    ): ApiResponse<SubscriptionListResponse> {
        val result = settingsCommandUseCase.updateMySubscriptions(
            UpdateSubscriptionsCommand(
                workspaceId = user.workspaceId,
                userId = user.userId,
                subscriptions = request.subscriptions,
            )
        )
        return ApiResponse.ok(result)
    }
}
```

#### Request/Response DTO

```kotlin
// ── Request ──

data class UpdateMySettingsRequest(
    @field:NotEmpty(message = "변경할 설정이 하나 이상 필요합니다")
    val settings: Map<
        @field:NotBlank String,  // alertType key (e.g., "evaluationCompleted")
        ChannelSettingRequest
    >,
)

data class ChannelSettingRequest(
    @field:NotEmpty(message = "채널을 하나 이상 선택해야 합니다")
    val channels: List<@field:Pattern(regexp = "IN_APP|EMAIL|SLACK") String>,
)

data class UpdateSubscriptionsRequest(
    @field:Valid
    val subscriptions: List<SubscriptionItemRequest>,
)

data class SubscriptionItemRequest(
    @field:NotNull
    val jobPostingId: Long,
    val stageIds: List<Long>?,    // null이면 해당 공고의 모든 전형 구독
    val enabled: Boolean = true,
)

// ── Response ──

data class MySettingsResponse(
    val workspaceDefaults: Map<String, AlertSettingDetail>,
    val myOverrides: Map<String, ChannelOverrideDetail>,
    val adminOverrides: Map<String, AdminOverrideDetail>,
    val resolvedSettings: Map<String, ResolvedSettingDetail>,
)

data class AlertSettingDetail(
    val enabled: Boolean,
    val channels: List<String>,
    val realtime: Boolean,
)

data class ChannelOverrideDetail(
    val channels: List<String>,
)

data class AdminOverrideDetail(
    val enabled: Boolean,
    val overriddenBy: String,
    val overriddenAt: OffsetDateTime,
)

data class ResolvedSettingDetail(
    val enabled: Boolean,
    val channels: List<String>,
    val source: String,  // "SYSTEM_DEFAULT" | "WORKSPACE_DEFAULT" | "USER_OVERRIDE" | "ADMIN_OVERRIDE"
)

data class UpdatedSettingsResponse(
    val updatedSettings: Map<String, ChannelOverrideDetail>,
    val updatedAt: OffsetDateTime,
)

data class SubscriptionListResponse(
    val subscriptions: List<SubscriptionItemResponse>,
)

data class SubscriptionItemResponse(
    val jobPostingId: Long,
    val jobPostingTitle: String,
    val stageId: Long?,
    val stageName: String?,
    val enabled: Boolean,
    val subscribedAt: OffsetDateTime?,
)
```

---

### 1.6 AdminSettingsController (관리자 알림 설정 - 신규)

```kotlin
@RestController
@RequestMapping("/service/notification/api/v1.0/admin/settings")
class AdminSettingsController(
    private val adminSettingsQueryUseCase: AdminSettingsQueryUseCase,
    private val adminSettingsCommandUseCase: AdminSettingsCommandUseCase,
) {

    /**
     * GET /admin/settings/members
     * 멤버별 알림 설정 목록 (관리자 전용)
     *
     * 워크스페이스 전체 멤버의 알림 설정 현황을 조회한다.
     * 관리자가 특정 멤버의 알림을 강제 비활성화/활성화할 수 있는 UI를 위해 사용.
     */
    @GetMapping("/members")
    fun getMemberSettings(
        @AuthenticatedUser user: UserContext,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) searchName: String?,
        @RequestParam(required = false) roleId: Long?,
    ): ApiResponse<PagedResponse<AdminMemberSettingResponse>> {
        requireAdmin(user)
        val result = adminSettingsQueryUseCase.getMemberSettings(
            GetMemberSettingsQuery(
                workspaceId = user.workspaceId,
                page = page,
                size = size,
                searchName = searchName,
                roleId = roleId,
            )
        )
        return ApiResponse.ok(result)
    }

    /**
     * PUT /admin/settings/members/{userId}
     * 멤버 알림 설정 강제 변경 (관리자 전용)
     *
     * 특정 멤버의 알림을 관리자 오버라이드로 제어한다.
     * admin_alert_config_overrides 테이블에 UPSERT.
     * 이 오버라이드는 개인 설정보다 높은 우선순위를 가진다.
     */
    @PutMapping("/members/{userId}")
    fun updateMemberSettings(
        @AuthenticatedUser user: UserContext,
        @PathVariable userId: Long,
        @Valid @RequestBody request: UpdateAdminMemberSettingsRequest,
    ): ApiResponse<AdminMemberSettingResponse> {
        requireAdmin(user)
        val result = adminSettingsCommandUseCase.updateMemberSettings(
            UpdateMemberSettingsCommand(
                workspaceId = user.workspaceId,
                targetUserId = userId,
                adminUserId = user.userId,
                overrides = request.overrides,
            )
        )
        return ApiResponse.ok(result)
    }

    private fun requireAdmin(user: UserContext) {
        if (!user.isAdmin()) {
            throw ForbiddenException("관리자 권한이 필요합니다")
        }
    }
}
```

#### Request/Response DTO

```kotlin
// ── Request ──

data class UpdateAdminMemberSettingsRequest(
    @field:NotEmpty(message = "변경할 오버라이드 설정이 하나 이상 필요합니다")
    val overrides: Map<
        String,  // alertType key (e.g., "evaluationCompleted")
        AdminOverrideRequest
    >,
)

data class AdminOverrideRequest(
    @field:NotNull
    val enabled: Boolean,
    val channels: List<String>?,  // null이면 채널은 변경하지 않음 (enabled만 제어)
)

// ── Response ──

data class AdminMemberSettingResponse(
    val userId: Long,
    val userName: String,
    val email: String?,
    val roleName: String?,
    val settings: Map<String, MemberAlertSettingDetail>,
)

data class MemberAlertSettingDetail(
    val workspaceDefault: AlertSettingDetail,     // WS 기본값
    val userOverride: ChannelOverrideDetail?,     // 개인 오버라이드 (있으면)
    val adminOverride: AdminOverrideDetail?,      // 관리자 오버라이드 (있으면)
    val resolved: ResolvedSettingDetail,           // 최종 적용값
)
```

---

### 1.7 RemindController (리마인드 설정 - 신규)

```kotlin
@RestController
@RequestMapping("/service/notification/api/v1.0/remind")
class RemindController(
    private val remindQueryUseCase: RemindSettingsQueryUseCase,
    private val remindCommandUseCase: RemindSettingsCommandUseCase,
) {

    // ── 평가 리마인드 ──

    /**
     * GET /remind/evaluation
     * 평가 리마인드 설정 조회
     *
     * 워크스페이스의 평가 리마인드 설정(미완료 평가 독촉 주기, 최대 횟수 등)을 조회한다.
     * 기존: greeting-new-back의 EvaluationRemindSettings 엔티티 직접 조회
     * → 신규: notification-service가 전담 관리
     */
    @GetMapping("/evaluation")
    fun getEvaluationRemindSettings(
        @AuthenticatedUser user: UserContext,
    ): ApiResponse<EvaluationRemindSettingsResponse> {
        requireAdmin(user)
        val result = remindQueryUseCase.getEvaluationRemindSettings(
            workspaceId = user.workspaceId,
        )
        return ApiResponse.ok(result)
    }

    /**
     * PUT /remind/evaluation
     * 평가 리마인드 설정 변경
     *
     * 기존: greeting-new-back에서 evaluation_remind_settings 테이블 직접 UPDATE
     * → 신규: notification-service가 evaluation_remind_settings 테이블을 관리하며
     *        Scheduler가 이 설정을 참조하여 리마인드 이벤트를 트리거한다.
     */
    @PutMapping("/evaluation")
    fun updateEvaluationRemindSettings(
        @AuthenticatedUser user: UserContext,
        @Valid @RequestBody request: UpdateEvaluationRemindRequest,
    ): ApiResponse<EvaluationRemindSettingsResponse> {
        requireAdmin(user)
        val result = remindCommandUseCase.updateEvaluationRemindSettings(
            UpdateEvaluationRemindCommand(
                workspaceId = user.workspaceId,
                updatedByUserId = user.userId,
                enabled = request.enabled,
                remindBeforeHours = request.remindBeforeHours,
                repeatIntervalDays = request.repeatIntervalDays,
                maxRemindCount = request.maxRemindCount,
                targetStatuses = request.targetStatuses,
                channels = request.channels,
            )
        )
        return ApiResponse.ok(result)
    }

    // ── 면접 리마인드 ──

    /**
     * GET /remind/meeting
     * 면접 리마인드 설정 조회
     *
     * 기존: greeting-new-back의 MeetingSystemAlertConfiguration 엔티티
     * → 신규: notification-service의 meeting_remind_settings 테이블로 이관
     */
    @GetMapping("/meeting")
    fun getMeetingRemindSettings(
        @AuthenticatedUser user: UserContext,
    ): ApiResponse<MeetingRemindSettingsResponse> {
        requireAdmin(user)
        val result = remindQueryUseCase.getMeetingRemindSettings(
            workspaceId = user.workspaceId,
        )
        return ApiResponse.ok(result)
    }

    /**
     * PUT /remind/meeting
     * 면접 리마인드 설정 변경
     *
     * 기존: MeetingSystemAlertConfiguration에서 면접관/지원자 각각의 알림 채널과 시점을 관리
     * → 신규: meeting_remind_settings 테이블 UPSERT + 채널별 세부 설정
     */
    @PutMapping("/meeting")
    fun updateMeetingRemindSettings(
        @AuthenticatedUser user: UserContext,
        @Valid @RequestBody request: UpdateMeetingRemindRequest,
    ): ApiResponse<MeetingRemindSettingsResponse> {
        requireAdmin(user)
        val result = remindCommandUseCase.updateMeetingRemindSettings(
            UpdateMeetingRemindCommand(
                workspaceId = user.workspaceId,
                updatedByUserId = user.userId,
                enabled = request.enabled,
                interviewerRemindMinutesBefore = request.interviewerRemindMinutesBefore,
                applicantRemindMinutesBefore = request.applicantRemindMinutesBefore,
                channels = request.channels,
            )
        )
        return ApiResponse.ok(result)
    }

    private fun requireAdmin(user: UserContext) {
        if (!user.isAdmin()) {
            throw ForbiddenException("관리자 권한이 필요합니다")
        }
    }
}
```

#### Request/Response DTO

```kotlin
// ── Request ──

data class UpdateEvaluationRemindRequest(
    @field:NotNull
    val enabled: Boolean,

    @field:Min(1) @field:Max(168)  // 1시간~7일
    val remindBeforeHours: Int,

    @field:Min(1) @field:Max(30)
    val repeatIntervalDays: Int,

    @field:Min(1) @field:Max(10)
    val maxRemindCount: Int,

    val targetStatuses: List<String>,  // e.g., ["NOT_STARTED", "IN_PROGRESS"]

    @field:NotEmpty
    val channels: List<@field:Pattern(regexp = "IN_APP|EMAIL|SLACK") String>,
)

data class UpdateMeetingRemindRequest(
    @field:NotNull
    val enabled: Boolean,

    @field:Valid
    val interviewerRemindMinutesBefore: List<@Min(5) @Max(1440) Int>,
    // e.g., [30, 60, 1440] → 30분 전, 1시간 전, 24시간 전

    @field:Valid
    val applicantRemindMinutesBefore: List<@Min(5) @Max(1440) Int>,

    @field:NotEmpty
    val channels: List<@field:Pattern(regexp = "IN_APP|EMAIL|SLACK|KAKAO") String>,
)

// ── Response ──

data class EvaluationRemindSettingsResponse(
    val enabled: Boolean,
    val remindBeforeHours: Int,
    val repeatIntervalDays: Int,
    val maxRemindCount: Int,
    val targetStatuses: List<String>,
    val channels: List<String>,
    val updatedAt: OffsetDateTime?,
    val updatedBy: String?,
)

data class MeetingRemindSettingsResponse(
    val enabled: Boolean,
    val interviewerRemindMinutesBefore: List<Int>,
    val applicantRemindMinutesBefore: List<Int>,
    val channels: List<String>,
    val updatedAt: OffsetDateTime?,
    val updatedBy: String?,
)
```

---

### 1.8 TemplateController (템플릿 관리 - 신규)

```kotlin
@RestController
@RequestMapping("/service/notification/api/v1.0/templates")
class TemplateController(
    private val templateQueryUseCase: NotificationTemplateQueryUseCase,
    private val templateCommandUseCase: NotificationTemplateCommandUseCase,
) {

    /**
     * GET /templates
     * 알림 템플릿 목록 조회
     *
     * 워크스페이스에서 커스터마이즈 가능한 알림 템플릿 목록을 반환한다.
     * 시스템 기본 템플릿 + 워크스페이스 커스텀 템플릿을 병합하여 반환.
     */
    @GetMapping
    fun getTemplates(
        @AuthenticatedUser user: UserContext,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) channel: String?,
    ): ApiResponse<TemplateListResponse> {
        requireAdmin(user)
        val result = templateQueryUseCase.getTemplates(
            GetTemplatesQuery(
                workspaceId = user.workspaceId,
                category = category,
                channel = channel,
            )
        )
        return ApiResponse.ok(result)
    }

    /**
     * PUT /templates/{id}
     * 템플릿 커스터마이즈
     *
     * 시스템 기본 템플릿을 워크스페이스별로 커스터마이즈한다.
     * notification_templates 테이블에서 해당 workspace_id + template_id 조합으로 UPSERT.
     * 변수 플레이스홀더({{applicantName}} 등)는 그대로 유지해야 한다.
     */
    @PutMapping("/{id}")
    fun updateTemplate(
        @AuthenticatedUser user: UserContext,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTemplateRequest,
    ): ApiResponse<TemplateDetailResponse> {
        requireAdmin(user)
        val result = templateCommandUseCase.updateTemplate(
            UpdateTemplateCommand(
                workspaceId = user.workspaceId,
                templateId = id,
                updatedByUserId = user.userId,
                titleTemplate = request.titleTemplate,
                bodyTemplate = request.bodyTemplate,
                emailSubjectTemplate = request.emailSubjectTemplate,
                emailBodyTemplate = request.emailBodyTemplate,
            )
        )
        return ApiResponse.ok(result)
    }

    /**
     * DELETE /templates/{id}
     * 템플릿 기본값 복원
     *
     * 워크스페이스 커스텀 템플릿을 삭제하여 시스템 기본값으로 되돌린다.
     * 시스템 기본 템플릿 자체는 삭제 불가.
     */
    @DeleteMapping("/{id}")
    fun resetTemplate(
        @AuthenticatedUser user: UserContext,
        @PathVariable id: Long,
    ): ApiResponse<TemplateDetailResponse> {
        requireAdmin(user)
        val result = templateCommandUseCase.resetToDefault(
            ResetTemplateCommand(
                workspaceId = user.workspaceId,
                templateId = id,
            )
        )
        return ApiResponse.ok(result)
    }

    private fun requireAdmin(user: UserContext) {
        if (!user.isAdmin()) {
            throw ForbiddenException("관리자 권한이 필요합니다")
        }
    }
}
```

#### Request/Response DTO

```kotlin
// ── Request ──

data class UpdateTemplateRequest(
    @field:NotBlank @field:Size(max = 255)
    val titleTemplate: String?,         // InApp 알림 제목 (e.g., "{{applicantName}} 평가 완료")

    @field:Size(max = 2000)
    val bodyTemplate: String?,          // InApp 알림 본문

    @field:Size(max = 255)
    val emailSubjectTemplate: String?,  // 이메일 제목

    @field:Size(max = 10000)
    val emailBodyTemplate: String?,     // 이메일 HTML 본문
)

// ── Response ──

data class TemplateListResponse(
    val templates: List<TemplateDetailResponse>,
)

data class TemplateDetailResponse(
    val id: Long,
    val templateKey: String,        // e.g., "evaluation-completed"
    val category: String,           // e.g., "EVALUATION"
    val channel: String,            // e.g., "IN_APP", "EMAIL"
    val titleTemplate: String,
    val bodyTemplate: String?,
    val emailSubjectTemplate: String?,
    val emailBodyTemplate: String?,
    val isCustomized: Boolean,      // 워크스페이스 커스텀 여부
    val availableVariables: List<TemplateVariableInfo>,  // 사용 가능한 변수 목록
    val updatedAt: OffsetDateTime?,
)

data class TemplateVariableInfo(
    val name: String,          // e.g., "applicantName"
    val description: String,   // e.g., "지원자 이름"
    val example: String,       // e.g., "홍길동"
)
```

---

### 1.9 전체 REST API 요약

| # | Method | Path | 권한 | 설명 | 기존/신규 |
|---|--------|------|------|------|----------|
| 1 | GET | /notifications | 인증 사용자 | 알림 목록 (페이지네이션) | 기존 이관 |
| 2 | GET | /notifications/count | 인증 사용자 | 미읽음 카운트 | 기존 이관 (개선) |
| 3 | POST | /notifications/{id}/read | 인증 사용자 | 단건 읽음 | 기존 이관 |
| 4 | POST | /notifications/read-all | 인증 사용자 | 전체 읽음 | 기존 이관 |
| 5 | GET | /settings | 인증 사용자 | 내 알림 설정 | 신규 |
| 6 | PUT | /settings | 인증 사용자 | 내 설정 변경 | 신규 |
| 7 | GET | /settings/subscriptions | 인증 사용자 | 내 구독 목록 | 신규 |
| 8 | PUT | /settings/subscriptions | 인증 사용자 | 구독 변경 | 신규 |
| 9 | GET | /admin/settings/members | ADMIN | 멤버별 설정 조회 | 신규 |
| 10 | PUT | /admin/settings/members/{userId} | ADMIN | 멤버 설정 오버라이드 | 신규 |
| 11 | GET | /remind/evaluation | ADMIN | 평가 리마인드 설정 | 신규 (이관) |
| 12 | PUT | /remind/evaluation | ADMIN | 평가 리마인드 변경 | 신규 (이관) |
| 13 | GET | /remind/meeting | ADMIN | 면접 리마인드 설정 | 신규 (이관) |
| 14 | PUT | /remind/meeting | ADMIN | 면접 리마인드 변경 | 신규 (이관) |
| 15 | GET | /templates | ADMIN | 템플릿 목록 | 신규 |
| 16 | PUT | /templates/{id} | ADMIN | 템플릿 수정 | 신규 |
| 17 | DELETE | /templates/{id} | ADMIN | 템플릿 기본값 복원 | 신규 |

> Base URL: `/service/notification/api/v1.0`

---

## 2. greeting-new-back 변경사항

### 2.1 제거 대상 (notification-service로 완전 이관)

> Feature Flag 전환 완료 후 제거. 과도기에는 `@ConditionalOnProperty`로 비활성화.

#### 2.1.1 communication 패키지 - 알림 발송 관련

| # | 파일 (패키지 경로) | 역할 | 이관 대상 |
|---|-------------------|------|----------|
| 1 | `communication/event/RealTimeAlertEventHandler.kt` | Spring ApplicationEvent 기반 인앱 알림 생성. 13개 이벤트 핸들러 (CreateApplicantDirectly, MentionRecruiter, JoinWorkspace 등) | notification-service의 Kafka Consumer + NotificationEngine |
| 2 | `communication/event/RealTimeAlertSenderEvent.kt` | sealed class, 13개 이벤트 타입 정의 | Kafka 이벤트 payload로 대체 |
| 3 | `communication/event/MailAlertEventHandler.kt` | 이메일 알림 이벤트 핸들러 | notification-service의 EmailChannelSender |
| 4 | `communication/event/MailAlertSenderEvent.kt` | 이메일 알림 이벤트 타입 | Kafka 이벤트 payload로 대체 |
| 5 | `communication/event/SlackAlertEventHandler.kt` | Slack 알림 이벤트 핸들러 | notification-service의 SlackChannelSender |
| 6 | `communication/event/SlackAlertSenderEvent.kt` | Slack 알림 이벤트 타입 | Kafka 이벤트 payload로 대체 |
| 7 | `communication/service/NotificationService.kt` | 수신자 목록 조회 (getRecipientRecruiterUserIds) | notification-service의 RecipientResolver |
| 8 | `communication/domain/Alert.kt` | Alert 도메인 모델 (id, userId, data JSON, alertType) | notification-service의 Notification 엔티티 |
| 9 | `communication/domain/AlertType.kt` | enum: Recruiting, System | NotificationType + NotificationCategory로 확장 |
| 10 | `communication/domain/AlertCriteria.kt` | 알림 조회 조건 | notification-service REST API로 대체 |
| 11 | `communication/domain/AlertMessage.kt` | 알림 메시지 DTO | Kafka 이벤트 payload로 대체 |
| 12 | `communication/entity/Alerts.kt` | JPA Entity (alerts 테이블) | notification-service의 notifications 테이블 |
| 13 | `communication/repository/AlertsRepository.kt` | alerts 테이블 JPA Repository | notification-service에서 관리 |
| 14 | `communication/repository/DetailsOnAlertRepository.kt` | details_on_alert 테이블 Repository | 제거 (metadata JSON으로 통합) |

#### 2.1.2 user 패키지 - 알림 설정 관련

| # | 파일 | 역할 | 이관 대상 |
|---|------|------|----------|
| 1 | `user/domain/AlertFunctions.kt` | enum 9개 (APPLICANT_REGISTERED, MEETING, MENTION 등) | notification-service의 NotificationType enum (확장) |
| 2 | `user/domain/AlertConfig.kt` (추정) | 알림 설정 도메인 모델 | notification-service의 alert_configs 테이블 |
| 3 | `user/entity/AlertConfigs.kt` (추정) | alert_configs JPA Entity | notification-service에서 관리 |

#### 2.1.3 evaluation/meeting 패키지 - 면접 알림 관련

| # | 파일 | 역할 | 이관 대상 |
|---|------|------|----------|
| 1 | `evaluation/meeting/service/SendMeetingRealtimeAlertApplicationService.kt` | 면접 인앱 알림 발송 | notification-service Kafka Consumer |
| 2 | `evaluation/meeting/service/SendMeetingSlackAlertApplicationService.kt` | 면접 Slack 알림 발송 | notification-service SlackChannelSender |
| 3 | `evaluation/meeting/service/SendMeetingMailAlertForUserApplicationService.kt` | 면접 메일(면접관) 발송 | notification-service EmailChannelSender |
| 4 | `evaluation/meeting/service/SendMeetingMailAlertForApplicantApplicationService.kt` | 면접 메일(지원자) 발송 | notification-service EmailChannelSender |
| 5 | `evaluation/meeting/service/GetMeetingSystemAlertMailMetadataService.kt` | 면접 메일 메타데이터 | notification-service 내부 |
| 6 | `evaluation/meeting/service/GetMeetingSystemAlertSlackMetadataService.kt` | 면접 Slack 메타데이터 | notification-service 내부 |

#### 2.1.4 interfaces 패키지 - Kafka Consumer/Controller

| # | 파일 | 역할 | 이관 대상 |
|---|------|------|----------|
| 1 | `interfaces/.../AtsAlertConfigInternalController.kt` | 알림 설정 내부 API | notification-service REST API |
| 2 | `interfaces/.../MeetingRealtimeAlertSendingMessageConsumer.kt` | 면접 인앱 알림 Kafka Consumer | notification-service Consumer |
| 3 | `interfaces/.../MeetingSlackAlertSendingMessageConsumer.kt` | 면접 Slack 알림 Consumer | notification-service Consumer |
| 4 | `interfaces/.../MeetingMailAlertSendingMessageConsumer.kt` | 면접 메일 알림 Consumer | notification-service Consumer |
| 5 | `interfaces/.../MeetingKakaoAlertSendingMessageConsumer.kt` | 면접 카카오 알림 Consumer | notification-service Consumer |
| 6 | `interfaces/.../MeetingExpirationEventConsumerForSlackAlert.kt` | 면접 만료 Slack Consumer | notification-service Consumer |

#### 2.1.5 batch 패키지

| # | 파일 | 역할 | 이관 대상 |
|---|------|------|----------|
| 1 | `batch/.../EvaluationRemindBatchJob.kt` | 평가 리마인드 배치 | notification-service Scheduler |

---

### 2.2 변경 대상 (Kafka 이벤트 발행으로 대체)

기존의 `applicationEventPublisher.publishEvent()` 호출을 Kafka Producer로 교체한다.

#### 2.2.1 신규 Kafka Producer 인터페이스

```kotlin
/**
 * notification-service 전용 이벤트 발행 인터페이스.
 * greeting-new-back의 domain 모듈에 정의하고, interfaces 모듈에서 구현한다.
 */
interface NotificationEventPublisher {

    fun publishEvaluationSubmitted(event: EvaluationSubmittedEvent)
    fun publishEvaluationCompleted(event: EvaluationCompletedEvent)
    fun publishStageEntered(event: StageEnteredEvent)
    fun publishApplicantRegistered(event: ApplicantRegisteredEvent)
    fun publishMentionCreated(event: MentionCreatedEvent)
    fun publishMeetingScheduleChanged(event: MeetingScheduleChangedEvent)
    fun publishFormResponseReceived(event: FormResponseReceivedEvent)
    fun publishMailReceivedFromApplicant(event: MailReceivedFromApplicantEvent)
    fun publishOpeningMemberChanged(event: OpeningMemberChangedEvent)
}
```

#### 2.2.2 Kafka Producer 구현체

```kotlin
@Component
@ConditionalOnProperty(name = ["notification.use.new.service"], havingValue = "true")
class KafkaNotificationEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : NotificationEventPublisher {

    override fun publishEvaluationSubmitted(event: EvaluationSubmittedEvent) {
        val message = NotificationEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "EVALUATION_SUBMITTED",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "greeting-new-back",
            payload = objectMapper.convertValue(event, Map::class.java),
        )
        kafkaTemplate.send(
            "event.notification.evaluation-submitted.v1",
            event.applicantId.toString(),  // partition key
            objectMapper.writeValueAsString(message),
        )
    }

    override fun publishEvaluationCompleted(event: EvaluationCompletedEvent) {
        val message = NotificationEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "EVALUATION_COMPLETED",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "greeting-new-back",
            payload = objectMapper.convertValue(event, Map::class.java),
        )
        kafkaTemplate.send(
            "event.notification.evaluation-completed.v1",
            event.applicantId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }

    override fun publishStageEntered(event: StageEnteredEvent) {
        val message = NotificationEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "STAGE_ENTERED",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "greeting-new-back",
            payload = objectMapper.convertValue(event, Map::class.java),
        )
        kafkaTemplate.send(
            "event.notification.stage-entered.v1",
            event.applicantId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }

    override fun publishApplicantRegistered(event: ApplicantRegisteredEvent) {
        val message = NotificationEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "APPLICANT_REGISTERED",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "greeting-new-back",
            payload = objectMapper.convertValue(event, Map::class.java),
        )
        kafkaTemplate.send(
            "event.notification.applicant-registered.v1",
            event.applicantId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }

    override fun publishMentionCreated(event: MentionCreatedEvent) {
        val message = NotificationEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "MENTION_CREATED",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "greeting-new-back",
            payload = objectMapper.convertValue(event, Map::class.java),
        )
        kafkaTemplate.send(
            "event.notification.mention-created.v1",
            event.openingId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }

    override fun publishMeetingScheduleChanged(event: MeetingScheduleChangedEvent) {
        val message = NotificationEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "MEETING_SCHEDULE_CHANGED",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "greeting-new-back",
            payload = objectMapper.convertValue(event, Map::class.java),
        )
        kafkaTemplate.send(
            "event.notification.remind-schedule.v1",
            event.meetingId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }

    override fun publishFormResponseReceived(event: FormResponseReceivedEvent) {
        val message = NotificationEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "FORM_RESPONSE_RECEIVED",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "greeting-new-back",
            payload = objectMapper.convertValue(event, Map::class.java),
        )
        kafkaTemplate.send(
            "event.notification.form-response-received.v1",
            event.applicantId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }

    override fun publishMailReceivedFromApplicant(event: MailReceivedFromApplicantEvent) {
        val message = NotificationEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "MAIL_RECEIVED_FROM_APPLICANT",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "greeting-new-back",
            payload = objectMapper.convertValue(event, Map::class.java),
        )
        kafkaTemplate.send(
            "event.notification.mail-received.v1",
            event.applicantId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }

    override fun publishOpeningMemberChanged(event: OpeningMemberChangedEvent) {
        val message = NotificationEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "OPENING_MEMBER_CHANGED",
            version = "v1",
            timestamp = Instant.now().toString(),
            source = "greeting-new-back",
            payload = objectMapper.convertValue(event, Map::class.java),
        )
        kafkaTemplate.send(
            "event.notification.opening-member-changed.v1",
            event.openingId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }
}
```

#### 2.2.3 이벤트 Payload 클래스

```kotlin
/**
 * Kafka 메시지 공통 래퍼
 */
data class NotificationEventMessage(
    val eventId: String,
    val eventType: String,
    val version: String,
    val timestamp: String,
    val source: String,
    val payload: Map<*, *>,
)

/**
 * 개별 평가 제출 이벤트
 */
data class EvaluationSubmittedEvent(
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
    val evaluationScore: Double?,
    val submittedAt: Instant,
)

/**
 * 전체 평가 완료 이벤트
 */
data class EvaluationCompletedEvent(
    val workspaceId: Long,
    val applicantId: Long,
    val applicantName: String,
    val stageId: Long,
    val stageName: String,
    val jobPostingId: Long,
    val jobPostingTitle: String,
    val totalEvaluatorCount: Int,
    val completedEvaluatorCount: Int,
    val averageScore: Double?,
    val completedAt: Instant,
)

/**
 * 전형 진입 이벤트
 */
data class StageEnteredEvent(
    val workspaceId: Long,
    val applicantId: Long,
    val applicantName: String,
    val jobPostingId: Long,
    val jobPostingTitle: String,
    val fromStageId: Long?,
    val fromStageName: String?,
    val toStageId: Long,
    val toStageName: String,
    val movedByUserId: Long,
    val movedByUserName: String,
    val enteredAt: Instant,
)

/**
 * 지원자 등록 이벤트 (직접/공고/추천 등 모든 경로 통합)
 */
data class ApplicantRegisteredEvent(
    val workspaceId: Long,
    val applicantId: Long,
    val applicantName: String,
    val openingId: Long,
    val openingTitle: String,
    val registrationType: String,  // "DIRECT" | "PUBLIC" | "RECOMMENDED" | "EMPLOYEE_REFERRAL"
    val registeredByUserId: Long?,
    val registeredAt: Instant,
)

/**
 * 멘션 생성 이벤트
 */
data class MentionCreatedEvent(
    val workspaceId: Long,
    val openingId: Long,
    val applicantId: Long?,
    val mentionedByUserId: Long,
    val mentionedByUserName: String,
    val mentionedUserIds: List<Long>,
    val contextType: String,  // "COMMENT" | "NOTE" | "EVALUATION"
    val contextId: Long?,
    val createdAt: Instant,
)

/**
 * 면접 일정 변경 이벤트 (생성/변경/취소 통합)
 */
data class MeetingScheduleChangedEvent(
    val workspaceId: Long,
    val meetingId: Long,
    val applicantId: Long,
    val applicantName: String,
    val jobPostingId: Long,
    val jobPostingTitle: String,
    val changeType: String,  // "CREATED" | "UPDATED" | "CANCELLED"
    val scheduledAt: Instant?,
    val interviewerUserIds: List<Long>,
    val changedByUserId: Long,
    val changedAt: Instant,
)

/**
 * 양식 응답 수신 이벤트
 */
data class FormResponseReceivedEvent(
    val workspaceId: Long,
    val applicantId: Long,
    val applicantName: String,
    val formId: Long,
    val formTitle: String,
    val isPrivate: Boolean,
    val receivedAt: Instant,
)

/**
 * 지원자 메일 수신 이벤트
 */
data class MailReceivedFromApplicantEvent(
    val workspaceId: Long,
    val applicantId: Long,
    val applicantName: String,
    val mailSubject: String?,
    val receivedAt: Instant,
)

/**
 * 공고 멤버 변경 이벤트 (초대/참가/승인)
 */
data class OpeningMemberChangedEvent(
    val workspaceId: Long,
    val openingId: Long,
    val openingTitle: String,
    val changeType: String,  // "INVITED" | "JOINED" | "JOIN_REQUESTED" | "JOIN_ACCEPTED"
    val changedUserId: Long,
    val changedUserName: String,
    val targetUserIds: List<Long>,
    val changedAt: Instant,
)
```

---

### 2.3 구체적 변경 포인트 (기존 → 신규)

#### 변경 포인트 1: 지원자 등록 시점

**파일:** `greeting-new-back/domain/.../candidate/application/service/ApplicantsServiceImpl.kt`

```kotlin
// ── AS-IS ──
// 지원자 등록 후 Spring ApplicationEvent 발행 → RealTimeAlertEventHandler가 처리
applicationEventPublisher.publishEvent(
    RealTimeAlertSenderEvent.CreateApplicantDirectly(
        applicant = savedApplicant,
        opening = opening,
        workspaceId = workspaceId,
    )
)

// ── TO-BE ──
// Kafka 이벤트 발행 → notification-service가 비동기 처리
notificationEventPublisher.publishApplicantRegistered(
    ApplicantRegisteredEvent(
        workspaceId = workspaceId,
        applicantId = savedApplicant.id,
        applicantName = savedApplicant.name,
        openingId = opening.id,
        openingTitle = opening.title,
        registrationType = "DIRECT",
        registeredByUserId = userId,
        registeredAt = Instant.now(),
    )
)
```

#### 변경 포인트 2: 평가 제출 시점

**파일:** `greeting-new-back/domain/.../evaluation/service/EvaluationServiceImpl.kt` (추정)

```kotlin
// ── AS-IS ──
applicationEventPublisher.publishEvent(
    RealTimeAlertSenderEvent.EvaluationSubmitted(...)  // 존재한다면
)

// ── TO-BE ──
notificationEventPublisher.publishEvaluationSubmitted(
    EvaluationSubmittedEvent(
        workspaceId = workspaceId,
        applicantId = applicant.id,
        applicantName = applicant.name,
        evaluatorUserId = evaluator.userId,
        evaluatorName = evaluator.name,
        evaluationId = evaluation.id,
        stageId = stage.id,
        stageName = stage.name,
        jobPostingId = jobPosting.id,
        jobPostingTitle = jobPosting.title,
        evaluationScore = evaluation.score,
        submittedAt = Instant.now(),
    )
)
```

#### 변경 포인트 3: 전체 평가 완료 시점

```kotlin
// ── AS-IS ──
// 전체 완료 판정 후 3채널 동시 발송 (RealTime + Mail + Slack)
applicationEventPublisher.publishEvent(RealTimeAlertSenderEvent.EvaluationCompleted(...))
applicationEventPublisher.publishEvent(MailAlertSenderEvent.EvaluationCompleted(...))
applicationEventPublisher.publishEvent(SlackAlertSenderEvent.EvaluationCompleted(...))

// ── TO-BE ──
// 단일 Kafka 이벤트 발행, notification-service가 채널별 분기 처리
notificationEventPublisher.publishEvaluationCompleted(
    EvaluationCompletedEvent(
        workspaceId = workspaceId,
        applicantId = applicant.id,
        applicantName = applicant.name,
        stageId = stage.id,
        stageName = stage.name,
        jobPostingId = jobPosting.id,
        jobPostingTitle = jobPosting.title,
        totalEvaluatorCount = totalCount,
        completedEvaluatorCount = completedCount,
        averageScore = avgScore,
        completedAt = Instant.now(),
    )
)
```

#### 변경 포인트 4: 전형 이동 시점

**파일:** `greeting-new-back/domain/.../candidate/application/service/ApplicantsServiceImpl.kt`

```kotlin
// ── AS-IS ──
// 전형 이동 시 알림 없었음 (기존에는 미지원)

// ── TO-BE ──
// 전형 이동 후 이벤트 발행 추가
notificationEventPublisher.publishStageEntered(
    StageEnteredEvent(
        workspaceId = workspaceId,
        applicantId = applicant.id,
        applicantName = applicant.name,
        jobPostingId = jobPosting.id,
        jobPostingTitle = jobPosting.title,
        fromStageId = fromStage?.id,
        fromStageName = fromStage?.name,
        toStageId = toStage.id,
        toStageName = toStage.name,
        movedByUserId = currentUser.id,
        movedByUserName = currentUser.name,
        enteredAt = Instant.now(),
    )
)
```

#### 변경 포인트 5: 면접 생성/변경/취소 시점

**파일:** 면접 관련 서비스 (Meeting 도메인)

```kotlin
// ── AS-IS ──
// MeetingSystemAlertConfiguration 기반, greeting-communication이 처리
// 6개 Kafka Consumer가 각각 채널별로 처리
//   - MeetingRealtimeAlertSendingMessageConsumer
//   - MeetingSlackAlertSendingMessageConsumer
//   - MeetingMailAlertSendingMessageConsumer
//   - MeetingKakaoAlertSendingMessageConsumer
//   - MeetingExpirationEventConsumerForSlackAlert

// ── TO-BE ──
notificationEventPublisher.publishMeetingScheduleChanged(
    MeetingScheduleChangedEvent(
        workspaceId = workspaceId,
        meetingId = meeting.id,
        applicantId = applicant.id,
        applicantName = applicant.name,
        jobPostingId = jobPosting.id,
        jobPostingTitle = jobPosting.title,
        changeType = "CREATED",  // or "UPDATED", "CANCELLED"
        scheduledAt = meeting.scheduledAt,
        interviewerUserIds = meeting.interviewers.map { it.userId },
        changedByUserId = currentUser.id,
        changedAt = Instant.now(),
    )
)
```

#### 변경 포인트 6: 멘션 시점

**파일:** `greeting-new-back/domain/.../user/service/UsersServiceImpl.kt` 등

```kotlin
// ── AS-IS ──
applicationEventPublisher.publishEvent(
    RealTimeAlertSenderEvent.MentionRecruiterV2(
        openingId = openingId,
        mentionedUserIds = mentionedUserIds,
        ...
    )
)

// ── TO-BE ──
notificationEventPublisher.publishMentionCreated(
    MentionCreatedEvent(
        workspaceId = workspaceId,
        openingId = openingId,
        applicantId = applicantId,
        mentionedByUserId = currentUser.id,
        mentionedByUserName = currentUser.name,
        mentionedUserIds = mentionedUserIds,
        contextType = "COMMENT",
        contextId = commentId,
        createdAt = Instant.now(),
    )
)
```

#### 변경 포인트 7: 공고 멤버 변경 시점

```kotlin
// ── AS-IS ──
applicationEventPublisher.publishEvent(
    RealTimeAlertSenderEvent.InvitedOnOpening(...)
)
applicationEventPublisher.publishEvent(
    RealTimeAlertSenderEvent.UserJoinedOnOpening(...)
)
applicationEventPublisher.publishEvent(
    RealTimeAlertSenderEvent.JoinOpeningRequest(...)
)
applicationEventPublisher.publishEvent(
    RealTimeAlertSenderEvent.JoinOpeningRequestAccepted(...)
)

// ── TO-BE ──
// 4개 이벤트를 단일 이벤트 타입으로 통합 (changeType으로 구분)
notificationEventPublisher.publishOpeningMemberChanged(
    OpeningMemberChangedEvent(
        workspaceId = workspaceId,
        openingId = opening.id,
        openingTitle = opening.title,
        changeType = "INVITED",  // "JOINED" | "JOIN_REQUESTED" | "JOIN_ACCEPTED"
        changedUserId = currentUser.id,
        changedUserName = currentUser.name,
        targetUserIds = invitedUserIds,
        changedAt = Instant.now(),
    )
)
```

---

### 2.4 과도기 하위 호환 전략

#### 2.4.1 Feature Flag 설계

```yaml
# application.yml
notification:
  use:
    new:
      service: false  # 기본값: 기존 동작 유지
```

#### 2.4.2 이중 발행기 구현

```kotlin
/**
 * Feature Flag에 따라 기존/신규 알림 경로를 제어하는 이중 발행기.
 *
 * - false (기본): 기존 Spring ApplicationEvent 발행 + 신규 Kafka 이벤트도 shadow 발행
 * - true: 신규 Kafka 이벤트만 발행, 기존 EventHandler 비활성화
 */
@Component
class DualNotificationPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val kafkaNotificationEventPublisher: NotificationEventPublisher?,
    @Value("\${notification.use.new.service:false}")
    private val useNewService: Boolean,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 지원자 등록 알림
     */
    fun publishApplicantRegistered(
        legacyEvent: RealTimeAlertSenderEvent,
        newEvent: ApplicantRegisteredEvent,
    ) {
        if (!useNewService) {
            // Phase 1 (shadow mode): 기존 동작 유지 + 신규 이벤트 병렬 발행 (모니터링용)
            applicationEventPublisher.publishEvent(legacyEvent)
            publishToKafkaSafely(newEvent) { kafkaNotificationEventPublisher?.publishApplicantRegistered(it) }
        } else {
            // Phase 2 (전환 완료): 신규 Kafka 이벤트만 발행
            kafkaNotificationEventPublisher?.publishApplicantRegistered(newEvent)
                ?: log.error("KafkaNotificationEventPublisher is null but useNewService=true")
        }
    }

    /**
     * 평가 완료 알림 (기존 3채널 → 신규 1이벤트)
     */
    fun publishEvaluationCompleted(
        legacyRealtimeEvent: RealTimeAlertSenderEvent,
        legacyMailEvent: MailAlertSenderEvent?,
        legacySlackEvent: SlackAlertSenderEvent?,
        newEvent: EvaluationCompletedEvent,
    ) {
        if (!useNewService) {
            applicationEventPublisher.publishEvent(legacyRealtimeEvent)
            legacyMailEvent?.let { applicationEventPublisher.publishEvent(it) }
            legacySlackEvent?.let { applicationEventPublisher.publishEvent(it) }
            publishToKafkaSafely(newEvent) { kafkaNotificationEventPublisher?.publishEvaluationCompleted(it) }
        } else {
            kafkaNotificationEventPublisher?.publishEvaluationCompleted(newEvent)
        }
    }

    // ... 각 이벤트 타입별 동일 패턴 ...

    /**
     * Shadow mode에서 Kafka 발행 실패해도 기존 동작에 영향 없도록 안전 래핑
     */
    private fun <T> publishToKafkaSafely(event: T, publisher: (T) -> Unit) {
        try {
            publisher(event)
        } catch (e: Exception) {
            log.warn("Shadow mode Kafka 발행 실패 (기존 동작에 영향 없음): event={}", event::class.simpleName, e)
        }
    }
}
```

#### 2.4.3 기존 EventHandler 비활성화

```kotlin
// 기존 RealTimeAlertEventHandler에 ConditionalOnProperty 추가
@Component
@ConditionalOnProperty(
    name = ["notification.use.new.service"],
    havingValue = "false",
    matchIfMissing = true  // 기본값 false → 기존 핸들러 활성화
)
class RealTimeAlertEventHandler(
    // ... 기존 코드 그대로
)

// MailAlertEventHandler, SlackAlertEventHandler도 동일하게 적용
```

#### 2.4.4 전환 단계

```
Phase 0: 준비 (현재)
├── notification.use.new.service = false
├── greeting-notification-service 배포 (Kafka Consumer만 동작, REST API 준비)
├── Kafka 신규 토픽 생성
└── 기존 동작 100% 유지

Phase 1: Shadow Mode (1주차)
├── notification.use.new.service = false (유지)
├── DualNotificationPublisher가 기존 + 신규 Kafka 이벤트 병렬 발행
├── notification-service가 Kafka 이벤트 consume → 알림 생성 (shadow DB 기록)
├── 기존 알림 vs 신규 알림 데이터 비교 검증
└── 불일치 시 신규 로직 수정

Phase 2: 점진적 전환 (2주차)
├── notification.use.new.service = true (워크스페이스 단위 Feature Flag)
├── 10% → 30% → 50% → 70% → 100% 점진 전환
├── 기존 EventHandler 비활성화 (@ConditionalOnProperty)
├── Rollback 기준: 에러율 > 1% or P95 > 500ms
└── API Gateway에서 /service/notification/** 라우팅 활성화

Phase 3: 정리 (3주차)
├── 기존 코드 제거 (2.1절 전체)
├── feature flag 제거
├── 기존 Kafka 토픽 (alert.added) deprecate
├── 기존 alerts, details_on_alert, alert_configs 테이블 아카이브
└── greeting-notification-server, greeting-alert-server 서비스 종료
```

---

## 3. API Gateway 변경

### 3.1 greeting-api-gateway 라우팅 추가

```yaml
# application.yml (Spring Cloud Gateway)
spring:
  cloud:
    gateway:
      routes:
        # ─── 신규: greeting-notification-service ───
        - id: notification-service-v2
          uri: lb://greeting-notification-service
          predicates:
            - Path=/service/notification/api/v1.0/**
            - Header=X-Notification-Version, v2  # Feature Flag 기반
          filters:
            - StripPrefix=0
            - AddRequestHeader=X-Forwarded-Service, notification-service
          metadata:
            connect-timeout: 3000
            response-timeout: 10000

        # ─── 기존: greeting-notification-server (Node.js) ───
        # 과도기 동안 유지, Phase 3에서 제거
        - id: notification-server-legacy
          uri: lb://greeting-notification-server
          predicates:
            - Path=/service/notification/api/v1.0/**
          filters:
            - StripPrefix=0
          metadata:
            connect-timeout: 3000
            response-timeout: 10000
          order: 100  # 낮은 우선순위 (v2 헤더 있으면 위 라우트 우선)

        # ─── WebSocket: notification-service ───
        - id: notification-ws-v2
          uri: lb:ws://greeting-notification-service
          predicates:
            - Path=/socket.io/**
            - Header=X-Notification-Version, v2
          filters:
            - StripPrefix=0
```

### 3.2 Feature Flag 연동 필터

```kotlin
/**
 * API Gateway 글로벌 필터.
 * 프론트엔드가 전달한 Feature Flag 값을 기반으로
 * X-Notification-Version 헤더를 주입한다.
 *
 * 프론트엔드는 LaunchDarkly/ConfigMap에서 feature flag을 읽어
 * 요청 헤더에 포함: X-Feature-Notification-V2: true
 */
@Component
class NotificationVersionRoutingFilter : GlobalFilter, Ordered {

    override fun getOrder() = -1  // 라우팅 전에 실행

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val path = exchange.request.uri.path

        if (path.startsWith("/service/notification/") || path.startsWith("/socket.io/")) {
            val useV2 = exchange.request.headers
                .getFirst("X-Feature-Notification-V2")
                ?.toBoolean() ?: false

            if (useV2) {
                val mutatedRequest = exchange.request.mutate()
                    .header("X-Notification-Version", "v2")
                    .build()
                return chain.filter(exchange.mutate().request(mutatedRequest).build())
            }
        }

        return chain.filter(exchange)
    }
}
```

### 3.3 라우팅 전환 타임라인

| Phase | Gateway 동작 | notification-server (Node.js) | notification-service (Spring) |
|-------|-------------|-------------------------------|-------------------------------|
| Phase 0 | 기존 라우팅만 | 100% 트래픽 | 배포만 (Kafka Consumer) |
| Phase 1 | 기존 라우팅 + shadow 로깅 | 100% 트래픽 | shadow 처리 |
| Phase 2 | Feature Flag 기반 분기 | 100%→70%→30%→0% | 0%→30%→70%→100% |
| Phase 3 | 신규 라우팅만 | 서비스 종료 | 100% 트래픽 |

---

## 4. 기존 RealTimeAlertSenderEvent → 신규 이벤트 매핑 테이블

기존 `RealTimeAlertSenderEvent`의 13개 sealed class가 신규 이벤트 타입으로 어떻게 매핑되는지 정리한다.

| # | 기존 RealTimeAlertSenderEvent | 신규 Kafka Event | 신규 토픽 |
|---|------|------|------|
| 1 | `CreateApplicantDirectly` | `ApplicantRegisteredEvent` (type=DIRECT) | `event.notification.applicant-registered.v1` |
| 2 | `CreateApplicantPublic` | `ApplicantRegisteredEvent` (type=PUBLIC) | `event.notification.applicant-registered.v1` |
| 3 | `RecommendedApplicantRegistered` | `ApplicantRegisteredEvent` (type=RECOMMENDED) | `event.notification.applicant-registered.v1` |
| 4 | `RequestedRecommendationSubmitted` | `ApplicantRegisteredEvent` (type=RECOMMENDATION_LETTER) | `event.notification.applicant-registered.v1` |
| 5 | `DeleteDuplicatedApplicantPublic` | `ApplicantRegisteredEvent` (type=DUPLICATE_DELETED) | `event.notification.applicant-registered.v1` |
| 6 | `InvitedOnOpening` | `OpeningMemberChangedEvent` (type=INVITED) | `event.notification.opening-member-changed.v1` |
| 7 | `UserJoinedOnOpening` | `OpeningMemberChangedEvent` (type=JOINED) | `event.notification.opening-member-changed.v1` |
| 8 | `JoinOpeningRequest` | `OpeningMemberChangedEvent` (type=JOIN_REQUESTED) | `event.notification.opening-member-changed.v1` |
| 9 | `JoinOpeningRequestAccepted` | `OpeningMemberChangedEvent` (type=JOIN_ACCEPTED) | `event.notification.opening-member-changed.v1` |
| 10 | `MentionRecruiter` | `MentionCreatedEvent` | `event.notification.mention-created.v1` |
| 11 | `MentionRecruiterV2` | `MentionCreatedEvent` | `event.notification.mention-created.v1` |
| 12 | `ReceiveFormResponseFromApplicant` | `FormResponseReceivedEvent` | `event.notification.form-response-received.v1` |
| 13 | `ReceiveMailFromApplicantRequest` | `MailReceivedFromApplicantEvent` | `event.notification.mail-received.v1` |
| 14 | `JoinWorkspace` | `OpeningMemberChangedEvent` (type=WORKSPACE_JOINED) | `event.notification.opening-member-changed.v1` |

**기존 대비 신규 추가 이벤트 (기존에 없던 알림):**

| # | 신규 Kafka Event | 토픽 | 비고 |
|---|------|------|------|
| A | `EvaluationSubmittedEvent` | `event.notification.evaluation-submitted.v1` | 개별 평가 제출 시 알림 (신규) |
| B | `EvaluationCompletedEvent` | `event.notification.evaluation-completed.v1` | 전체 평가 완료 시 알림 (신규) |
| C | `StageEnteredEvent` | `event.notification.stage-entered.v1` | 전형 이동 시 알림 (신규) |

---

## 5. 추가 Kafka 토픽 (Part 2 보완)

Part 2에서 정의한 4개 토픽 외에 기존 RealTimeAlertEventHandler의 모든 이벤트를 커버하기 위해 추가 토픽이 필요하다.

| 토픽 | 파티션 | 키 | 보존 | 비고 |
|------|--------|-----|------|------|
| `event.notification.applicant-registered.v1` | 6 | applicantId | 7일 | 신규 (기존 5개 이벤트 통합) |
| `event.notification.mention-created.v1` | 3 | openingId | 7일 | 신규 (기존 2개 이벤트 통합) |
| `event.notification.form-response-received.v1` | 3 | applicantId | 7일 | 신규 |
| `event.notification.mail-received.v1` | 3 | applicantId | 7일 | 신규 |
| `event.notification.opening-member-changed.v1` | 3 | openingId | 7일 | 신규 (기존 4개 이벤트 통합) |
| `event.notification.meeting-schedule-changed.v1` | 6 | meetingId | 7일 | remind-schedule.v1과 분리 |
