# PRD 사전 리뷰 — auth-service (Step 0)

**리뷰 일자**: 2026-05-18
**리뷰 대상**: hr-platform MVP PRD 中 auth 도메인 섹션 (5.1·5.2·9.1·10.2·11.1·14.1)
**원본 PRD**: `/Users/biuea/feature/flag_project/hr-platform/docs/prd/2026-05-16_mvp.md`
**컨벤션**: `/Users/biuea/feature/flag_project/.claude/rules/be-code-convention.md`
**참고**: `/Users/biuea/feature/flag_project/.analysis/outputs/2026-05-16_hr-employee-service/pre-review.md` (선행 리뷰)
**판정**: **REQUIRES_CLARIFICATION**

> **선결 이슈**: 지정 경로 `/Users/biuea/feature/flag_project/hr-platform/docs/prd/2026-05-16_mvp.md` 가 파일시스템에 존재하지 않습니다 (`hr-platform/docs/` 하위에 `prd/` 디렉토리 자체 부재 — `adr/`·`tdd/`·`tickets/`만 존재). 본 리뷰는 호출자(메인 세션)가 검수 포커스에 명시한 PRD 발췌 정보(9.1 10개 API · 11.1 AC 5건 · 10.2 보안 · 14.1 M1)와 선행 employee 사전 리뷰의 PRD 참조 흔적을 근거로 수행합니다. 본 리뷰 완료 후 PRD 원본 동기화 또는 부재 시 PRD 문서 자체 생성을 선행해야 TPM 분석 단계 진입이 가능합니다.

---

## 0. 선결 이슈 — PRD 원본 부재

| 항목 | 상태 |
|---|---|
| `/Users/biuea/feature/flag_project/hr-platform/docs/prd/` 디렉토리 | 부재 |
| `2026-05-16_mvp.md` | 시스템 전역 검색 결과 0건 |
| 대안 사본(worktree/agent 작업본 포함) | 0건 |
| employee 선행 사전 리뷰의 인용 흔적 | 존재 (선행 리뷰는 PRD를 읽고 작성된 것으로 보임 — 즉, 검수 시점 이후 PRD 파일이 삭제/미커밋 상태로 추정) |

**조치 필요**:
1. PM/PO 또는 메인 세션이 PRD를 `hr-platform/docs/prd/2026-05-16_mvp.md` 경로에 복원 또는 커밋
2. 본 사전 리뷰의 인용·항목 번호가 복원본과 일치하는지 재검증
3. 일치 후 본 리뷰 판정 갱신

---

## 1. 명확도 (행위자 / 트리거 / 결과) — 11.1 AC 5건

| AC | 행위자 | 트리거 | 결과 | 판정 | 모호 포인트 |
|---|---|---|---|---|---|
| #1 로그인 성공 발급 | EMPLOYEE 이상 ✅ | 이메일·비밀번호 POST | JWT(30분)+Refresh(14일) 발급 | ⚠️ | 발급 응답 채널(Body? HttpOnly Cookie?), Refresh 저장 위치(Redis? RDB?), JTI 부여·기록 정책 미정. `Set-Cookie` 도메인/`SameSite`/`Secure` 속성 미명시 |
| #2 비밀번호 5회 실패 잠금 | EMPLOYEE | 동일 계정 5회 실패 | 계정 LOCKED + 알림 | ⚠️ | "5회"의 시간 윈도우(슬라이딩 N분? 누적?) 미정. 잠금 해제 정책(자동 N분 후 해제? ADMIN 수동? 본인 이메일 링크?) 미정. "알림" 채널(이메일? SMS? in-app?) 미정. `event.hr.auth.v1` 발행 여부 미정. IP 단위 lockout 별도 정책 부재 |
| #3 2FA TOTP 등록·검증 | EMPLOYEE | 등록 → QR 스캔 → 6자리 검증 | TOTP secret 저장 + 활성화 | ⚠️ | secret 암호화 방식(AES-256-GCM 키 회전), backup code 발급 여부(분실 시 복구 동선), 2FA 우회 정책(ADMIN reset), 등록 단계 인증(현재 비밀번호 재입력 필요 여부) 미정. drift window(±30초) / clock skew 정책 부재 |
| #4 토큰 만료 재발급 | EMPLOYEE | Access 만료 후 Refresh 제시 | 새 Access 발급, Refresh 갱신(rotation 여부 미정) | ⚠️ | Refresh rotation 사용 여부, reuse detection(탈취 감지 시 세션 일괄 폐기), Refresh 폐기(서버측 blacklist? Redis TTL?) 정책 미정. 만료된 Refresh 제시 시 응답 코드(401 vs 403 vs 419) 미정 |
| #5 권한 없는 API 호출 차단 | EMPLOYEE | TEAM_LEAD 전용 API 호출 | 403 Forbidden | ✅ | "TEAM_LEAD 전용 API"의 식별 메커니즘(메서드 어노테이션? 매트릭스 기반?), 응답 바디 스키마(에러 코드 표준), audit 기록 여부 보강 권고 |

