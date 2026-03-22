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
| 스토리지 | AWS S3 (상품 이미지, 리뷰 이미지) |
| 인프라 | Docker, AWS (ECS, S3, RDS, CloudFront) |
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
- PR → 코드 리뷰 → main 머지

### 코드
- Kotlin: 엔티티에 비즈니스 로직 캡슐화, Service는 얇게
- enum에 상태 전이 규칙 캡슐화 (`canTransitionTo`, `validateTransitionTo`)
- Controller → Facade → Service (SRP)
- DB: FK/JSON/ENUM 미사용, TINYINT(1) for boolean, DATETIME(6), COMMENT 필수

### Feature Flag
- `SimpleRuntimeConfig` + `FeatureFlagService` + `BooleanFeatureKey` 패턴
- DB 기반 런타임 on/off

### 테스트
- Kotest BehaviorSpec (Given/When/Then)
- TestContainers (MySQL, Redis, Kafka)
- `BaseIntegrationTest` 패턴 (싱글턴 컨테이너 + Initializer)

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

## 구현 기록 규칙

- 모든 구현은 `.analysis/` 디렉토리에 기록
- 역할별 가이드라인: `.analysis/common/ROLE_GUIDELINES.md`
- 템플릿: `.analysis/common/IMPLEMENTATION_LOG_TEMPLATE.md`
- 프레임워크 설명: `.analysis/common/IMPLEMENTATION_LOG.md`
- 기록 없는 PR은 리뷰 거부 사유가 된다
- 구현 기록은 `.analysis/` 디렉토리와 Confluence 양쪽에 기록한다
