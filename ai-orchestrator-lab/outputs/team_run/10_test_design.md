# Stage 10: 테스트 설계 (Test Design)

> PRD v2.1 기준 | 작성일: 2026-03-14 | 담당: fe-developer

## 테스트 전략 개요

### 테스트 피라미드

| 레벨 | 범위 | 도구 | 비율 |
|------|------|------|------|
| Unit | 유틸 함수, 커스텀 훅 | Vitest + React Testing Library | 60% |
| Integration | 페이지 컴포넌트 + API 연동 | Vitest + MSW (Mock Service Worker) | 30% |
| E2E | 핵심 사용자 시나리오 | Playwright | 10% |

---

## 1. 문서 생성 (POST /api/v1/documents)

### TC-01: 정상적인 문서 생성

```
Given: 사용자가 /documents/new 페이지에 접속
When: 제목 "테스트 문서", 본문 "본문 내용"을 입력하고 저장 버튼 클릭
Then:
  - createDocument API가 { title: "테스트 문서", content: "본문 내용" }으로 호출됨
  - 성공 시 /documents/{id} 페이지로 리다이렉트
  - 저장 버튼이 "저장 중..." 상태로 변경됨
```

### TC-02: 빈 필드 제출 방지

```
Given: 사용자가 /documents/new 페이지에 접속
When: 제목 또는 본문을 비운 채 저장 시도
Then: HTML required 속성에 의해 제출이 차단됨
```

### TC-03: API 오류 시 에러 표시

```
Given: 서버가 500 에러를 반환하는 상황
When: 사용자가 문서를 저장 시도
Then:
  - 에러 메시지가 화면에 표시됨
  - 폼 데이터가 유지됨 (입력 손실 없음)
  - 저장 버튼이 다시 활성화됨
```

---

## 2. 문서 목록 조회 (GET /api/v1/documents)

### TC-04: 홈페이지 문서 목록 로딩

```
Given: 사용자가 / (홈페이지)에 접속
When: 페이지가 마운트됨
Then:
  - "문서 목록을 불러오는 중..." 로딩 메시지가 표시됨
  - searchDocuments() API 호출
  - 성공 시 DocumentStatusBoard에 문서 카드 렌더링
```

### TC-05: 문서 목록 로딩 실패

```
Given: API가 네트워크 오류를 반환
When: 홈페이지 로딩 시
Then:
  - 로딩 상태가 해제됨
  - 에러 메시지가 빨간 배경 박스에 표시됨
  - DocumentStatusBoard가 렌더링되지 않음
```

### TC-06: 빈 문서 목록

```
Given: API가 빈 배열 [] 반환
When: 홈페이지 로딩 시
Then: DocumentStatusBoard에 아무 카드도 표시되지 않음
```

---

## 3. 문서 검색 (GET /api/v1/documents?q=...)

### TC-07: 검색어 입력 시 디바운스 검색

```
Given: 사용자가 /search 페이지에 접속
When: 검색 input에 "AI Wiki" 입력
Then:
  - 300ms 디바운스 후 searchDocuments("AI Wiki") 호출
  - "검색 중..." 로딩 표시
  - 결과가 카드 리스트로 렌더링됨
```

### TC-08: 빈 검색어로 전체 목록 조회

```
Given: 사용자가 /search 페이지에 접속
When: 검색어 없이 페이지 로드
Then: searchDocuments(undefined) 호출하여 전체 문서 반환
```

### TC-09: 검색 결과 없음

```
Given: API가 빈 배열 반환
When: 검색 수행 후
Then: "검색 결과가 없습니다." 메시지 표시
```

### TC-10: 검색 API 오류

```
Given: API가 500 에러 반환
When: 검색 수행 시
Then: 에러 메시지가 빨간 배경 박스에 표시됨
```

---

## 4. 문서 상세 조회 (GET /api/v1/documents/{id})

### TC-11: 문서 상세 페이지 렌더링

```
Given: 사용자가 /documents/1에 접속
When: 페이지가 마운트됨
Then:
  - getDocument(1) API 호출
  - 문서 제목, 상태, AI 상태가 표시됨
  - SSE 연결이 수립되어 실시간 AI 상태 반영
```

### TC-12: 존재하지 않는 문서 접근

```
Given: 존재하지 않는 문서 ID (예: 9999)
When: /documents/9999 접속
Then: 에러 메시지 표시 또는 404 페이지 표시
```

---

## 5. 문서 활성화 (POST /api/v1/documents/{id}/activate)

