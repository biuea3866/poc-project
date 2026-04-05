# Closet — 의류 이커머스 플랫폼 (무신사 벤치마킹)

의류 이커머스 도메인 학습을 위한 개인 프로젝트. 무신사를 벤치마킹하여 패션 이커머스의 핵심 도메인을 설계·구현한다.

## 프로젝트 개요

무신사 스토어를 참고하여 패션 이커머스의 전체 도메인을 직접 설계하고 구현하며 학습하는 프로젝트.
B2C 마켓플레이스(브랜드 입점 + 자체 브랜드) 모델을 기반으로 한다.

주요 도메인:
- **상품 관리**: 카탈로그, 카테고리(상의/하의/아우터/신발/액세서리), 옵션(사이즈/색상), SKU, 브랜드
- **주문 관리**: 장바구니, 주문 생성, 주문 상태 관리, 부분 취소
- **결제**: PG 연동 (Toss Payments), 결제 수단, 환불
- **배송/물류**: 택배사 연동, 배송 추적, 반품/교환, 묶음 배송
- **재고 관리**: 입출고, 재고 차감, 안전재고 알림, 옵션별 재고
- **회원**: 회원가입, 소셜 로그인, 등급(일반/실버/골드/플래티넘), 포인트/적립금
- **전시**: 메인 페이지, 기획전, 배너, 랭킹, 신상품, 브랜드관
- **검색**: 상품 검색, 필터(카테고리/브랜드/가격/색상/사이즈), 자동완성
- **프로모션**: 쿠폰, 할인, 적립금, 타임세일, 기획전 할인
- **리뷰**: 상품 리뷰, 별점, 포토/영상 리뷰, 사이즈 후기, 리뷰 보상
- **CS/클레임**: 1:1 문의, 반품, 교환, 환불, FAQ
- **정산**: 브랜드/셀러 정산, 수수료 관리, 정산 주기
- **콘텐츠**: 스타일 매거진, 코디 추천, 스냅(OOTD)
- **알림**: 주문/배송 알림, 마케팅 푸시, 재입고 알림
- **셀러**: 브랜드 입점, 셀러 관리, 상품 등록, 셀러 어드민

의류 특화:
- 사이즈 가이드 / 핏 정보 (오버핏/레귤러/슬림)
- 색상 · 소재 관리 (면/폴리에스터/나일론 등)
- 코디 · 스타일링 추천
- 시즌 컬렉션 (SS/FW)
- 성별 · 연령대별 카테고리

## 기술 스택

| 영역 | 기술 |
|------|------|
| BE | Kotlin, Spring Boot 3.x, JPA/Hibernate, QueryDSL |
| DB | MySQL 8.0 (Flyway) |
| 메시징 | Apache Kafka |
| 캐시 | Redis 7.0 |
| 검색 | Elasticsearch / OpenSearch |
| 스토리지 | MinIO (S3 호환, 로컬) → AWS S3 전환 가능 |
| APM | Pinpoint 2.5.1 (HBase + Collector + Web) |
| 인프라 | Docker Compose, GitHub Actions CI/CD |
| FE | Next.js 14 (App Router), TypeScript, Tailwind, Shadcn/ui |
| 빌드 | Gradle (Kotlin DSL) |
| 테스트 | Kotest (BehaviorSpec), Testcontainers, MockK |

## 개발 환경

```
JAVA_HOME=/Users/biuea/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
```

- Java 17 (Amazon Corretto)
- Gradle 8.x (gradlew 사용)
- Docker Desktop

## 서비스 아키텍처

```
                          ┌──────────────┐
                          │  API Gateway │
                          └──────┬───────┘
                                 │
      ┌──────────┬───────────────┼───────────────┬──────────┐
      │          │               │               │          │
 ┌────▼────┐ ┌───▼───┐    ┌─────▼─────┐   ┌─────▼────┐ ┌───▼────┐
 │ product │ │ order │    │  payment  │   │ shipping │ │ member │
 │ service │ │service│    │  service  │   │ service  │ │service │
 └────┬────┘ └───┬───┘    └─────┬─────┘   └─────┬────┘ └───┬────┘
      │          │               │               │          │
    MySQL      MySQL           MySQL           MySQL      MySQL
    Redis      Kafka           Kafka           Kafka      Redis
    ES
```

### 서비스 목록

| 서비스 | 역할 |
|--------|------|
| **product-service** | 상품 카탈로그, 카테고리, 옵션(사이즈/색상), SKU, 브랜드 관리 |
| **order-service** | 장바구니, 주문 생성, 주문 상태 관리, 부분 취소/교환 |
| **payment-service** | PG 연동 (Toss Payments), 결제/환불, 빌링키 관리 |
| **shipping-service** | 택배사 연동, 배송 추적, 반품/교환 물류 |
| **inventory-service** | 옵션별 재고 관리, 입출고, 안전재고 알림 |
| **member-service** | 회원 인증/인가 (JWT, 소셜 로그인), 등급, 포인트 |
| **display-service** | 메인 페이지, 기획전, 배너, 랭킹, 브랜드관 |
| **search-service** | Elasticsearch 기반 상품 검색, 필터, 자동완성 |
| **promotion-service** | 쿠폰, 할인, 적립금, 타임세일 |
| **review-service** | 상품 리뷰, 별점, 포토/영상 리뷰, 사이즈 후기 |
| **cs-service** | 1:1 문의, FAQ, 반품/교환/환불 접수 |
| **settlement-service** | 브랜드/셀러 정산, 수수료 관리 |
| **notification-service** | 이메일/SMS/푸시 알림, 재입고 알림 |
| **content-service** | 스타일 매거진, 코디 추천, OOTD 스냅 |
| **seller-service** | 브랜드 입점, 셀러 어드민, 상품 등록 관리 |
| **api-gateway** | 인증, 라우팅, 레이트 리밋 |

