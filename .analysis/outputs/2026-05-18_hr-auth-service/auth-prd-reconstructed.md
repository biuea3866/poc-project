# PRD 재구성 — auth-service (메모리 기반)

**주의**: 원본 PRD(`hr-platform/docs/prd/2026-05-16_mvp.md`)가 main에 commit되지 않아 분실됨. 이 문서는 employee-service 작업 중 누적된 컨텍스트 메모리에서 auth 관련 부분을 재구성한 reference입니다. 사용자 검증 권장.

## 5.1 도메인 위치

auth-service는 hr-platform 7개 도메인 중 하나로, 모든 API의 인증·인가를 담당. employee-service의 UserAccount 대신 auth-service가 단독 소유.

```
┌─────────┐  ┌──────────┐
│  auth   │  │ employee │
│         │  │  (SSOT)  │
└─────────┘  └──────────┘
```

## 5.2 도메인 책임

| 도메인 | 책임 | SSOT 객체 |
|--------|------|---------|
| **auth** | 인증·인가, JWT 발급, 역할 관리 | UserAccount, Role |

## 9.1 auth-service 명세

### 주요 화면
- 로그인 페이지 (이메일/PW, "회사 SSO로 로그인" Phase 1.5)
- 비밀번호 재설정 페이지
- 2FA 등록/확인 페이지
- 권한 관리 페이지 (HR 매니저 전용)

### API (10개 + 보강 5개 = 15개)

| 메서드 | 경로 | 책임 |
|--------|------|------|
| POST | /auth/login | 이메일/PW 로그인 → JWT 발급 |
| POST | /auth/logout | 토큰 무효화 |
| POST | /auth/refresh | 리프레시 토큰으로 액세스 토큰 재발급 |
| POST | /auth/password-reset/request | 비밀번호 재설정 메일 발송 |
| POST | /auth/password-reset/confirm | 토큰 + 새 비밀번호로 변경 |
| POST | /auth/2fa/enroll | 2FA 등록 (TOTP) |
| POST | /auth/2fa/verify | 2FA 코드 검증 |
| GET | /auth/me | 내 정보 + 권한 |
| GET | /auth/roles | 회사 전체 역할 목록 (HR_MANAGER+) |
| POST | /auth/users/{id}/roles | 역할 할당 (HR_MANAGER+) |
| **POST** | **/auth/password/change** | 본인 비밀번호 변경 (보강) |
| **POST** | **/auth/users/{id}/unlock** | 계정 잠금 해제 (HR_MANAGER+, 보강) |
| **POST** | **/auth/users/{id}/sessions/logout-all** | 세션 강제 종료 (보강) |
| **POST** | **/auth/api-tokens** | API 토큰 발급 (외부 API용, 보강) |
| **DELETE** | **/auth/api-tokens/{id}** | API 토큰 폐기 (보강) |

### 권한 매트릭스 (역할 × 기능)

| 기능 | EMPLOYEE | TEAM_LEAD | HR_MANAGER | ADMIN |
|------|:-:|:-:|:-:|:-:|
| 로그인/로그아웃 | ● | ● | ● | ● |
| 자기 정보 보기 | ● | ● | ● | ● |
| 자기 비밀번호 변경 | ● | ● | ● | ● |
| 2FA 등록 (자기) | ● | ● | ● | ● |
| 팀원 정보 보기 | ○ | ● | ● | ● |
| 전체 직원 정보 보기 | ○ | ○ | ● | ● |
| 역할 할당 | ○ | ○ | ● | ● |
| 계정 잠금/해제 | ○ | ○ | ● | ● |
| 세션 강제 종료 (타인) | ○ | ○ | ● | ● |
| 권한·역할 관리 | ○ | ○ | ◐ | ● |
| API 토큰 발급 | ○ | ○ | ● | ● |

## 10.2 보안 요구사항

- 모든 API HTTPS (TLS 1.2+)
- **JWT 액세스 토큰 만료 30분 / 리프레시 14일**
- **비밀번호: bcrypt cost 12, 최소 10자 + 영숫특 조합**
- **2FA(TOTP) 옵션** — 등록 시 비밀키 + QR 코드 + 백업 코드 5개
- 비밀번호 5회 연속 실패 시 15분 잠금 + 알림 메일
- 토큰 무효화는 Redis SET (jti blacklist)
- 모든 데이터 변경 audit log (who/what/when/oldValue/newValue, 5년 보관)

## 11.1 인수 기준 (Acceptance Criteria)

