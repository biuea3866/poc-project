# API 스펙 변경 분석 파이프라인

> BE API 변경 시 FE + 다른 BE 서비스 영향 추적

---

## 파이프라인 흐름

```
[INPUT] API 변경 내용 (엔드포인트, 요청/응답 변경)
    │
    ▼
━━ Phase 1: 변경 분류 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    │  팀장이 API 변경 유형 분류
    │  - 신규 API 추가
    │  - 기존 API 수정 (요청/응답 변경)
    │  - API 삭제/deprecation
    │  - 내부 API vs 외부 API
    │
    ▼
━━ Phase 2: 소비자 추적 (병렬) ━━━━━━━━━━━━━━━━━━━━
    │  ┌─ Agent 🌐: FE 소비자 추적
    │  └─ Agent 🔌: BE 소비자 추적
    │
    ▼
━━ Phase 3: 호환성 분석 ━━━━━━━━━━━━━━━━━━━━━━━━━━━
    │  - 하위 호환 여부 판단
    │  - 필수 동시 수정 목록
    │  - 선택적 수정 목록
    │  - 배포 순서 결정
    │
    ▼
[OUTPUT] API 변경 영향 보고서
```

---

## API 변경 유형별 위험도

| 변경 유형 | 하위 호환 | 위험도 | 대응 |
|----------|----------|--------|------|
| 새 엔드포인트 추가 | 호환 | 낮음 | FE에 알림만 |
| 응답 필드 추가 | 호환 | 낮음 | FE에 알림만 |
| 요청 선택 파라미터 추가 | 호환 | 낮음 | FE에 알림만 |
| 요청 필수 파라미터 추가 | **비호환** | **높음** | FE 동시 수정 필수 |
| 응답 필드 삭제/이름변경 | **비호환** | **높음** | FE 동시 수정 필수 |
| 응답 타입 변경 | **비호환** | **높음** | FE 동시 수정 필수 |
| URL 경로 변경 | **비호환** | **높음** | Gateway + FE 동시 수정 |
| HTTP 메서드 변경 | **비호환** | **높음** | FE 동시 수정 필수 |
| 엔드포인트 삭제 | **비호환** | **Critical** | 모든 소비자 확인 |
| 에러 코드 변경 | 부분 호환 | 중간 | FE 에러 핸들링 확인 |

---

## 에이전트 역할 정의

### Agent 🌐: FE 소비자 추적가
**목표**: 변경된 API를 호출하는 모든 FE 코드 추적

**탐색 전략**:
```
1. URL 패턴으로 FE 레포 전체 검색
   Grep: "/service/ats/api/v1.0/..." 또는 "/applicants" 등

2. API 함수 정의 파일 찾기
   - greeting_front/src/api/
   - next-greeting/packages/api/
   - greeting_career-next/src/api/

3. 해당 API 함수를 호출하는 컴포넌트/훅 추적
   Grep: import된 API 함수명

4. React Query 키/옵션 확인
   - 캐시 무효화 영향
   - 에러 핸들링 영향
```

**레포별 API 코드 위치**:
```
greeting_front:            src/api/{domain}/apis.ts
next-greeting:             packages/api/src/ (Orval 자동생성)
greeting_career-next:      src/api/, src/hooks/
greeting_forms-next:       src/api/
greeting_front_mobile-next: src/api/
greeting_trm_front:        src/api/
greeting_analytics:        src/api/
```

### Agent 🔌: BE 소비자 추적가
**목표**: 변경된 API를 호출하는 다른 BE 서비스 추적

**탐색 전략**:
```
1. Feign/Retrofit 클라이언트에서 URL 패턴 검색
   Grep: @GetMapping, @PostMapping + URL

2. Gateway 라우팅 설정 확인
   greeting-api-gateway/routes/*.yaml

3. 내부 API (/internal/*) 호출 추적
   Grep: "/internal/" + 엔드포인트

4. Aggregator에서의 호출 확인
   greeting-aggregator/adaptor/
```

**레포별 클라이언트 위치**:
```
greeting-aggregator:      adaptor/{service}/client/
greeting-api-gateway:     routes/*.yaml
greeting-communication:   adaptor/retrofit/
greeting-integration:     adaptor/retrofit/client/
greeting-workspace-server: dependency/openfeign/
greeting_authz-server:    adaptor/
```

---

## 에이전트 할당 규칙

| API 변경 유형 | 할당 에이전트 |
|-------------|-------------|
| 새 API 추가 (내부) | 에이전트 없이 팀장만 |
| 새 API 추가 (외부) | 🌐만 (FE에 안내) |
| 기존 API 수정 (호환) | 🌐만 (FE 확인) |
| 기존 API 수정 (비호환) | 🌐 + 🔌 (전체 추적) |
| API 삭제 | 🌐 + 🔌 (전체 추적 필수) |
| 내부 API 변경 | 🔌만 (BE 간 호출) |
| Gateway 라우팅 변경 | 🌐 + 🔌 |

---

## Gateway 라우팅 매핑 (참조)

```
/service/ats/**         → greeting-ats          (${ATS_HOST})
/service/wno/**         → greeting-ats/wno      (${WNO_HOST})
/service/offer/**       → greeting-communication(${OFFER_HOST})
/service/evaluation/**  → greeting-ats/eval
/service/new-back/**    → greeting-new-back     (${NEWBACK_HOST})
/service/integration/** → greeting-integration
/app/**                 → greeting-aggregator
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

분석 결과는 `.analysis/api-change/results/` 디렉토리에 저장
