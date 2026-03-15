# Stage 09: 리스크 및 복잡도 스코어링

> 08_ticket_breakdown.md 기준 | 작성일: 2026-03-14

---

## 1. 스코어링 기준

| 점수 | 리스크 | 복잡도 |
|------|--------|--------|
| 1 | 실패 가능성 매우 낮음, 영향 범위 제한적 | 단순 CRUD, 인터페이스 정의 |
| 2 | 기존 패턴 활용, 예상 가능한 이슈 | 도메인 로직 포함, 테스트 필요 |
| 3 | 외부 연동 또는 동시성 관련, 디버깅 필요 | 여러 모듈 연동, 상태 관리 |
| 4 | 비동기/이벤트 기반, 장애 시 영향 큼 | 복잡한 흐름, 에러 핸들링 다수 |
| 5 | 미확정 요구사항, 아키텍처 변경 가능 | 신규 기술 도입, 대규모 통합 |

---

## 2. 전체 티켓 스코어 보드

### BE Lane

| 티켓 ID | 제목 | Risk | Complexity | Capacity | Day Bucket | 의존성 |
|---------|------|------|------------|----------|------------|--------|
| NAW-BE-001 | Document 도메인 POJO + 상태 머신 | 2 | 3 | 2 | single-day | - |
| NAW-BE-002 | Outbound Port 인터페이스 정의 | 1 | 1 | 1 | single-day | BE-001 |
| NAW-BE-003 | JPA Entity + Repository + Mapper | 3 | 3 | 3 | single-day | BE-001, BE-002 |
| NAW-BE-004 | 문서 CRUD UseCase | 2 | 3 | 3 | single-day | BE-003 |
| NAW-BE-005 | AI 파이프라인 비동기 실행 | **4** | **4** | 4 | **multi-day** | BE-004, BE-006 |
| NAW-BE-006 | Mock AI Adapter | 1 | 1 | 1 | single-day | BE-002 |
| NAW-BE-007 | SSE 엔드포인트 | 3 | 3 | 2 | single-day | BE-005 |
| NAW-BE-008 | REST Controller + Security | 3 | 3 | 3 | single-day | BE-004, BE-005, BE-007 |
| NAW-BE-009 | 문서 검색 기능 | 3 | 3 | 3 | single-day | BE-003 |
| NAW-BE-010 | Revision 자동 생성 | 2 | 2 | 2 | single-day | BE-004 |

### FE Lane

| 티켓 ID | 제목 | Risk | Complexity | Capacity | Day Bucket | 의존성 |
|---------|------|------|------------|----------|------------|--------|
| NAW-FE-001 | API Client + 타입 정의 | 1 | 2 | 2 | single-day | - |
| NAW-FE-002 | 문서 목록/상세 페이지 | 2 | 2 | 2 | single-day | FE-001 |
| NAW-FE-003 | 문서 생성/수정 폼 | 2 | 3 | 3 | single-day | FE-001 |
| NAW-FE-004 | 상태 전환 + AI 분석 + SSE | **3** | **4** | 3 | single-day | FE-002 |
| NAW-FE-005 | 검색 UI + 태그 필터 | 2 | 2 | 2 | single-day | FE-001 |

### DevOps Lane

| 티켓 ID | 제목 | Risk | Complexity | Capacity | Day Bucket | 의존성 |
|---------|------|------|------------|----------|------------|--------|
| NAW-DO-001 | 개발 환경 Docker Compose | 1 | 2 | 2 | single-day | - |
| NAW-DO-002 | Pinpoint 모니터링 환경 | 2 | 2 | 2 | single-day | - |
| NAW-DO-003 | Dockerfile + Pinpoint Agent | 3 | 3 | 2 | single-day | DO-001, DO-002 |
| NAW-DO-004 | CI/CD 파이프라인 | 2 | 2 | 2 | single-day | DO-001 |

### Cross-Lane

| 티켓 ID | 제목 | Risk | Complexity | Capacity | Day Bucket | 의존성 |
|---------|------|------|------------|----------|------------|--------|
| NAW-CL-001 | Pinpoint 추적 + 운영 체크리스트 | 2 | 2 | 2 | single-day | BE-008, DO-003 |

---

## 3. 리스크 상위 티켓 (Risk ≥ 3)

