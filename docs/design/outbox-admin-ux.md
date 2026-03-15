# Outbox Admin UX Spec

> 어드민 아웃박스 관리 페이지 — Kafka 미발행 이벤트 모니터링 및 수동 재처리 UI

---

## 1. 개요

아웃박스 패턴(Transactional Outbox)은 Kafka 발행 실패 시 `outbox` 테이블에 미발행 이벤트를 적재하고, 이후 스케줄러 또는 어드민 수동 재처리를 통해 신뢰성을 보장하는 패턴이다.

이 페이지는 **운영자가 아웃박스 이벤트 상태를 모니터링하고 실패한 이벤트를 수동으로 재처리**할 수 있는 어드민 화면을 정의한다.

---

## 2. 페이지 레이아웃

### 전체 구조

```
┌──────────────────────────────────────────────────────────────────────┐
│  Outbox 관리                                                         │
│  Kafka 미발행 이벤트를 모니터링하고 재처리합니다.                         │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │  상태 요약 카드                                                  │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │ │
│  │  │ 전체     │  │ PENDING  │  │ SUCCESS  │  │ FAILED   │       │ │
│  │  │   142    │  │    3     │  │   135    │  │    4     │       │ │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │  필터/액션 바                                                    │ │
│  │  [상태 ▼]  [날짜 범위 ▼]  [Topic ▼]         [FAILED 전체 재처리]│ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │  이벤트 테이블                                                   │ │
│  │  ┌────────┬────────────┬──────────┬───────┬────────────┬─────┐ │ │
│  │  │Event ID│ Topic      │ Status   │ Retry │ Created At │ Act │ │ │
│  │  ├────────┼────────────┼──────────┼───────┼────────────┼─────┤ │ │
│  │  │ 1042   │ event.doc  │ PENDING  │  0    │ 03-15 14:  │     │ │ │
│  │  │ 1041   │ queue.ai.  │ SUCCESS  │  1    │ 03-15 13:  │     │ │ │
│  │  │ 1040   │ event.doc  │ FAILED   │  3    │ 03-15 12:  │[재] │ │ │
│  │  └────────┴────────────┴──────────┴───────┴────────────┴─────┘ │ │
│  │                                                                 │ │
│  │                  ← 이전  1  [2]  3  다음 →                      │ │
│  └─────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 3. 상태 요약 카드

### 레이아웃

```
┌─────────────────┐
│  라벨 (Caption)  │  ← text-xs font-bold uppercase tracking-wider text-muted
│  숫자 (Display)  │  ← text-2xl font-extrabold text-primary
│  [●] 상태 색상   │  ← 하단 4px 색상 바
└─────────────────┘
```

### 카드별 스타일

| 카드 | 라벨 | 하단 바 색상 | 숫자 색상 |
|---|---|---|---|
| 전체 | `전체 이벤트` | `bg-accent` | `text-primary` |
| PENDING | `대기 중` | `bg-warning` | `text-warning` |
| SUCCESS | `성공` | `bg-success` | `text-success` |
| FAILED | `실패` | `bg-danger` | `text-danger` |

### 컨테이너 스타일

```css
grid grid-cols-2 md:grid-cols-4 gap-4
```

각 카드:
```css
card p-5 text-center
/* 하단 바: absolute bottom-0 left-0 right-0 h-1 rounded-b-xl */
```

---

## 4. 필터/액션 바

### 필터 컴포넌트

| 필터 | 타입 | 옵션 | 기본값 |
|---|---|---|---|
| **상태** | Select dropdown | ALL, PENDING, SUCCESS, FAILED | ALL |
| **날짜 범위** | Date range picker | 시작일 ~ 종료일 | 최근 7일 |
| **Topic** | Select dropdown | ALL, event.document, queue.ai.tagging, queue.ai.embedding, ... | ALL |

#### Select 스타일

```css
rounded-lg border border-border-line bg-surface px-3 py-2 text-sm text-secondary
focus:border-accent focus:ring-2 focus:ring-accent/20
```

### Bulk 재처리 버튼

```
[FAILED 전체 재처리]
```

- 스타일: `btn-accent` (단, FAILED 건이 0이면 `disabled`)
- 클릭 시: 확인 모달 표시

```
┌────────────────────────────────────────┐
│  FAILED 이벤트 전체 재처리              │
│                                        │
│  실패한 4건의 이벤트를 모두             │
│  재처리하시겠습니까?                     │
│                                        │
│          [취소]     [전체 재처리]       │
└────────────────────────────────────────┘
```

- API: 각 FAILED 이벤트에 대해 `POST /api/admin/outbox/{id}/retry` 순차 호출
- 성공 시: `toast.success("4건의 이벤트가 재처리 요청되었습니다.")`

---

## 5. 이벤트 테이블

### 컬럼 정의

| 컬럼 | 필드 | 너비 | 정렬 | 스타일 |
|---|---|---|---|---|
| Event ID | `id` | `w-20` | 좌 | `text-sm font-mono text-muted` |
| Topic | `topic` | `flex-1` | 좌 | `text-sm font-mono text-secondary` |
| Status | `status` | `w-28` | 중앙 | Badge 컴포넌트 |
| Retry | `retry_count` | `w-16` | 중앙 | `text-sm text-secondary` |
| Created At | `created_at` | `w-36` | 좌 | `text-sm text-muted` |
| Action | — | `w-20` | 중앙 | 재처리 버튼 |

### 테이블 스타일

```css
/* 테이블 컨테이너 */
rounded-xl border border-border-line overflow-hidden

/* 헤더 행 */
bg-surface text-xs font-bold uppercase tracking-wider text-muted

/* 데이터 행 */
border-t border-border-line bg-card hover:bg-surface/50 transition

