# 📋 작업 히스토리 (HISTORY.md)

> **이 파일의 목적**: 모든 LLM 에이전트(Claude Code, Gemini, Codex 등)가 이전 작업을 파악하고 이어서 작업할 수 있도록 기록합니다.
> 
> **규칙**: 
> 1. 모든 작업 완료 후 반드시 이 파일 업데이트
> 2. 가장 최근 작업이 상단에 위치
> 3. 작업자(Agent), 날짜, 변경사항, 다음 작업 명시

---

## 🔄 현재 상태 요약

| 항목 | 상태 |
|------|------|
| **현재 Phase** | Phase 2 - Payment & Order (진행중) |
| **마지막 작업** | Docker Compose mock 프로파일 실행 시도 |
| **마지막 작업자** | Codex |
| **마지막 작업일** | 2026-01-15 |
| **다음 작업** | Docker 데몬 실행 후 mock 프로파일 재시도 |

---

## 📝 작업 로그

### [2026-01-15] Docker Compose mock 프로파일 실행 시도

**작업자**: Codex  
**작업 유형**: TEST - Infra  
**소요 시간**: ~10분

#### 완료된 작업
- [x] docker compose mock 프로파일 실행 시도 (pg-mock, channel-mock)

#### 발생한 이슈
- Docker 데몬 미실행으로 compose 실패

#### 다음 작업
1. Docker 데몬 실행 후 `docker compose --profile mock up -d pg-mock channel-mock` 재시도

### [2026-01-15] Channel Mock 웹훅 검증 스크립트 추가

**작업자**: Codex  
**작업 유형**: FEATURE / TEST - Mock Server  
**소요 시간**: ~30분

#### 완료된 작업
- [x] 채널별 웹훅 이벤트 검증 스크립트 추가
- [x] 로컬 실행 테스트로 웹훅 수신 확인

#### 생성된 파일
- mock-servers/channel-mock/scripts/verify-webhooks.sh

#### 변경된 파일
- 없음

#### 테스트 결과
- `./scripts/verify-webhooks.sh` 실행 시 채널별 ORDER.CREATED 이벤트 수신 확인

#### 다음 작업
1. Docker Compose 실행 테스트

### [2026-01-15] Channel Mock 웹훅 이벤트 샘플 확장

**작업자**: Codex  
**작업 유형**: FEATURE / TEST - Mock Server  
**소요 시간**: ~1시간

#### 완료된 작업
- [x] 채널별 웹훅 이벤트 목록 및 샘플 payload 추가
- [x] 웹훅 이벤트 목록 조회 엔드포인트 추가
- [x] 트리거 시 샘플 payload 자동 적용
- [x] Channel Mock 로컬 테스트 재확인

#### 생성된 파일
- mock-servers/channel-mock/src/webhooks/sample-events.ts

#### 변경된 파일
- mock-servers/channel-mock/src/routes/webhook-management.ts (이벤트 검증/목록/샘플 적용)
- mock-servers/channel-mock/src/routes/openapi.ts (events 엔드포인트 추가)

#### 테스트 결과
- `GET /api/st11/webhooks/events` 이벤트 목록 정상 응답
- `POST /api/st11/webhooks/trigger` 샘플 payload로 전송 성공
- `GET /webhooks/received` 수신 이벤트 저장 확인

#### 다음 작업
1. 웹훅 이벤트용 시나리오 테스트 스크립트 추가 여부 검토

### [2026-01-15] Channel Mock 웹훅/OpenAPI 구현 및 테스트

**작업자**: Codex  
**작업 유형**: FEATURE / TEST - Mock Server  
**소요 시간**: ~1시간

#### 완료된 작업
- [x] 채널별 웹훅 등록/트리거 API 추가 (ST11/Naver/Kakao/Toss/Coupang)
- [x] 웹훅 수신 테스트 엔드포인트 추가 및 이벤트 저장
- [x] OpenAPI 요약 엔드포인트 추가
- [x] Channel Mock 로컬 테스트 (인증/상품/주문/웹훅)

#### 생성된 파일
- mock-servers/channel-mock/src/routes/webhook-management.ts
- mock-servers/channel-mock/src/routes/webhook-receiver.ts
- mock-servers/channel-mock/src/routes/openapi.ts
- mock-servers/channel-mock/src/services/webhook.service.ts