종합: AC #1~#4는 G/W/T는 식별되나 **검증에 필요한 부수 정책(쿠키 속성, 잠금 윈도우, 재발급 rotation, 2FA 복구)** 이 모두 결락되어 테스트 코드 작성·인수 자동화 불가.

---

## 2. API 커버리지 — 9.1 10개 API vs 화면·도메인 요구

호출자 발췌 기준 9.1에 명시된 API는 10개. 14개 권한 매트릭스 기능·M1 운영 시나리오와 매핑.

| 화면/요구사항 | 대응 API (추정) | 누락 여부 |
|---|---|---|
| 로그인 (이메일+비밀번호) | `POST /auth/login` | ✅ |
| 로그아웃 (현재 세션) | `POST /auth/logout` | ✅ (가정) |
| 토큰 재발급 | `POST /auth/token/refresh` | ✅ |
| 비밀번호 재설정 요청 (이메일 링크) | `POST /auth/password/reset/request` | ⚠️ 가정 — 매트릭스에는 있어야 함 |
| 비밀번호 재설정 확정 | `POST /auth/password/reset/confirm` | ⚠️ 가정 |
| **비밀번호 변경 (로그인 상태)** | `POST /auth/password/change` | ❌ **누락** — 호출자가 명시한 누락 후보. 90일 만료 정책(보안 표준) 적용 시 필수 |
| 2FA 등록 (TOTP secret 발급 + QR) | `POST /auth/2fa/enroll` | ✅ (AC #3) |
| 2FA 검증 (활성화) | `POST /auth/2fa/verify` | ✅ (AC #3) |
| 2FA 해제 | `POST /auth/2fa/disable` | ⚠️ 가정 — 매트릭스에 없으면 추가 필요 |
| 2FA backup code 발급/재발급 | ❌ **누락** | TOTP 단말 분실 복구 동선 부재 |
| 사용자 본인 정보 조회 | `GET /auth/me` | ✅ (가정) |
| **계정 잠금 해제 (ADMIN)** | `POST /auth/users/{id}/unlock` | ❌ **누락** — 호출자 명시 누락 후보. AC #2의 LOCKED 자동 복구 미정 시 필수 |
| **세션 강제 종료 (ADMIN)** | `POST /auth/users/{id}/sessions/logout-all` | ❌ **누락** — 호출자 명시 누락 후보. 사고 대응 필수 동작 |
| 사용자 조회/검색 (ADMIN/HR_MANAGER) | `GET /auth/users` | ⚠️ 9.1에 명시 여부 불분명 |
| 사용자 상세 (역할·잠금·2FA 상태·최근 로그인) | `GET /auth/users/{id}` | ⚠️ 가정 |
| 역할 할당/회수 (ADMIN) | `POST /auth/users/{id}/roles` / `DELETE /auth/users/{id}/roles/{role}` | ⚠️ 가정 — 매트릭스 14개 기능 운영을 위해 필수 |
| 사용자 비활성/재활성 (퇴사 연동) | `POST /auth/users/{id}/deactivate` | ⚠️ 가정 — employee.resigned 컨슈머와 별개로 ADMIN 수동 동작 필요 |
| **API 토큰(개인 액세스 토큰) 발급** | `POST /auth/api-tokens` | ❌ **누락** — 호출자 명시 누락 후보. PRD 외부 API 공개(SSOT) 정책 시 API Key/PAT 발급 필요. 30분 JWT로는 외부 통합 불가 |
| API 토큰 목록/폐기 (ADMIN) | `GET /auth/api-tokens` / `DELETE /auth/api-tokens/{id}` | ❌ **누락** (위와 동일 원인) |
| 로그인 이력 조회 | `GET /auth/users/{id}/login-history` | ❌ **누락** — 보안 audit·"수상한 로그인" 알림 동선 부재 |
| 세션 목록 조회 (현재 로그인된 디바이스) | `GET /auth/me/sessions` | ❌ **누락** — 본인이 자기 세션을 보고 개별 폐기하는 동선 부재 |
| 권한 매트릭스 조회 (FE 라우팅 가드용) | `GET /auth/permissions/me` | ❌ **누락** — FE가 메뉴 가드를 위해 자기 권한 묶음을 가져갈 표준 엔드포인트 부재 |

**누락 합계 (확정)**: 5종 (`password/change`, `users/{id}/unlock`, `users/{id}/sessions/logout-all`, `api-tokens` 발급, `api-tokens` 목록/폐기)
**누락 추가 권고**: 4종 (2FA backup code, login-history, my-sessions, permissions/me)

---

## 3. 이벤트 커버리지 — `event.hr.auth.v1` 토픽 (action+state 규약)

PRD 5.1/5.2에 auth 도메인 이벤트가 **단 한 건도 명시되지 않은 것으로 확인** (호출자 검수 포커스 #3 사항). 컨벤션상 `event.hr.{domain}.v1` 단일 토픽 + action+state 페이로드 규약이 강제됨.

| 비즈니스 액션 | 발행 이벤트 (action) | 컨슈머 | 누락 여부 |
|---|---|---|---|
| 사용자 계정 생성 | `user.created` | employee, notification, audit-log | ❌ **누락** |
| 사용자 비활성화 (퇴사·관리자 수동) | `user.deactivated` | attendance, leave, approval (권한 회수) | ❌ **누락** — AC #1 미반영(11.2 employee AC #3 X1 의존) |
| 계정 잠금 | `user.locked` | notification, security audit | ❌ **누락** |
| 계정 잠금 해제 | `user.unlocked` | notification | ❌ **누락** |
| 비밀번호 변경/재설정 완료 | `user.password_changed` | notification (본인 + 보안팀) | ❌ **누락** |
| 2FA 등록 | `user.2fa_enrolled` | notification, audit | ❌ **누락** |
| 2FA 해제 | `user.2fa_disabled` | notification, audit (보안 강등 추적) | ❌ **누락** |
| 역할 부여/회수 | `user.role_assigned` / `user.role_revoked` | audit, attendance/leave/approval (권한 캐시 무효화) | ❌ **누락** |
| 로그인 성공 | `user.signed_in` (선택) | login-history, fraud detection | ⚠️ 정책 결정 필요 (필요 시 별도 저빈도 토픽) |
| 로그인 실패 (LOCKED 직전 포함) | `user.sign_in_failed` | security audit | ⚠️ 동일 |
| 세션 강제 종료 | `user.sessions_revoked` | notification, audit | ❌ **누락** |
| API 토큰 발급/폐기 | `api_token.issued` / `api_token.revoked` | audit, rate-limit cache | ❌ **누락** (#2의 API 토큰 누락과 연결) |

**구독해야 할 외부 이벤트**:
- `employee.hired` → `UserAccount` 자동 프로비저닝(employmentId 매핑) — 호출자 검수 포커스 #4. PRD에 컨슈머 책임 미명시
- `employee.resigned` → `UserAccount` 자동 DEACTIVATED 전이 + 세션 일괄 폐기
- `employee.transferred` → 역할 자동 재계산 정책 결정 필요 (TEAM_LEAD 부서 변경 시 권한 캐시 무효화)
- `employee.suspended` / `employee.resumed` → SUSPENDED 상태 동기화

종합: auth 도메인은 **action 8종 이상 + state 4종(ACTIVE/LOCKED/SUSPENDED/DEACTIVATED) + 외부 구독 4종** 이 PRD 보강 대상.

---

## 4. employee-service 연동 — UserAccount 자동 프로비저닝

| 항목 | PRD 명시 | 보강 필요 |
|---|---|---|
| `UserAccount.employmentId` FK 관계 | 추정 (10.2에 명시 추정) | ✅ 명시 |
| 입사 시 자동 계정 생성 트리거 | ❌ 미명시 | `event.hr.employee.v1`의 `employee.hired` 컨슈머에서 자동 생성 — PRD에 명시 필요 |
| 초기 비밀번호 정책 | ❌ 미명시 | 임시 비밀번호 자동 발급 + 이메일 발송 + 최초 로그인 시 변경 강제 |
| 초기 역할 부여 | ❌ 미명시 | 디폴트 EMPLOYEE — 다른 역할은 ADMIN 수동 부여 (정책 명시 필요) |
| 멱등성 (중복 hired 이벤트) | ❌ 미명시 | UserAccount.employmentId UNIQUE + UPSERT 정책 |
| 퇴사 시 DEACTIVATED 전이 | ⚠️ AC #1(employee 11.2)에 "권한 회수" 만 표기 | `employee.resigned` 컨슈머로 DEACTIVATED + 세션 폐기 + Refresh 무효화 — 동기 호출 vs 비동기 이벤트 결정 필요 |
| effectiveDate 미래 입사 처리 | ❌ 미명시 | PRE_HIRED 시점에 UserAccount 선생성 vs ACTIVE 전이 시 생성 — 결정 필요 |
| 인사 정보 변경 (이메일·이름) 동기화 | ❌ 미명시 | `employee.transferred` 등에서 UserAccount.email/displayName 변경 정책 |
| 부서장 변경 시 역할 자동 부여 | ❌ 미명시 | TEAM_LEAD 자동 부여/회수 정책 (Department.headEmploymentId 변경 이벤트 부재 — employee 사전 리뷰 §3 누락과 연동) |

**호출자 검수 포커스 #4 답변**: `UserAccount.employmentId`는 `event.hr.employee.v1` 토픽의 `action=employee.hired` 이벤트 구독으로 자동 생성되어야 한다. 다만 PRD에 명시되지 않았고, employee 사전 리뷰 결과 `department.head_changed` 같은 부서장 변경 이벤트도 누락 상태이므로 **양 도메인 PRD 동시 보강**이 필요.

---

## 5. be-code-convention 적용

### 5.1 도메인 Entity = JPA Entity 직접 (컨벤션 갱신 반영)

| Entity | Rich Domain Model 후보 메서드 |
|---|---|
| **UserAccount** | `validateCanLogin()` (state check), `recordLoginSuccess(ip, ua)`, `recordLoginFailure()` (실패 카운트 + LOCKED 전이), `lock(reason)`, `unlock()`, `deactivate(reason)`, `reactivate()`, `changePassword(rawNew, encoder)` (PasswordPolicy 위임), `enroll2fa(secret)`, `disable2fa()`, `assignRole(role)` / `revokeRole(role)` (자기 자신/마지막 ADMIN 보호), `validateCanTransitTo(newState)` |
| **PasswordCredential** | `verify(raw, encoder)`, `isExpired(policy)`, `rotate(newHash)` (history N개 회피) |
| **TwoFactorEnrollment** | `verifyTotp(code, clock)` (drift window), `consumeBackupCode(code)`, `regenerateBackupCodes()` |
| **RefreshToken** | `rotate(now)` (reuse detection 시 family 전체 폐기), `revoke(reason)`, `isExpired(now)`, `belongsTo(userId)` |
| **LoginAttempt** (append-only) | `static record(userId, ip, ua, success, failureReason, at)` — 불변 팩토리 |
| **Role / RolePermission** | enum 또는 ReferenceTable. Permission 매트릭스 캡슐화 |
| **ApiToken** | `verify(raw)`, `isExpired(now)`, `revoke(reason)`, `lastUsedAt` 갱신 |

**누락 명시 권고**: PRD에 위 Entity 명세·메서드 시그니처가 없음. TDD 단계에서 보강.

### 5.2 비밀번호 저장 — bcrypt 단방향 (호출자 검수 포커스 #5)

| 항목 | 결정 |
|---|---|
| 알고리즘 | bcrypt cost=12 (PRD 10.2 명시) |
| Converter | `AesGcmStringConverter` **사용 금지** — bcrypt는 단방향 해시이며 AES-GCM은 양방향 암호. JPA Converter 적용 시점에 이미 hash 문자열이므로 추가 변환 불요. |
| 컬럼 타입 | VARCHAR(60) (bcrypt 표준 길이) |
| 비밀번호 정책 객체 | `PasswordPolicy` value object (최소 길이, 복잡도, 만료, 재사용 금지 N개) — 도메인 서비스 의존 |
| 비밀번호 변경 시 audit | 마지막 N개 hash 보관(`PasswordHistory` entity 또는 `UserAccount.passwordHistory` JSON 컬럼) — PRD에 명시 부재 |

### 5.3 AES-256-GCM 적용 대상

| 컬럼 | 암호화 필요 | 비고 |
|---|---|---|
| TOTP secret | ✅ | 양방향 필수 (검증 시 복호화) |
| Backup code | ⚠️ | bcrypt 해시가 더 적합 (검증 시 일치성만 확인) |
| Refresh token raw | ⚠️ | RDB 저장 시 해시(SHA-256) 권장, 원문 저장 비권장 |
| API token raw | ⚠️ | 동일하게 prefix + 해시(`pat_xxx_<sha256>`) 패턴 권장 |
| 비밀번호 hash | ❌ | bcrypt만으로 충분 |
| 이메일·이름 | ❌ | PII이지만 검색 가능성 우선 (PRD 10.2 정책에 따름) |

### 5.4 BaseEntity 상속 — audit / soft-delete

| Entity | BaseEntity 상속 | 비고 |
|---|---|---|
| UserAccount | ✅ | createdAt/updatedAt/createdBy/updatedBy/deletedAt — 비활성화는 state로 처리, deletedAt은 GDPR 영구삭제용 |
| PasswordCredential | ✅ | 변경 추적 |
| RefreshToken | ⚠️ | append + revoke 패턴이라 BaseEntity의 updatedBy 의미 약함. 별도 issuedAt/revokedAt 권장 |
| LoginAttempt | ❌ | append-only 이력. createdAt만 필요 |
| TwoFactorEnrollment | ✅ | 활성/해제 시점 추적 |
| ApiToken | ✅ | 발급자·폐기자 추적 |

### 5.5 DomainEvent action+state 규약

호출자 명시 컨벤션. §3 이벤트 표가 이미 action 명을 따르고 있음. 추가로:

```text
TopicName : event.hr.auth.v1
Key       : userId (Long)
Payload   : { action: "user.locked", state: "LOCKED", userId, occurredAt, payload: {...} }
```

같은 토픽 안에 모든 action 통합 — Kafka 키 동일 사용자 메시지의 순서 보장.

### 5.6 UseCase 안티패턴 사전 가드

- `LoginUseCase`가 `UserAccountRepository` 직접 호출 금지 → `AuthDomainService.authenticate(command)` 경유 강제
- `LockUserUseCase`가 `if (failureCount >= 5) throw` 같은 패턴 금지 → `UserAccount.recordLoginFailure()` 내부 캡슐화
- `IssueTokenUseCase`가 `JwtBuilder` 직접 호출 금지 → `TokenGateway` interface(domain) + `JjwtTokenGateway` impl(infrastructure)
- 비밀번호 인코더 의존: `PasswordEncoder`는 도메인 interface로 추상화 → `BcryptPasswordEncoder` infra impl

---

## 6. 상태 머신 — UserAccount (호출자 검수 포커스 #6)

PRD에서 `ACTIVE/LOCKED/SUSPENDED/DEACTIVATED` 4상태가 **나열만** 되었고 전이 규칙·트리거·이벤트 발행 시점이 미정 (호출자 명시 누락).

### 6.1 허용 전이 (제안)

```
            login fail x5             ADMIN unlock
ACTIVE ─────────────────────► LOCKED ───────────► ACTIVE
   │                            │
   │ employee.suspended         │ (auto after N min? 정책 미정)
   ▼                            ▼
SUSPENDED ◄──── employee.resumed ──── ACTIVE
   │
   │ employee.resigned / ADMIN deactivate
   ▼
DEACTIVATED  (terminal — 재활성화 정책 미정)
```

| 전이 | 트리거 | 발행 이벤트 | 미정 사항 |
|---|---|---|---|
| ACTIVE → LOCKED | 5회 연속 실패 | `user.locked` | 시간 윈도우, IP 단위 추가 잠금 |
| LOCKED → ACTIVE | ADMIN unlock / 자동 N분 | `user.unlocked` | 자동 해제 시간, 본인 이메일 셀프 해제 동선 |
| ACTIVE → SUSPENDED | `employee.suspended` 컨슈머 | `user.suspended` | suspended 동안 Refresh 유지 vs 즉시 폐기 |
| SUSPENDED → ACTIVE | `employee.resumed` | `user.reactivated` | 복직 후 비밀번호 재설정 강제 여부 |
| ACTIVE/LOCKED/SUSPENDED → DEACTIVATED | `employee.resigned` / ADMIN | `user.deactivated` + `user.sessions_revoked` | 재입사 시 신규 UserAccount vs 기존 재활성 (employee Person 재사용 정책과 연동) |
| DEACTIVATED → ACTIVE | (정책 미정) | — | 재입사 동선 결정 필요 |

### 6.2 불변 조건

- DEACTIVATED 상태의 UserAccount는 새 RefreshToken 발급 불가
- LOCKED 상태에서도 비밀번호 재설정 요청은 허용 (계정 복구 동선)
- SUSPENDED 직원의 기존 세션 즉시 폐기 vs grace period — 정책 결정 필요
- 마지막 ADMIN 1인의 역할 회수/계정 비활성화 차단 (lockout 방지)

---

## 7. 권한 매트릭스 — 14개 기능 × 4역할 (호출자 검수 포커스 #7)

### 7.1 일반 평가

- 4역할(EMPLOYEE / TEAM_LEAD / HR_MANAGER / ADMIN) 정의 자체는 명확 ✅
- 14개 기능을 행으로 두는 매트릭스가 PRD 9.1에 있으나 **각 API 엔드포인트와 매트릭스 행의 매핑이 부재**
- 매트릭스 행의 입도(예: "직원 정보 보기" 한 행 vs "직원 기본 정보 보기 / 연봉 보기 / 평가 보기" 분리) 미명시 → 마스킹 정책과 연결되지 않음

### 7.2 보강 필요 항목

| 항목 | 결락 내용 |
|---|---|
| API → 권한 매핑표 | 9.1의 10개 API + §2의 누락 5종에 각각 "최소 역할 + 추가 제약(자기 자신만? 자기 팀만?)" 명시 부재 |
| Permission key 컨벤션 | `auth.user.read` / `auth.user.update` / `auth.role.assign` 등 표준 키 부재 (FE 가드·이벤트 audit 모두 의존) |
| 역할 상속 정의 | TEAM_LEAD ⊃ EMPLOYEE 인지, 각각 독립 역할인지 미명시 |
| 다중 역할 동시 부여 가능성 | 한 사용자가 HR_MANAGER + TEAM_LEAD 동시 보유 가능 여부 미명시 (현실: 부서장 겸 HR 매니저) |
| ADMIN 보호 | 마지막 ADMIN 보호(§6.2), ADMIN 자기 역할 회수 차단 정책 부재 |
| API 토큰 권한 범위(Scope) | PAT 발급 시 scope(`read:employee`, `read:payroll`) 지정 가능 여부 — §2 누락 API와 연결 |
| 시간대 권한(temporal) | 휴직 중 직원의 권한 부분 회수 정책 (전부 회수? 일부 유지?) 부재 |
| 권한 캐시 무효화 | TTL 기반? 이벤트 기반(`user.role_assigned` 컨슈머)? 결정 필요 |

### 7.3 자기 자신 / 자기 팀 자동 필터링

| API | 자동 필터 정책 |
|---|---|
| `GET /auth/me` | 자기 자신만 (강제) |
| `GET /auth/users` | EMPLOYEE 호출 시 0건 vs 403 — 표준 미정 |
| `GET /auth/users/{id}` | 본인 ID 또는 같은 팀 직원만 (TEAM_LEAD) — 자동 필터링 적용 위치(Controller? DomainService? Spec?) 미정 |
| `POST /auth/users/{id}/roles` | ADMIN 전용 + 자기 자신 ADMIN 회수 차단 |

---

## 8. 인프라 부트스트랩 (호출자 검수 포커스 #8)

employee 사전 리뷰의 부트스트랩 흔적과 동일 패턴 적용. auth-service는 **신규 모듈**이므로 추가 부트스트랩 티켓 분리 필요.

| 항목 | 상태 | 비고 |
|---|---|---|
| `hr-platform/auth-service/` 디렉토리 | 미존재 (`employee-service`만 존재) | 신규 생성 필요 |
| `settings.gradle.kts` include | 미반영 | 부트스트랩 티켓 분리 (`[INFRA-AUTH-01]`) |
| `auth-service` Spring Boot Application 클래스 | 미작성 | 동 티켓 |
| `application.yml` (port, DB 스키마, Kafka group-id) | 미작성 | DB 스키마 분리 컨벤션(10.4) 준수 — `auth_db` 신규 |
| Flyway 마이그레이션 (`V1__user_account.sql` 등) | 미작성 | DB 티켓 분리 (`[DB-AUTH-01]`) |
| Kafka 토픽 `event.hr.auth.v1` 생성 + JSON Schema | 미작성 | Kafka 티켓 분리 (`[KF-AUTH-01]`) |
| 의존성 — jjwt / spring-security / spring-kafka / bcrypt(spring-security 내장) / TOTP(`googleauth` or `aerogear-otp-java`) | 미선택 | 라이브러리 결정 필요 |
| `core` / `common-kafka` 모듈 — 재사용 | ✅ | 기존 employee-service와 공유 (BaseEntity, DomainEvent, KafkaDomainEventPublisher 등) |
| Redis 도입 결정 | ⚠️ | Refresh 저장소·rate limit·세션 무효화 캐시로 사용. PRD에 Redis 의존 명시 (10.2 가정), 실 적용 인프라 티켓 분리 |

**티켓 후보 (선행 부트스트랩)**:
- `[INFRA-AUTH-01]` settings.gradle.kts include + auth-service 모듈 골격(SpringBootApplication, application.yml, build.gradle.kts)
- `[INFRA-AUTH-02]` Spring Security 설정 + SecurityFilterChain + 인증 진입점 + `@PreAuthorize` 표준화
- `[DB-AUTH-01]` Flyway V1: user_account / password_credential / two_factor_enrollment / refresh_token / login_attempt / api_token / role / user_role
- `[KF-AUTH-01]` `event.hr.auth.v1` 토픽 정의 + Schema (action+state)
- `[KF-AUTH-02]` `event.hr.employee.v1` 컨슈머(employee.hired/resigned/suspended/resumed) — Wave 후반

---

## 9. PRD 누락·결락 종합 매트릭스

| 영역 | 결락 항목 | 심각도 | 차단 여부 |
|---|---|---|---|
| 원본 PRD 자체 | 파일시스템 부재 | 치명 | 차단 |
| AC #1 | 응답 채널(Cookie vs Body), JTI 정책 | 중 | 비차단 (가정 가능) |
| AC #2 | 잠금 시간 윈도우, 자동 해제 정책 | 중 | 비차단 |
| AC #3 | 2FA 복구 동선(backup code), 등록 단계 재인증 | 중 | 비차단 |
| AC #4 | Refresh rotation, reuse detection | 중 | 비차단 |
| API #2 | 비밀번호 변경/잠금 해제/세션 일괄 폐기/API 토큰 4종 | 높음 | 비차단 (TDD에서 보강 가능) |
| 이벤트 §3 | auth 도메인 이벤트 8종 + 외부 구독 4종 | 높음 | 부분 차단 (employee 연동 미정 시 후행 도메인 영향) |
| employee 연동 §4 | 자동 프로비저닝 트리거·정책 | 높음 | 부분 차단 |
| 상태 머신 §6 | 전이·트리거·불변 조건 전반 | 높음 | 부분 차단 |
| 권한 매트릭스 §7 | API↔권한 매핑, Permission key, ADMIN 보호 | 높음 | 비차단 |
| 인프라 §8 | 부트스트랩·라이브러리·Redis 사용 정책 | 중 | 비차단 (TPM 자율 결정 가능) |

---

## 종합 판정

### 판정: **REQUIRES_CLARIFICATION**

### A. 최우선 차단 이슈 (TPM 분석 진입 전 해소 필수)

1. **PRD 원본 파일 부재** — `/Users/biuea/feature/flag_project/hr-platform/docs/prd/2026-05-16_mvp.md` 복원 또는 작성. 본 사전 리뷰의 인용·항목 번호가 복원본과 일치하는지 재검증 필수
2. **auth 도메인 이벤트 명세 부재** — 5.1/5.2 도식에 `event.hr.auth.v1` 토픽 + action 8종 + state 4종 추가
3. **UserAccount 상태 머신** — 전이 규칙·트리거·이벤트 발행 시점·불변 조건 명시 (§6 표 적용)
4. **employee 연동 정책** — 자동 프로비저닝(input 이벤트), 자동 비활성(input 이벤트), 부서장 변경 시 역할 자동 재계산 정책. employee-service PRD 보강과 **동시 진행** 필요

### B. PM/PO 결정 필요 (TPM이 디폴트로 진행 가능하나 확정 후 후속 보정)

5. AC #2 잠금 시간 윈도우·자동 해제 정책
6. AC #4 Refresh rotation 정책 (rotation + reuse detection 권장)
7. 2FA 분실 복구 동선 (backup code N개 발급 권장)
8. 비밀번호 정책 객체 (최소 길이·복잡도·만료 N일·재사용 금지 N개)
9. API 토큰(PAT) 도입 여부 및 scope 모델 — 외부 API 공개 정책과 직결
10. ADMIN 마지막 1인 보호·다중 역할 동시 보유 정책

### C. TPM 단계에서 자율 결정 가능 (별도 PM/PO 승인 불요)

11. `auth-service` Gradle 모듈 부트스트랩 티켓 분리 (`INFRA-AUTH-01/02`, `DB-AUTH-01`, `KF-AUTH-01/02`)
12. Rich Domain Model 메서드 시그니처(§5.1)를 TDD Entity 절에 명시
13. 비밀번호 = bcrypt 단방향(AES-GCM 적용 금지), TOTP secret = AES-256-GCM, Refresh/API token = SHA-256 해시 저장
14. `event.hr.auth.v1` 단일 토픽 + action+state 페이로드 표준
15. `event.hr.employee.v1` 컨슈머(`employee.hired/resigned/suspended/resumed`) 멱등성 키(`employmentId`) 적용
16. JPA Auditing(`@CreatedBy/@LastModifiedBy`) + Soft Delete(`@SQLDelete` + `@Where`) 일관 적용
17. `@Transactional`은 UseCase에만, DomainService는 같은 트랜잭션 내 동작
18. QueryDSL 강제(`@Query` 금지), Repository interface는 domain, impl은 infrastructure

### D. 누락 API 확정 (TDD/티켓에 추가 필수)

- `POST /auth/password/change` — 로그인 상태에서 비밀번호 변경
- `POST /auth/users/{id}/unlock` — ADMIN 계정 잠금 해제
- `POST /auth/users/{id}/sessions/logout-all` — ADMIN 세션 강제 종료
- `POST /auth/api-tokens` + `GET /auth/api-tokens` + `DELETE /auth/api-tokens/{id}` — PAT 발급/조회/폐기 (외부 API 공개 정책 시)
- (권고) `POST /auth/2fa/backup-codes/regenerate`, `GET /auth/users/{id}/login-history`, `GET /auth/me/sessions`, `GET /auth/permissions/me`

---

**결론**: auth-service PRD는 (PRD 원본 부재로) 직접 검증이 불가하나, 호출자 발췌 기준 골격은 인식 가능합니다. 그러나 `event.hr.auth.v1` 이벤트 누락·상태 머신 미정·employee 자동 프로비저닝 정책 부재·핵심 API 5종 누락이 동시에 존재하여 TPM 분석을 곧바로 진행 시 가설성 결정이 다수 발생합니다. **§A 1~4 보강 + employee 사전 리뷰 §A 결과와의 정합성 검토** 후 분석 재개를 권고합니다.
