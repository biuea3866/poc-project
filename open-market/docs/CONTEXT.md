# 📍 현재 컨텍스트 (CONTEXT.md)

> 이 문서는 LLM Agent가 작업 시작 전 **반드시** 읽어야 하는 현재 상태 요약입니다.
> 마지막 업데이트: 2025-01-04

---

## Quick Status

| 항목 | 상태 |
|------|------|
| **현재 Phase** | Phase 0: 프로젝트 세팅 (진행 중) |
| **다음 작업** | Docker Compose 실행 테스트 → Backend 기본 세팅 |
| **블로커** | 없음 |

---

## 역할 분담

| 역할 | 담당 | 현재 상태 |
|------|------|----------|
| **Backend** | Human | 🟡 뼈대 생성 완료, 구현 대기 |
| **Frontend** | LLM Agent | 🟡 뼈대 생성 완료, 구현 대기 |
| **Infrastructure** | LLM Agent | 🟢 Docker Compose 생성 완료 |
| **Mock Server** | LLM Agent | ⚪ 미시작 |

---

## 프로젝트 구조 현황

```
~/feature/flag_project/open-market/
├── README.md                    ✅ 완료
├── docs/                        ✅ 완료 (14개 문서)
│   ├── SERVICE_FLOW.md          # 전체 서비스 플로우
│   ├── WORK_PLAN.md             # 작업 순서 계획
│   ├── CONTEXT.md               # 현재 상태 (이 문서)
│   ├── HISTORY.md               # 작업 히스토리
│   ├── FRONTEND_FLOW_SPEC.md    # 프론트엔드 플로우 스펙
│   ├── INFRA_FLOW_SPEC.md       # 인프라 플로우 스펙
│   └── ...                      # 기타 스펙 문서
├── backend/                     ✅ 뼈대 완료
│   ├── build.gradle.kts         # 루트 빌드 설정
│   ├── settings.gradle.kts      # 멀티모듈 설정
│   ├── api/                     # API 모듈 (뼈대)
│   ├── domain/                  # Domain 모듈 (뼈대)
│   ├── infra/                   # Infra 모듈 (뼈대)
│   └── batch/                   # Batch 모듈 (뼈대)
├── frontend/                    ✅ 뼈대 완료
│   ├── package.json
│   └── src/                     # 디렉토리 구조만
├── infra/                       ✅ Docker Compose 완료
│   ├── docker/
│   │   └── docker-compose.yml   # 로컬 개발 환경
│   └── k6/
│       └── scenarios/smoke.js   # 연기 테스트
└── mock-servers/                ⚪ 미시작
    ├── pg-mock/
    └── channel-mock/
```

---

## 도메인 목록

SERVICE_FLOW.md에 정의된 도메인들:

| 도메인 | 설명 | 우선순위 |
|--------|------|----------|
| **회원 (User)** | 회원가입, 로그인, 권한 | P0 |
| **가게 (Shop)** | 판매자 가게 관리, 입점 | P0 |
| **상품 (Product)** | 상품 CRUD, 옵션, 이미지 | P0 |
| **카테고리 (Category)** | 상품 분류 체계 | P0 |
| **재고 (Stock)** | 재고 수량, 동시성 제어 | P1 |
| **장바구니 (Cart)** | 구매 전 상품 보관 | P1 |
| **주문 (Order)** | 주문 생성, 상태 관리 | P1 |
| **결제 (Payment)** | PG 연동, 결제 처리 | P1 |
| **배송 (Delivery)** | 배송 상태, 송장 관리 | P2 |
| **정산 (Settlement)** | 판매 대금 정산 | P2 |
| **리뷰 (Review)** | 상품/가게 리뷰 | P2 |
| **쿠폰 (Promotion)** | 할인, 쿠폰 | P2 |
| **포인트 (Point)** | 적립금 관리 | P2 |
| **전시 (Display)** | 배너, 기획전 | P3 |
| **검색 (Search)** | ES 검색, 자동완성 | P3 |
| **광고 (Ad)** | 유료 광고 | P3 |
| **알림 (Notification)** | 푸시, 이메일, SMS | P3 |
| **고객센터 (CS)** | 문의, 분쟁 처리 | P3 |
| **외부연동 (External)** | 채널 연동, OpenAPI | P3 |

---

## 기술 스택 확정

### Backend
- **언어**: Kotlin 1.9+
- **프레임워크**: Spring Boot 3.2+
- **ORM**: JPA + QueryDSL
- **DB**: MySQL 8.0
- **캐시**: Redis 7.0 + Redisson
- **메시지큐**: Kafka
- **검색**: Elasticsearch 8.x
- **배치**: Spring Batch

### Frontend
- **프레임워크**: Next.js 14 (App Router)
- **언어**: TypeScript
- **스타일**: Tailwind CSS + shadcn/ui
- **상태관리**: Zustand (클라이언트) + React Query (서버)

### Infrastructure
- **컨테이너**: Docker Compose
- **APM**: Pinpoint 2.5
- **부하테스트**: k6
- **CI/CD**: GitHub Actions

---

## 다음 작업 목록

### 즉시 (Human)
1. [ ] Backend 프로젝트 Gradle 빌드 확인
2. [ ] Application.kt 메인 클래스 생성
3. [ ] 공통 모듈 개발 (ApiResponse, Exception)
4. [ ] 회원 도메인 Entity 설계

### 즉시 (LLM)
1. [ ] Docker Compose 실행 테스트
2. [ ] MySQL 초기화 스크립트 작성
3. [ ] Frontend Next.js 설정 파일 추가
4. [ ] Mock Server 기본 구조 생성

---

## 인프라 접속 정보 (로컬)

| 서비스 | URL | 비고 |
|--------|-----|------|
| Backend | http://localhost:8080 | Spring Boot |
| Frontend | http://localhost:3000 | Next.js |
| MySQL | localhost:3306 | user: openmarket |
| Redis | localhost:6379 | |
| Kafka | localhost:9092 | |
| Kafka UI | http://localhost:8090 | |
| Elasticsearch | http://localhost:9200 | |
| Kibana | http://localhost:5601 | |
| Pinpoint Web | http://localhost:8079 | admin/admin |
| PG Mock | http://localhost:8081 | |
| Channel Mock | http://localhost:8082 | |

---

## LLM Agent 작업 가이드

### 작업 시작 시
1. 이 문서(CONTEXT.md) 읽기
2. WORK_PLAN.md에서 현재 Phase 확인
3. 담당 SPEC 문서 읽기 (FRONTEND_FLOW_SPEC.md 또는 INFRA_FLOW_SPEC.md)
4. 작업 수행

### 작업 완료 시
1. HISTORY.md에 작업 로그 추가
2. 이 문서(CONTEXT.md) 상태 업데이트
3. 커밋 메시지에 Agent 명시

### 커밋 메시지 형식
```
[타입] 설명 - Agent명

예시:
[FEAT] 회원가입 페이지 구현 - Frontend Agent
[INFRA] Docker Compose Pinpoint 추가 - Infra Agent
[FIX] 장바구니 수량 버그 수정 - Frontend Agent
```
