# 2026-03-14 테스트 우선 설계

## NAW-AI-202

### 테스트 케이스 1
- Given: 사용자가 새 문서를 생성한다.
- When: 문서를 저장한다.
- Then: 상태는 `DRAFT`, AI 상태는 `NOT_STARTED`로 시작한다.

### 테스트 케이스 2
- Given: `DRAFT` 문서가 존재한다.
- When: 사용자가 문서를 `ACTIVE`로 전환한다.
- Then: 문서 상태는 `ACTIVE`가 된다.

### 테스트 케이스 3
- Given: `ACTIVE` 문서가 존재하고 AI 상태가 `NOT_STARTED`다.
- When: 사용자가 분석을 요청한다.
- Then: AI 상태는 `PENDING`이 된다.

### 테스트 케이스 4
- Given: 문서 상태가 `DRAFT`다.
- When: 사용자가 분석을 요청한다.
- Then: 예외가 발생한다.

### 테스트 케이스 5
- Given: 문서 상태가 `ACTIVE`이고 AI 상태가 `PROCESSING`이다.
- When: 사용자가 분석을 다시 요청한다.
- Then: 예외가 발생한다.

### 테스트 케이스 6
- Given: `ACTIVE` 문서가 존재한다.
- When: 사용자가 내용을 수정한다.
- Then: revision 저장이 요청된다.

### 테스트 케이스 7
- Given: 검색 대상 문서 목록이 있다.
- When: 사용자가 검색어를 입력한다.
- Then: 본인 `ACTIVE` 문서만 검색 결과에 포함된다.

### 테스트 케이스 8
- Given: 문서가 조회된 이후 다른 사용자가 먼저 수정했다.
- When: 사용자가 이전 `updated_at` 값으로 수정 요청을 보낸다.
- Then: 낙관적 잠금 예외가 발생한다.