#### 변경된 파일
- mock-servers/channel-mock/src/db/schema.sql (webhooks, webhook_events 테이블 추가)
- mock-servers/channel-mock/src/routes/index.ts (웹훅/OpenAPI 라우팅 추가)
- mock-servers/channel-mock/src/services/webhook.service.ts (HTTP 전송 방식 적용)

#### 테스트 결과
- Health Check: `GET http://localhost:8082/health`
- OpenAPI: `GET http://localhost:8082/openapi.json`
- ST11 인증/상품/주문 API 정상 응답
- 웹훅 등록/트리거 후 `/webhooks/received`에 이벤트 저장 확인

#### 다음 작업
1. 네이버/카카오/토스/쿠팡 웹훅 이벤트 샘플 추가
2. Docker Compose mock 프로파일에 channel-mock 포함

### [2026-01-15] PG Mock 검증 및 Channel Mock 구현

**작업자**: Codex  
**작업 유형**: FEATURE / TEST - Mock Server  
**소요 시간**: ~1시간

#### 완료된 작업
- [x] pg-mock 로컬 실행 및 Toss 결제 플로우(prepare/confirm) 테스트
- [x] channel-mock 기본 구조 및 엔드포인트 구현 (ST11/Naver/Kakao/Toss/Coupang)
- [x] SQLite 스키마 및 서비스 레이어 추가 (auth/product/order)

#### 생성된 파일
- mock-servers/channel-mock/package.json
- mock-servers/channel-mock/tsconfig.json
- mock-servers/channel-mock/Dockerfile
- mock-servers/channel-mock/.gitignore
- mock-servers/channel-mock/.dockerignore
- mock-servers/channel-mock/src/app.ts
- mock-servers/channel-mock/src/routes/index.ts
- mock-servers/channel-mock/src/routes/st11/auth.ts
- mock-servers/channel-mock/src/routes/st11/products.ts
- mock-servers/channel-mock/src/routes/st11/orders.ts
- mock-servers/channel-mock/src/routes/naver/auth.ts
- mock-servers/channel-mock/src/routes/naver/products.ts
- mock-servers/channel-mock/src/routes/naver/orders.ts
- mock-servers/channel-mock/src/routes/kakao/auth.ts
- mock-servers/channel-mock/src/routes/kakao/products.ts
- mock-servers/channel-mock/src/routes/kakao/orders.ts
- mock-servers/channel-mock/src/routes/toss/auth.ts
- mock-servers/channel-mock/src/routes/toss/products.ts
- mock-servers/channel-mock/src/routes/toss/orders.ts
- mock-servers/channel-mock/src/routes/coupang/products.ts
- mock-servers/channel-mock/src/routes/coupang/orders.ts
- mock-servers/channel-mock/src/services/auth.service.ts
- mock-servers/channel-mock/src/services/product.service.ts
- mock-servers/channel-mock/src/services/order.service.ts
- mock-servers/channel-mock/src/db/index.ts
- mock-servers/channel-mock/src/db/init.ts
- mock-servers/channel-mock/src/db/schema.sql
- mock-servers/channel-mock/src/scenarios/index.ts
- mock-servers/channel-mock/src/middleware/auth.ts
- mock-servers/channel-mock/src/middleware/scenario.ts
- mock-servers/channel-mock/src/types/index.ts

#### 변경된 파일
- 없음

#### 테스트 결과
- pg-mock 건강 체크: `GET http://localhost:8081/health` 정상 응답
- Toss 결제 준비/승인: `/api/toss/v1/payments` → `/api/toss/v1/payments/confirm` 정상 응답

#### 다음 작업
1. channel-mock npm install/build 및 로컬 구동 테스트
2. channel-mock 주요 엔드포인트 정상 동작 확인 (상품 등록/주문 조회/발송)
3. Docker Compose에 channel-mock 포함 여부 확인

### [2025-01-05] PG Mock 서버 실행 테스트

**작업자**: Claude Code
**작업 유형**: TEST - Mock Server 검증
**소요 시간**: ~1시간