## 공통 컨벤션

### Git
- 브랜치: `feature/{도메인}-{기능}`, `fix/{도메인}-{기능}`, `refactor/{설명}`
- base branch: `main`
- **main 직접 커밋/푸시 절대 금지** — 반드시 feature 브랜치 + PR로만 반영
- PR → 코드 리뷰 → main 머지

### 코드
- Kotlin: 엔티티에 비즈니스 로직 캡슐화, Service는 얇게
- enum에 상태 전이 규칙 캡슐화 (`canTransitionTo`, `validateTransitionTo`)
- Controller → Facade → Service (SRP)
- **Kafka Consumer도 엔드포인트** → 반드시 Facade/Service 경유 (비즈니스 로직 Consumer에 직접 작성 금지)
- DB: FK/JSON/ENUM 미사용, TINYINT(1) for boolean, DATETIME(6), COMMENT 필수
- **시간 타입: `ZonedDateTime`** (LocalDateTime 사용 금지)

### Kafka
- **토픽 네이밍: `event.closet.{domain}`** (도메인 단위 통합, 상태별 분리 금지)
  - 토픽 상수: `ClosetTopics` (closet-common)
  - 이벤트 유형은 메시지 내 `eventType` 필드로 구분
  - 파티션 키: 엔티티 ID (orderId, productId, sku 등) → 동일 엔티티 순서 보장
- **Consumer는 DTO로 직접 매핑** (`ConsumerRecord<String, String>` + ObjectMapper 수동 파싱 금지)
  - `JsonDeserializer` + `trusted.packages` 설정
  - 이벤트 엔벨로프 DTO (eventId, eventType, payload, occurredAt)
- **Outbox 패턴**: 트랜잭션 아웃박스로 이벤트 발행 신뢰성 보장

### Feature Toggle
- `FeatureToggle` + `FeatureToggleService` + `FeatureKey` 패턴
- DB 기반 런타임 on/off (feature_toggle 테이블)

### 테스트 (TDD)
- **TDD 필수: 테스트 먼저 작성 → 구현 → 리팩토링** (Red-Green-Refactor)
- Kotest BehaviorSpec (Given/When/Then)
- TestContainers (MySQL, Redis, Kafka)
- `BaseIntegrationTest` 패턴 (싱글턴 컨테이너 + Initializer)
- **모든 PR에 테스트 코드 필수**
- 에이전트 작업 시에도 테스트 코드를 먼저 작성하고 구현

### 스토리지
- MinIO (S3 호환 API) — Docker Compose에서 로컬 실행
- 이미지 업로드: Presigned URL 방식 (PUT for upload, GET for download)
- 버킷: `closet-product-images`, `closet-review-images`
- AWS S3 전환 시 endpoint만 변경하면 됨

### 모니터링
- Prometheus + Grafana (메트릭/대시보드)
- Loki (로그)
- Pinpoint APM (트레이싱) — http://localhost:8079
- Alertmanager (알림)

## 전직군 구성

| 직군 | 역할 | 주요 산출물 |
|------|------|-----------|
| **PM** | 요구사항, KPI, 우선순위, A/B 테스트 설계 | PRD, 릴리즈 노트 |
| **Designer** | UI/UX 디자인, 디자인 시스템, 와이어프레임 | Figma 목업, 디자인 토큰, 스타일 가이드 |
| **Researcher** | 사용자 리서치, UT, 퍼널 분석, A/B 결과 분석 | 페르소나, UT 리포트, 인사이트 리포트 |
| **BE** | API, 도메인, DB, 이벤트 설계/구현 | API Contract, ADR, ERD, 코드 |
| **FE** | UI 구현, API 연동, 반응형 | 페이지, Storybook, E2E 테스트 |
| **DevOps** | 인프라, CI/CD, 모니터링, 스케일링 | Docker, GitHub Actions, Grafana, Terraform |
| **QA** | 테스트 케이스, API/E2E/부하/보안 테스트 | TC, 검증 리포트, 부하 테스트 결과 |
| **Marketer** | GTM, 프로모션 기획, CRM, 퍼포먼스 마케팅 | 마케팅 캘린더, 캠페인 기획서, UTM 설계 |

## 분석 파이프라인

`.analysis/` 디렉토리에 파이프라인이 정의되어 있다.

| 파이프라인 | 용도 | 가이드 |
|----------|------|--------|
| **prd** | PRD 분석 | `.analysis/prd/PIPELINE.md` |
| **be-implementation** | BE 구현 설계 (TDD + 티켓) | `.analysis/be-implementation/PIPELINE.md` |
| **implementation** | 구현 (티켓 → 코드 → 테스트 → PR) | `.analysis/implementation/PIPELINE.md` |
| **verification** | 검증 (설계 원칙 + AC) | `.analysis/verification/PIPELINE.md` |
| **inquiry** | 문의/버그 대응 | `.analysis/inquiry/PIPELINE.md` |
| **incident** | 장애 대응 | `.analysis/incident/PIPELINE.md` |
| **pr-review** | PR 코드 리뷰 | `.analysis/pr-review/PIPELINE.md` |
| **release** | 배포 영향 분석 | `.analysis/release/PIPELINE.md` |
| **refactoring** | 리팩토링 계획 | `.analysis/refactoring/PIPELINE.md` |
| **api-change** | API 변경 분석 | `.analysis/api-change/PIPELINE.md` |

사용법: 해당 PIPELINE.md를 읽고 지시에 따라 수행.