1. **Given** 활성 사용자가 올바른 이메일/PW를 입력 **When** 로그인 **Then** JWT 액세스+리프레시 토큰을 받고 200을 반환한다
2. **Given** 비밀번호가 5회 연속 틀림 **When** 6번째 시도 **Then** 15분 잠금 + 알림 메일 발송
3. **Given** 2FA 등록된 사용자 **When** 로그인 **Then** OTP 6자리 추가 입력 화면이 나타난다
4. **Given** HR_MANAGER가 EMPLOYEE에게 TEAM_LEAD 역할을 부여 **When** 해당 사용자가 재로그인 **Then** TEAM_LEAD 전용 메뉴가 활성화된다
5. **Given** 만료된 액세스 토큰 **When** API 호출 **Then** 401 + 코드 `TOKEN_EXPIRED`를 반환

## employee-service 연동

auth-service는 employee-service의 다음 이벤트를 구독:
- `event.hr.employee.v1` — `EmployeeHired` 수신 → UserAccount 자동 생성 (status=ACTIVE, 기본 role=EMPLOYEE)
- `EmployeeResigned` 수신 → UserAccount.status=DEACTIVATED + 모든 세션 강제 종료 (X1 시나리오)
- `EmployeeSuspended` 수신 → UserAccount.status=SUSPENDED (로그인 차단)
- `EmployeeResumed` 수신 → UserAccount.status=ACTIVE (로그인 재개)

auth-service가 발행하는 이벤트:
- `event.hr.auth.v1` 토픽 + 다음 이벤트 종류 (보강):
  - `UserCreated` (action=CREATE) — employee.hired 수신 후
  - `UserLocked` (action=LOCK) — 5회 실패 시
  - `UserUnlocked` (action=UNLOCK)
  - `UserSuspended` / `UserReactivated` / `UserDeactivated`
  - `UserRoleAssigned` / `UserRoleRevoked`
  - `UserPasswordChanged`
  - `UserTwoFactorEnrolled` / `UserTwoFactorDisabled`
  - `UserSessionStarted` / `UserSessionTerminated` (옵션)

## UserAccount 상태 머신

```
[*]  ─create──▶  ACTIVE  ─lock(5회 실패)──▶  LOCKED
                  │                            │
                  │                            └─unlock──▶ ACTIVE
                  │
                  ├─suspend──▶  SUSPENDED  ─reactivate──▶ ACTIVE
                  │
                  └─deactivate──▶  DEACTIVATED  (재활성화 불가, 새 UserAccount 생성)
LOCKED  ─[15분 자동 해제]──▶ ACTIVE (백오프 잠금)
```

## 핵심 엔티티 (개념 모델)

### UserAccount (auth의 SSOT)
- id, employmentId(FK to employee), companyId, email, passwordHash (bcrypt cost 12), status (ACTIVE/LOCKED/SUSPENDED/DEACTIVATED)
- failedLoginAttempts, lockedUntil (ZonedDateTime), lastLoginAt
- 2FA 관련: twoFactorEnabled, twoFactorSecret (AES-256-GCM 암호화, base32)
- audit + soft-delete 자동 (BaseEntity)

### Role
- id, companyId, code (EMPLOYEE/TEAM_LEAD/HR_MANAGER/ADMIN), name, description
- isSystemRole (boolean) — 시스템 정의 역할은 수정 불가

### UserAccountRole (M:N 관계)
- id, userAccountId, roleId, assignedAt, assignedBy (audit)

### RefreshToken
- id, userAccountId, tokenHash (SHA-256), expiresAt, deviceInfo, ipAddress
- 14일 만료 + 단일 사용 (rotation)

### LoginAttempt (시도 이력, append-only)
- id, userAccountId (nullable, 존재하지 않는 이메일은 null), email, attemptedAt, success, failureReason, ipAddress, userAgent

### TwoFactorBackupCode
- id, userAccountId, codeHash (bcrypt), usedAt (사용 후 단일 사용)

### ApiToken (외부 API용)
- id, userAccountId, name, tokenHash (SHA-256), scopes (JSON), expiresAt, lastUsedAt, revokedAt

## 비기능 요구사항

- 로그인 API p95 < 500ms (bcrypt 포함)
- 토큰 검증 API < 10ms (JWT verify only, in-memory)
- 5,000 req/s 무중단 (출근 피크 대응)
- Redis 세션·잠금·blacklist 캐시
- SOC2 Type 2 대비 audit log 5년 보관

## be-code-convention 적용 (필수)

- 도메인 Entity = JPA Entity 직접 (UserAccount·Role·UserAccountRole·RefreshToken·LoginAttempt·TwoFactorBackupCode·ApiToken — 7개 모두 `@Entity` 직접 부착, shadow 금지)
- BaseEntity 상속 → audit 6컬럼 자동
- DomainEvent action+state 페이로드 규약 (위 8~12종 이벤트)
- 토픽 `event.hr.auth.v1` (KF-02 패턴 동일)
- 비밀번호는 bcrypt (AesGcmStringConverter 아님), 2FA secret만 AES
- `@Query` 금지, QueryDSL
- @Transactional은 UseCase에
- detekt 전역 룰 변경 금지