/* 행 높이 */
py-3 px-4
```

### Status Badge 매핑

| Status | Badge Variant | 텍스트 | 예시 |
|---|---|---|---|
| `PENDING` | `warning` | 대기 중 | `Badge variant="warning" dot` → 노란 점 + "대기 중" |
| `SUCCESS` | `success` | 성공 | `Badge variant="success" dot` → 초록 점 + "성공" |
| `FAILED` | `danger` | 실패 | `Badge variant="danger" dot` → 빨간 점 + "실패" |

### 재처리 버튼

- **조건:** `status === "FAILED"` 일 때만 노출
- **스타일:** `text-xs font-semibold text-accent hover:text-accent/80 transition`
- **텍스트:** `재처리`
- **클릭 시:**
  1. 버튼 → 로딩 스피너 (작은 원형)
  2. `POST /api/admin/outbox/{id}/retry` 호출
  3. 성공: `toast.success("이벤트가 재처리 요청되었습니다.")` + 테이블 갱신
  4. 실패: `toast.error("재처리에 실패했습니다.")`

### 정렬

| 컬럼 | 클릭 동작 |
|---|---|
| Created At | 기본 내림차순 (최신 순), 클릭 시 토글 |
| Status | 클릭 시 FAILED → PENDING → SUCCESS 순 정렬 |
| Retry | 클릭 시 재시도 횟수 내림차순 |

정렬 아이콘: `ChevronUp` / `ChevronDown` — 활성 컬럼만 아이콘 표시

---

## 6. 빈 상태

### 전체 이벤트 없음

```
┌──────────────────────────────────┐
│                                  │
│       [Inbox icon 48px           │
│        opacity-10]               │
│                                  │
│   아웃박스 이벤트가 없습니다.      │
│   Kafka 발행이 모두 정상 처리     │
│   되었습니다.                     │
│                                  │
└──────────────────────────────────┘
```

### 필터 결과 없음

```
┌──────────────────────────────────┐
│                                  │
│       [Filter icon 48px          │
│        opacity-10]               │
│                                  │
│   선택한 필터에 해당하는          │
│   이벤트가 없습니다.              │
│                                  │
│         [필터 초기화]             │
│                                  │
└──────────────────────────────────┘
```

- "필터 초기화" 버튼: `btn-ghost border border-border-line`
- 클릭 시 모든 필터를 기본값으로 리셋

---

## 7. API Contract

### 목록 조회: `GET /api/admin/outbox`

#### Request

| Parameter | Type | Required | 설명 |
|---|---|---|---|
| `status` | string | No | PENDING, SUCCESS, FAILED (전체면 생략) |
| `topic` | string | No | 토픽 필터 |
| `startDate` | string | No | 시작일 (ISO 8601) |
| `endDate` | string | No | 종료일 (ISO 8601) |
| `page` | number | No | 페이지 (default: 0) |
| `size` | number | No | 크기 (default: 20) |
| `sort` | string | No | `createdAt,desc` (기본) |

#### Response

```json
{
  "content": [
    {
      "id": 1042,
      "topic": "event.document",
      "payload": "{\"documentId\":42,\"action\":\"PUBLISH\"}",
      "status": "FAILED",
      "retryCount": 3,
      "createdAt": "2024-03-15T14:30:00",
      "processedAt": null
    }
  ],
  "totalElements": 142,
  "totalPages": 8,
  "number": 0,
  "size": 20,
  "summary": {
    "total": 142,
    "pending": 3,
    "success": 135,
    "failed": 4
  }
}
```

### 수동 재처리: `POST /api/admin/outbox/{id}/retry`

#### Response (성공)

```json
{
  "id": 1040,
  "status": "PENDING",
  "retryCount": 4,
  "message": "이벤트가 재처리 큐에 등록되었습니다."
}
```

#### Response (실패 — 이미 처리됨)

```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "이미 SUCCESS 상태의 이벤트는 재처리할 수 없습니다."
}
```

---

## 8. 이벤트 상세 확인 (Row Expand)

테이블 행 클릭 시 해당 행 아래에 상세 패널이 확장됩니다.

### 확장 패널 레이아웃

```
┌──────────────────────────────────────────────────────────┐
│  Event #1040                                             │
│──────────────────────────────────────────────────────────│
│  Topic:     event.document                               │
│  Status:    [FAILED]                                     │
│  Retry:     3 / 3                                        │
│  Created:   2024-03-15 12:00:00                          │
│  Processed: —                                            │
│                                                          │
│  Payload:                                                │
│  ┌────────────────────────────────────────────────────┐  │
│  │ {                                                  │  │
│  │   "documentId": 42,                               │  │  ← bg-surface
│  │   "action": "PUBLISH",                            │  │     font-mono text-sm
│  │   "timestamp": "2024-03-15T12:00:00"              │  │     rounded-lg p-4
│  │ }                                                  │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│                                          [재처리]        │
└──────────────────────────────────────────────────────────┘
```

- 확장 애니메이션: `max-height` transition, `animate-in`
- Payload: JSON pretty-print, syntax highlighting (mono font)
- 배경: `bg-surface/50 border-t border-border-line`

---

## 9. 반응형

| Breakpoint | 변경 사항 |
|---|---|
| `lg+` (1024px+) | 기본 레이아웃 |
| `md` (768px) | 상태 카드 2×2 그리드 |
| `sm` 미만 | 테이블 → 카드 리스트 전환, 필터 접기 |

### 모바일 카드 레이아웃 (sm 미만)

```
┌────────────────────────────────┐
│  #1040  ·  event.document      │
│                                │
│  [FAILED]   Retry: 3           │
│  2024-03-15 12:00              │
│                                │
│                    [재처리]     │
└────────────────────────────────┘
```
