# QA Release Readiness Report
**Date:** 2026-03-15
**Tester:** QA Engineer (Claude)

## 인프라 상태
| 서비스 | 상태 | 비고 |
|--------|------|------|
| MySQL (3306) | ✅ | Docker `wiki-mysql` 컨테이너 정상 |
| Redis (6379) | ✅ | Docker `wiki-redis` 컨테이너 정상, PONG 응답 |
| BE API (8081) | ✅ | Docker `wiki-api` 컨테이너, 포트 8081 (8080 아님) |
| FE (3000) | ✅ | Tailwind 설정 수정 후 정상 기동 |

## API 테스트 결과

> **참고:** 실제 BE API 엔드포인트는 `/signup`, `/login`, `/logout` (요구사항의 `/sign-up`, `/sign-in`, `/sign-out`과 다름)

| # | 시나리오 | 결과 | 비고 |
|---|---------|------|------|
| 1 | 회원가입 (`POST /api/v1/auth/signup`) | ✅ | 200 OK, `{"id":2,"email":"qa@test.com","name":"QA Tester"}` |
| 2 | 로그인 (`POST /api/v1/auth/login`) | ✅ | 200 OK, accessToken + refreshToken + user 정상 반환 |
| 3 | 문서 생성 (`POST /api/v1/documents`) | ❌ | 403 — **Document Controller 미구현** |
| 4 | 문서 발행 (`POST /api/v1/documents/{id}/publish`) | ❌ | 403 — Document Controller 미구현 |
| 5 | AI 상태 (`GET /api/v1/documents/{id}/ai-status`) | ❌ | 403 — Document Controller 미구현 |
| 6 | 문서 목록 (`GET /api/v1/documents`) | ❌ | 403 — Document Controller 미구현 |
| 7 | 검색 (`GET /api/v1/search/integrated`) | ❌ | 403 — Search Controller 미구현 |
| 8 | 소프트 삭제 (`DELETE /api/v1/documents/{id}`) | ❌ | 403 — Document Controller 미구현 |
| 9 | Trash 조회 (`GET /api/v1/documents/trash`) | ❌ | 403 — Document Controller 미구현 |
| 10 | 문서 복구 (`POST /api/v1/documents/{id}/restore`) | ❌ | 403 — Document Controller 미구현 |
| 11 | Token Refresh (`POST /api/v1/auth/refresh`) | ✅ | 200 OK, 새 accessToken 정상 발급 |
| 12 | 로그아웃 (`POST /api/v1/auth/logout`) | ✅ | 204 No Content, 정상 처리 |

## FE 스모크 테스트
| 페이지 | HTTP 상태 | 비고 |
|--------|---------|------|
| / | 200 | ✅ |
| /login | 200 | ✅ |
| /signup | 200 | ✅ |
| /dashboard | 200 | ✅ |

## 발견된 버그

### BUG-1: FE Tailwind 설정 누락 (수정 완료)
- **증상:** FE 기동 시 500 에러 — `bg-disabled/40` 클래스 미존재
- **원인:** `globals.css`에서 사용하는 `disabled`, `border-line`, `border-hover` 커스텀 색상이 `tailwind.config.ts`에 정의되지 않음
- **수정:** `tailwind.config.ts`에 누락된 색상 3개 추가
- **파일:** `worktrees/feat-fe-shell/wiki/frontend/tailwind.config.ts`

### BUG-2: Document/Search API 미구현
- **증상:** 문서 CRUD, 발행, AI 상태, 검색, 휴지통 관련 API 모두 403 반환
- **원인:** 현재 Docker 컨테이너(`wiki-api`)에 배포된 코드에 Document/Search Controller가 존재하지 않음. `UserApiController`(Auth 관련)만 구현되어 있음
- **영향:** 핵심 기능인 문서 관리가 전혀 동작하지 않음

### BUG-3: API 엔드포인트 네이밍 불일치
- **증상:** 요구사항 문서의 엔드포인트(`/sign-up`, `/sign-in`, `/sign-out`)와 실제 구현(`/signup`, `/login`, `/logout`)이 다름
- **영향:** FE에서 API 호출 시 404/403 발생 가능

### BUG-4: BE 포트 불일치
- **증상:** 요구사항에서는 포트 8080이나 실제 Docker 컨테이너는 8081로 매핑
- **영향:** FE의 API base URL 설정 불일치 가능