| 순위 | 티켓 | Risk | 주요 리스크 | 완화 전략 |
|------|------|------|-----------|----------|
| 1 | NAW-BE-005 | 4 | 비동기 실행 + 이벤트 기반 + 상태 전이 복합 | @Async 단순 구현 우선, MQ 후속. Mock Adapter로 흐름 검증 |
| 2 | NAW-BE-003 | 3 | Entity↔Domain 매핑 오류, @Version 동작 검증 | 통합 테스트 필수, H2로 자동 검증 |
| 3 | NAW-BE-007 | 3 | SSE 연결 관리, 메모리 누수, 타임아웃 | 연결 수 제한, 주기적 정리, 타임아웃 설정 |
| 4 | NAW-BE-008 | 3 | 인증/인가 전체 통합, 에러 핸들링 | SecurityConfig 단위 테스트, 통합 테스트 |
| 5 | NAW-BE-009 | 3 | Full-text search 한국어, 성능 | pg_bigm 확장 검토, 인덱스 추가 |
| 6 | NAW-DO-003 | 3 | Agent 버전 호환, JVM 옵션 충돌 | 로컬 검증 후 배포 |
| 7 | NAW-FE-004 | 3 | SSE 연결 안정성, 재연결 로직 | EventSource 재연결 + 폴링 fallback |

---

## 4. 권장 구현 순서

### Lane별 하루 Capacity: BE 4, FE 4, DevOps 3

#### Day 1 — 기반 구조

| Lane | 티켓 | Capacity |
|------|------|----------|
| BE | NAW-BE-001 (도메인 POJO, 2) + NAW-BE-002 (Port, 1) | 3/4 |
| FE | NAW-FE-001 (API Client, 2) | 2/4 |
| DevOps | NAW-DO-001 (Docker Compose, 2) + NAW-DO-002 (Pinpoint, 2) | 4/3 ⚠️ |

> DevOps Day 1은 capacity 초과이나, DO-001과 DO-002는 독립 작업이므로 병렬 가능

#### Day 2 — 영속성 + UI 기본

| Lane | 티켓 | Capacity |
|------|------|----------|
| BE | NAW-BE-003 (JPA, 3) + NAW-BE-006 (Mock Adapter, 1) | 4/4 |
| FE | NAW-FE-002 (목록/상세, 2) + NAW-FE-003 (생성/수정, 3) | 5/4 ⚠️ |
| DevOps | NAW-DO-003 (Dockerfile+Agent, 2) | 2/3 |

> FE Day 2는 FE-003을 Day 3으로 이월 가능

#### Day 3 — UseCase + 검색

| Lane | 티켓 | Capacity |
|------|------|----------|
| BE | NAW-BE-004 (CRUD UseCase, 3) | 3/4 |
| FE | NAW-FE-003 (생성/수정, 3, 이월 분) | 3/4 |
| DevOps | NAW-DO-004 (CI/CD, 2) | 2/3 |

#### Day 4 — AI 파이프라인 (핵심)

| Lane | 티켓 | Capacity |
|------|------|----------|
| BE | NAW-BE-005 (AI 파이프라인, 4) | 4/4 |
| FE | NAW-FE-005 (검색 UI, 2) | 2/4 |
| DevOps | buffer |

#### Day 5 — SSE + 통합

| Lane | 티켓 | Capacity |
|------|------|----------|
| BE | NAW-BE-007 (SSE, 2) + NAW-BE-010 (Revision, 2) | 4/4 |
| FE | NAW-FE-004 (상태전환+SSE, 3) | 3/4 |
| DevOps | buffer |

#### Day 6 — Controller + 검색 + Cross-lane

| Lane | 티켓 | Capacity |
|------|------|----------|
| BE | NAW-BE-008 (Controller, 3) + NAW-BE-009 (검색, 3) | 6/4 ⚠️ |
| DevOps | NAW-CL-001 (운영 체크리스트, 2) | 2/3 |

> BE Day 6은 capacity 초과, BE-009를 Day 7로 이월 가능

#### Day 7 — 마무리 + 통합 테스트

| Lane | 티켓 | Capacity |
|------|------|----------|
| BE | NAW-BE-009 (이월) + 통합 테스트 | 3/4 |
| FE | 통합 테스트 + 버그 수정 | buffer |
| DevOps | 통합 환경 검증 | buffer |

---

## 5. 의존성 그래프

```
BE Lane:
BE-001 ──→ BE-002 ──→ BE-006
  │            │
  │            └──→ BE-003 ──→ BE-004 ──→ BE-005 ──→ BE-007 ──→ BE-008
  │                   │           │
  │                   └──→ BE-009 └──→ BE-010
  │
  └──→ (BE-002)

FE Lane:
FE-001 ──→ FE-002 ──→ FE-004
  │
  ├──→ FE-003
  └──→ FE-005

DevOps Lane:
DO-001 ──→ DO-003 ──→ CL-001
  │
  └──→ DO-004
DO-002 ──→ DO-003

Cross-Lane:
BE-008 ──→ CL-001
DO-003 ──→ CL-001
```
