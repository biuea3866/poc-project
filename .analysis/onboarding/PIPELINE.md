# 온보딩 (도메인 설명) 파이프라인

> 새 팀원 또는 다른 팀 개발자에게 특정 도메인/기능을 설명할 때 사용

---

## 파이프라인 흐름

```
[INPUT] "이 기능/도메인 설명해줘", "이 서비스 구조가 어떻게 돼?"
    │
    ▼
━━ Phase 1: 질문 범위 파악 ━━━━━━━━━━━━━━━━━━━━━━━━
    │  팀장이 질문 분석
    │  - 대상 도메인/기능 식별
    │  - 설명 깊이 결정 (개요 vs 상세)
    │  - 질문자 맥락 파악 (FE? BE? 신규?)
    │
    ▼
━━ Phase 2: 정보 수집 (병렬) ━━━━━━━━━━━━━━━━━━━━━━
    │  ┌─ Agent 📖: 구조 및 흐름 분석
    │  └─ Agent 🔗: 연결 지점 분석
    │
    ▼
━━ Phase 3: 설명 문서 작성 ━━━━━━━━━━━━━━━━━━━━━━━━
    │  - 전체 그림 (아키텍처 다이어그램)
    │  - 핵심 개념 설명
    │  - 코드 진입점 안내
    │  - 주의사항 및 함정
    │
    ▼
[OUTPUT] 도메인 설명 문서
```

---

## 설명 깊이 단계

### Level 1: 개요 (5분 이해)
```
- 이 서비스/기능이 뭘 하는지
- 어떤 사용자가 쓰는지
- 전체 시스템에서 어디에 위치하는지
```

### Level 2: 구조 (30분 이해)
```
- 모듈 구조 및 계층
- 주요 도메인 모델
- API 엔드포인트 목록
- 데이터 흐름
```

### Level 3: 상세 (깊은 이해)
```
- 핵심 비즈니스 로직 코드 레벨 설명
- 상태 머신 / 분기 조건
- 에러 처리 / 엣지 케이스
- 다른 서비스와의 통신 상세
- 주의해야 할 함정
```

---

## 에이전트 역할 정의

### Agent 📖: 구조 및 흐름 분석가
**목표**: 대상 도메인의 구조와 데이터 흐름을 명확히 파악

**분석 항목**:
1. 레포/모듈 구조 (디렉토리 트리)
2. 핵심 파일 식별 (진입점, 도메인 모델, 서비스)
3. 데이터 흐름 추적 (입력 → 처리 → 출력)
4. 상태 전환 로직 (있는 경우)
5. 주요 설정 파일

### Agent 🔗: 연결 지점 분석가
**목표**: 이 도메인이 다른 도메인/서비스와 어떻게 연결되는지 파악

**분석 항목**:
1. 이 서비스를 호출하는 서비스 (upstream)
2. 이 서비스가 호출하는 서비스 (downstream)
3. Kafka 이벤트 발행/구독
4. 공유 DB 테이블
5. FE 어디서 이 API를 사용하는지

---

## 에이전트 할당 규칙

| 질문 유형 | 할당 에이전트 |
|----------|-------------|
| "이 서비스 뭐야?" (Level 1) | 에이전트 없이 팀장만 (보고서 참조) |
| "이 서비스 구조 설명해줘" (Level 2) | 📖만 |
| "이 기능 완전히 이해하고 싶어" (Level 3) | 📖 + 🔗 |
| "이거 수정하려면 뭘 알아야 해?" | 📖 + 🔗 |

---

## 도메인별 빠른 진입점 가이드

### 지원자/지원서
```
코드: greeting-new-back/domain/candidate/
API:  /service/new-back/api/v1.0/apply, /applicants
상태: CREATE → SUBMIT → PASS/REJECT
FE:   greeting_front → /workspace/:id/opening/:id/applicants
```

### 채용공고
```
코드: opening/opening/business/domain/
API:  /service/ats/openings
상태: CLOSE → OPEN → CLOSE → ARCHIVE
FE:   greeting_front → /workspace/:id/openings
```

### 평가
```
코드: greeting-ats/greeting-evaluation/
API:  /service/evaluation/
FE:   greeting_front → 공고 상세 내 평가 탭
```

### 면접
```
코드: greeting-communication/communication/
API:  /service/offer/interview
FE:   greeting_interview-next
외부: Google Calendar, Zoom
```

### 커뮤니케이션 (메일/SMS/알림톡)
```
오케스트레이션: greeting-communication
실제 발송:     doodlin-communication
Kafka:        queue.ats.task.* → queue.ats.mail.send.* → queue.doodlin.mail.send.*
```

### 인증/인가
```
인증: greeting_authn-server (JWT, SAML, OIDC, 2FA)
인가: greeting_authz-server (RBAC, 역할/권한)
게이트웨이: greeting-api-gateway (필터 체인)
```

### 결제/구독
```
코드: greeting_payment-server
API:  /payment/plan/*
외부: Toss Payments
모델: PlanOnWorkspace (Basic/Standard)
```

### TRM (인재 관리)
```
FE:   greeting_trm_front
BE:   greeting_trm-server (별도 분석 보고서 참조)
확장: greeting_trm-extension (Chrome)
```

---


---

## 출력 어조

산출물은 **팀원과 공유하는 문서**입니다. 읽는 사람이 지치지 않도록 아래 원칙을 따릅니다.

- **핵심부터** — 결론·액션 아이템을 앞에 씁니다. 이유와 배경은 뒤에 써도 됩니다.
- **짧게** — 한 문장으로 쓸 수 있으면 세 문장으로 쓰지 않습니다.
- **구체적으로** — "여러 곳" 대신 "3곳", "느릴 수 있음" 대신 "users 테이블 full scan" 처럼 씁니다.
- **표·불릿 우선** — 비교·목록은 문장보다 표나 불릿으로 씁니다.
- **중립적으로** — 문제를 발견해도 단정짓지 않고 확인 사항으로 전달합니다.

## 출력 형식

분석 결과는 `.analysis/onboarding/results/` 디렉토리에 저장