## 릴리즈 판정 (1차)
**❌ NOT READY**

### 판정 이유:
1. **핵심 기능 미구현:** 문서 생성/조회/수정/삭제, 검색, AI 상태 등 제품의 핵심 기능인 Document API가 전혀 구현되지 않았음 (12개 테스트 중 8개 실패)
2. **Auth API만 동작:** 회원가입, 로그인, 토큰 갱신, 로그아웃만 정상 (4/12 통과)
3. **FE Tailwind 설정 버그:** 수정은 완료했으나 원본 코드에 머지되지 않은 상태
4. **엔드포인트 네이밍 불일치:** 요구사항 문서와 실제 구현 간 API 경로가 다름

---

## 재테스트 — NAW-108 포트/보안 수정 검증
**Date:** 2026-03-15
**Trigger:** NAW-108 버그 수정 (PR #40 머지 후 재배포)

### 인프라 상태
| 서비스 | 상태 | 비고 |
|--------|------|------|
| MySQL (3306) | ✅ | Docker `wiki-mysql` 정상 |
| Redis (6379) | ✅ | Docker `wiki-redis` 정상 |
| Kafka (9094) | ✅ | Docker `wiki-kafka` 정상 |
| OTel Collector (4317-4318) | ✅ | Docker `wiki-otel-collector` 정상 |
| Tempo (3200) | ✅ | Docker `wiki-tempo` 정상 |
| BE API (**8080**) | ✅ | 포트 8081→**8080 변경 확인** ✅ |
| FE (3000) | ✅ | 이전 Tailwind 수정 유지, 정상 |

### NAW-108 수정 검증 결과
| # | 시나리오 | 기대 | 결과 | 비고 |
|---|---------|------|------|------|
| 1 | Health Check (`GET /actuator/health`) | 200 | ✅ 200 | DB, Redis, Disk 모두 UP |
| 2 | 회원가입 (`POST /api/v1/auth/signup`) | 200 | ✅ 200 | `{"id":4,"email":"qa2@test.com"}` |
| 3 | 로그인 (`POST /api/v1/auth/login`) | 200 | ✅ 200 | accessToken + refreshToken 정상 |
| 4 | Token Refresh (`POST /api/v1/auth/refresh`) | 200 | ✅ 200 | 새 accessToken 발급 |
| 5 | 로그아웃 (`POST /api/v1/auth/logout`) | 204 | ✅ 204 | Bearer 토큰으로 정상 처리 |
| 6 | 미인증 요청 (`GET /api/v1/documents`, no token) | **401** | ✅ **401** | 기존 403→401 수정 확인 ✅ |
| 7 | 인증 + 미존재 엔드포인트 (`GET /api/v1/nonexistent`) | **404** | ✅ **404** | 기존 403→404 수정 확인 ✅ |

### Document/Search API 상태 (미배포)
| # | 시나리오 | 결과 | 비고 |
|---|---------|------|------|
| 8 | 문서 생성 (`POST /api/v1/documents`) | ❌ 404 | Controller 미배포 (기존 403→404 개선) |
| 9 | 문서 목록 (`GET /api/v1/documents`) | ❌ 404 | Controller 미배포 |
| 10 | 검색 (`GET /api/v1/search/integrated`) | ❌ 404 | Controller 미배포 |
| 11 | Trash 조회 (`GET /api/v1/documents/trash`) | ❌ 404 | Controller 미배포 |

### BUG-4 해소 확인
- **포트 불일치:** ✅ 해소 — `wiki-api`가 이제 `0.0.0.0:8080->8080/tcp`로 매핑됨

### NAW-108 재테스트 판정
**✅ NAW-108 수정사항 전부 정상 확인**
- 포트 8080 변경 ✅
- 미인증 401 반환 ✅
- 인증+미존재 404 반환 ✅
- Auth API 전체 정상 ✅

### 전체 릴리즈 판정 (2차)
**❌ NOT READY**

**사유:** Document/Search API Controller가 아직 main에 미배포 상태. Auth + 보안 응답 코드는 정상이나, 핵심 기능인 문서 CRUD/발행/검색/Trash가 동작하지 않음. Document API PR 머지 + 재배포 후 3차 재테스트 필요.