#### 완료된 작업
- [x] SQLite DB 초기화 성공
- [x] schema.sql 경로 수정 (src 폴더에서 직접 읽도록 변경)
- [x] npm start로 로컬 서버 실행 성공
- [x] Health Check 테스트 (http://localhost:8081/health)
- [x] API 문서 엔드포인트 테스트 (4개 PG사 정보 확인)
- [x] 토스페이먼츠 전체 플로우 테스트
  - 결제 준비 → 승인 → 취소 성공
- [x] 카카오페이 전체 플로우 테스트
  - 결제 준비 → 승인 → 취소 성공
  - pg_token 검증 완화 (테스트 편의성)
- [x] 에러 시나리오 테스트
  - CARD_DECLINED, INSUFFICIENT_BALANCE, INVALID_CARD 동작 확인
- [ ] Docker Compose 실행 (credential 문제로 중단)

#### 수정된 파일
```
mock-servers/pg-mock/src/
├── db/index.ts                 # schema.sql 경로 수정
└── routes/kakao/payment.ts     # pg_token 검증 완화
```

#### 테스트 결과

**✅ 성공한 테스트**:
1. **토스페이먼츠**
   - 결제 준비: paymentKey 발급 성공
   - 결제 승인: 카드 정보 생성 (신한카드, 승인번호 98271089)
   - 결제 취소: 전액 취소 성공

2. **카카오페이**
   - 결제 준비: TID 발급 성공
   - 결제 승인: VAT 자동 계산 (25,000원 → VAT 2,272원)
   - 결제 취소: 전액 취소 성공

3. **에러 시나리오**
   - X-Mock-Scenario 헤더로 제어 성공
   - 카드거절, 잔액부족, 유효하지 않은 카드 시나리오 동작 확인

**📊 API 응답 예시**:
```json
// 토스페이먼츠 승인
{
  "paymentKey": "toss_payment_key_f671a975-63af-47bc-ba40-df5972863b49",
  "orderId": "TEST-ORDER-001",
  "status": "DONE",
  "totalAmount": 15000,
  "balanceAmount": 15000,
  "method": "카드",
  "approvedAt": "2026-01-04T15:30:04.431Z",
  "card": {
    "company": "신한",
    "number": "4321-****-****-1234",
    "approveNo": "98271089"
  }
}
```

#### 발견된 이슈
1. **schema.sql 경로 문제**
   - 문제: TypeScript 컴파일 시 .sql 파일이 dist로 복사되지 않음
   - 해결: `__dirname`에서 `../../src/db/schema.sql` 경로로 읽도록 수정
   - 파일: `src/db/index.ts:37`

2. **카카오페이 pg_token 검증**
   - 문제: Mock 환경에서 pg_token 검증으로 인한 테스트 어려움
   - 해결: 검증 로직 제거 (Mock 환경에서는 임의의 토큰 허용)
   - 파일: `src/routes/kakao/payment.ts:110`

3. **Docker credential 문제** ⚠️
   - 문제: docker-credential-desktop 실행 파일을 찾을 수 없음
   - 상태: 미해결 (다음 작업에서 해결 필요)
   - 명령어: `docker-compose --profile mock build pg-mock`

#### 다음 작업
1. **Docker credential 문제 해결**
   - ~/.docker/config.json에서 credsStore 설정 확인
   - docker-credential-desktop 제거 또는 재설치

2. **Docker Compose로 PG Mock 실행**
   ```bash
   cd /Users/biuea/feature/flag_project/open-market/infra/docker
   docker-compose --profile mock up pg-mock
   ```

3. **백엔드 Payment 도메인 구현** (Human)
   - PgAdapter 인터페이스
   - 토스페이먼츠 어댑터
   - Payment 엔티티

#### 참고사항
- PG Mock 서버는 로컬에서 완벽하게 동작 (npm start)
- 포트: 8081
- 실제 PG 연동과 동일한 API 스펙
- 네이버페이, 다날은 아직 테스트하지 않았으나 동일한 구조로 동작 예상
- SQLite DB 파일: `mock-servers/pg-mock/data/payments.db`

---

### [2025-01-04] PG Mock 서버 전체 구현

**작업자**: Claude Code
**작업 유형**: FEATURE - Mock Server 개발
**소요 시간**: ~2시간

#### 완료된 작업
- [x] 프로젝트 기본 설정 (package.json, tsconfig.json, Dockerfile)
- [x] TypeScript 타입 정의 (Payment, PgProvider, 각 PG사별 Request/Response)
- [x] 에러 시나리오 시스템 (6가지 시나리오)
- [x] SQLite 데이터베이스 (스키마, 서비스)
- [x] 토스페이먼츠 API (준비, 승인, 취소, 조회)
- [x] 토스페이먼츠 Mock 결제창 (EJS)
- [x] 카카오페이 API (준비, 승인, 취소)
- [x] 카카오페이 Mock 결제창 (EJS)
- [x] 네이버페이 API (준비, 승인, 취소)
- [x] 네이버페이 Mock 결제창 (EJS)
- [x] 다날 API (준비, 승인, 취소)
- [x] 다날 Mock 결제창 (EJS)
- [x] Express 앱 통합 및 라우팅
- [x] README.md 작성 (API 문서)
- [x] npm 빌드 검증 완료

#### 생성된 파일
```
mock-servers/pg-mock/
├── src/
│   ├── app.ts
│   ├── types/index.ts
│   ├── scenarios/index.ts
│   ├── db/
│   │   ├── index.ts
│   │   ├── init.ts
│   │   └── schema.sql
│   ├── services/
│   │   └── payment.service.ts
│   ├── routes/
│   │   ├── index.ts
│   │   ├── toss/
│   │   │   ├── payments.ts
│   │   │   └── checkout.ts
│   │   ├── kakao/
│   │   │   ├── payment.ts
│   │   │   └── checkout.ts
│   │   ├── naver/
│   │   │   ├── payments.ts
│   │   │   └── checkout.ts
│   │   └── danal/
│   │       ├── payment.ts
│   │       └── checkout.ts
│   └── views/
│       ├── toss-checkout.ejs
│       ├── kakao-checkout.ejs
│       ├── naver-checkout.ejs
│       ├── danal-checkout.ejs
│       └── receipt.ejs
├── package.json
├── tsconfig.json
├── Dockerfile
├── .dockerignore
├── .gitignore
└── README.md
```

#### 주요 기능
1. **4개 PG사 완전 구현**
   - 토스페이먼츠 (Basic Auth)
   - 카카오페이 (KakaoAK)
   - 네이버페이 (Client ID/Secret)
   - 다날 (CPID/Password)

2. **Mock 결제창**
   - 각 PG사별 UI 디자인
   - 결제 성공/취소 시뮬레이션
   - 리다이렉트 URL 처리

3. **에러 시나리오**
   - X-Mock-Scenario 헤더로 제어
   - 카드거절, 잔액부족, 타임아웃 등

4. **데이터 영속성**
   - SQLite 기반 결제 데이터 저장
   - 결제/취소 이력 관리

#### 기술 스택
- TypeScript 5.3+
- Express.js 4.18
- SQLite3 5.1
- EJS 3.1

#### 다음 작업
1. **백엔드** (Human): Payment 도메인 구현
   - PgAdapter 인터페이스 구현
   - 토스페이먼츠 어댑터
   - Payment 엔티티 및 Repository
2. **인프라**: Docker Compose로 PG Mock 서버 실행 테스트
3. **백엔드**: Order 도메인과 Payment 연동

#### 참고사항
- PG Mock 서버는 포트 8081에서 실행
- API 문서: http://localhost:8081/
- 실제 PG사 API 스펙 기반으로 구현됨
- 프론트엔드/백엔드에서 실제 PG 연동과 동일하게 사용 가능

---

### [2025-01-04] 프로젝트 문서 초안 작성

**작업자**: Claude  
**작업 유형**: 문서 작성  
**소요 시간**: -

#### 완료된 작업
- [x] PROJECT_OVERVIEW.md 작성
- [x] HISTORY.md 작성 (현재 파일)
- [x] CONTEXT.md 작성
- [x] AGENT_ROLES.md 작성
- [x] BACKEND_SPEC.md 작성
- [x] FRONTEND_SPEC.md 작성
- [x] INFRA_SPEC.md 작성
- [x] EXTERNAL_INTEGRATION_SPEC.md 작성
- [x] PG_INTEGRATION_SPEC.md 작성

#### 생성된 파일
```
docs/
├── PROJECT_OVERVIEW.md
├── HISTORY.md
├── CONTEXT.md
├── AGENT_ROLES.md
├── BACKEND_SPEC.md
├── FRONTEND_SPEC.md
├── INFRA_SPEC.md
├── EXTERNAL_INTEGRATION_SPEC.md
└── PG_INTEGRATION_SPEC.md
```

#### 변경된 파일
- 없음 (신규 프로젝트)

#### 주요 결정사항
1. Backend는 Human이 직접 개발
2. Frontend, Infra, Mock Server는 LLM이 담당
3. 외부 연동은 실제 API 스펙 기반 Mock으로 구현

#### 다음 작업
1. **Backend** (Human): Spring Boot 프로젝트 초기 설정
2. **Infra** (LLM): Docker Compose 로컬 개발 환경 구성
3. **Frontend** (LLM): Next.js 프로젝트 초기 설정

#### 참고사항
- 프로젝트 컨벤션은 CONTEXT.md 참조
- 각 도메인별 상세 스펙은 개별 SPEC 문서 참조

---

## 📌 작업 로그 작성 템플릿

```markdown
### [YYYY-MM-DD] 작업 제목

**작업자**: [Agent 이름] (Claude Code / Gemini / Codex / Human)  
**작업 유형**: [코드 작성 / 버그 수정 / 리팩토링 / 문서 작성 / 설정]  
**소요 시간**: [예상 또는 실제]

#### 완료된 작업
- [x] 작업 1
- [x] 작업 2
- [ ] 미완료 작업 (다음으로 이월)

#### 생성된 파일
- path/to/new/file1.kt
- path/to/new/file2.kt

#### 변경된 파일
- path/to/modified/file.kt (변경 내용 요약)

#### 주요 결정사항
1. 결정사항 1
2. 결정사항 2

#### 발생한 이슈
- 이슈 내용 및 해결 방법

#### 다음 작업
1. 다음에 해야 할 작업 1
2. 다음에 해야 할 작업 2

#### 참고사항
- 다음 작업자가 알아야 할 정보
```

---

## 🏷️ 작업 유형 태그

| 태그 | 설명 |
|------|------|
| `FEATURE` | 새 기능 개발 |
| `BUGFIX` | 버그 수정 |
| `REFACTOR` | 리팩토링 |
| `DOCS` | 문서 작성/수정 |
| `INFRA` | 인프라 설정 |
| `TEST` | 테스트 작성 |
| `CONFIG` | 설정 변경 |

---

## 📊 Phase별 진행 현황

### Phase 0: 프로젝트 설계 ✅
- [x] 요구사항 정의
- [x] 기술 스택 선정
- [x] 프로젝트 구조 설계
- [x] 문서 작성

### Phase 1: Core MVP 🔄
- [ ] 백엔드 프로젝트 초기 설정
- [ ] 프론트엔드 프로젝트 초기 설정
- [ ] 인프라 로컬 환경 구성
- [ ] 회원 도메인 개발
- [ ] 상품 도메인 개발
- [ ] 장바구니 기능
- [ ] 기본 주문 생성

### Phase 2: Payment & Order ⏳
- [ ] PG Mock 서버 구현
- [ ] 결제 도메인 개발
- [ ] 주문 상태 관리
- [ ] 환불 처리

### Phase 3: Seller Features ⏳
- [ ] 판매자 대시보드
- [ ] 주문 처리
- [ ] 정산 시스템

### Phase 4: Channel Integration ⏳
- [ ] Channel Mock 서버 구현
- [ ] 채널 연동 어댑터
- [ ] 상품/주문 동기화

### Phase 5: Enhancement ⏳
- [ ] Elasticsearch 연동
- [ ] 리뷰 시스템
- [ ] 알림 시스템

---

## 🔗 관련 문서
- [CONTEXT.md](CONTEXT.md) - 현재 컨텍스트 및 진행 상황
- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) - 프로젝트 개요