### TC-13: DRAFT → ACTIVE 전환

```
Given: DRAFT 상태인 문서 상세 페이지
When: "ACTIVE 전환" 버튼 클릭
Then:
  - activateDocument(id) API 호출
  - 성공 시 문서 상태가 ACTIVE로 변경됨
  - 버튼이 비활성화되거나 숨김 처리
```

---

## 6. AI 분석 요청 (POST /api/v1/documents/{id}/analyze)

### TC-14: ACTIVE 문서 분석 요청

```
Given: ACTIVE 상태인 문서 상세 페이지
When: "분석 요청" 버튼 클릭
Then:
  - requestAnalysis(id) API 호출
  - AI 상태가 PENDING → PROCESSING으로 SSE를 통해 업데이트
```

### TC-15: PROCESSING 중 재요청 409 처리

```
Given: AI 상태가 PROCESSING인 문서
When: "분석 요청" 버튼 클릭
Then:
  - API가 409 Conflict 반환
  - "이미 처리 중입니다" 에러 메시지 표시
```

---

## 7. SSE 실시간 AI 상태 (useAiStatus 훅)

### TC-16: SSE 연결 및 상태 수신

```
Given: useAiStatus(documentId) 훅이 마운트됨
When: 서버가 ai-status 이벤트 전송 { "status": "PROCESSING" }
Then: 훅의 반환값이 "PROCESSING"으로 업데이트됨
```

### TC-17: SSE 연결 끊김 처리

```
Given: SSE EventSource가 연결된 상태
When: 서버 연결이 끊어짐 (onerror 발생)
Then: EventSource가 close()됨
```

### TC-18: 컴포넌트 언마운트 시 정리

```
Given: useAiStatus 훅이 활성화된 컴포넌트
When: 컴포넌트가 언마운트됨
Then: EventSource가 정리(close)됨, 메모리 누수 없음
```

---

## 8. 문서 수정 (PATCH /api/v1/documents/{id})

### TC-19: 문서 수정 성공

```
Given: ACTIVE 문서의 편집 모드
When: 제목/본문 수정 후 저장
Then:
  - updateDocument(id, title, content) API 호출
  - 성공 시 revision 생성 확인 (서버 응답 기준)
  - 수정된 내용이 화면에 반영
```

### TC-20: 낙관적 잠금 충돌 (409)

```
Given: 다른 사용자가 먼저 수정한 상황
When: 저장 시도
Then:
  - API가 409 Conflict 반환
  - "다른 사용자가 문서를 수정했습니다" 에러 표시
```

---

## 9. DocumentStatusBoard 컴포넌트

### TC-21: 문서 카드 렌더링 확인

```
Given: DocumentStatusBoard에 items 배열 전달
When: 컴포넌트 렌더링
Then:
  - 각 문서의 title, excerpt, status, aiStatus 표시
  - tags가 #태그 형태 배지로 렌더링
  - updatedAt 날짜 표시
  - status별 색상 톤 올바르게 적용
```

### TC-22: 빈 목록 처리

```
Given: items가 빈 배열 []
When: 컴포넌트 렌더링
Then: 아무 카드도 표시되지 않음 (에러 없이)
```

---

## 10. API 클라이언트 단위 테스트 (api-client.ts)

### TC-23: createDocument 성공 응답 파싱

```
Given: fetch가 201 OK, { id: 1, title: "test", ... } 반환
When: createDocument("test", "content") 호출
Then: DocumentCard 객체 반환
```

### TC-24: searchDocuments 쿼리 파라미터 설정

```
Given: query = "wiki"
When: searchDocuments("wiki") 호출
Then: fetch URL이 /api/v1/documents?q=wiki 형태
```

### TC-25: API 에러 시 Error throw

```
Given: fetch가 500 응답
When: 임의의 API 함수 호출
Then: Error("... 실패: 500") throw됨
```

---

## 검증 우선순위

| 우선순위 | 테스트 케이스 | 이유 |
|----------|-------------|------|
| P0 (필수) | TC-01, TC-04, TC-07, TC-11 | 핵심 사용자 시나리오 |
| P0 (필수) | TC-13, TC-14, TC-16 | AI 파이프라인 핵심 흐름 |
| P1 (중요) | TC-03, TC-05, TC-10, TC-15 | 에러 핸들링 |
| P1 (중요) | TC-17, TC-18, TC-23~25 | 안정성 |
| P2 (보통) | TC-02, TC-06, TC-08, TC-09, TC-19~22 | 엣지 케이스 |
