# Greeting Platform — Doodlin Workspace

46개 레포로 구성된 ATS(채용 관리 시스템) 채용 플랫폼.

## 플랫폼 개요

Greeting은 B2B SaaS 채용 관리 시스템이다. 채용 공고 → 지원자 접수 → 평가 → 합격까지의 파이프라인을 관리한다.

주요 제품:
- **Greeting ATS**: 채용 관리 (공고, 지원자, 평가, 면접)
- **Greeting TRM**: 인재 관리 (탤런트 풀)
- **Offercent**: 보상/오퍼 관리
- **Greepick AI**: AI 서류 평가, 매칭

## 기술 스택

| 영역 | 기술 |
|------|------|
| BE (메인) | Kotlin, Spring Boot 3.x, JPA/Hibernate, QueryDSL |
| BE (레거시/워커) | Node.js, NestJS, Express |
| DB | MySQL 8.0 (Flyway), MongoDB 5.0 |
| 메시징 | Apache Kafka (Avro, spring-kafka) |
| 캐시 | Redis 7.0 |
| 검색 | OpenSearch (offercent-search) |
| 인프라 | AWS EKS, S3, Terraform |
| 빌드 | Gradle (Kotlin DSL), npm/pnpm |
| 테스트 | Kotest (BehaviorSpec), Testcontainers, MockK |

## 개발 환경

```
JAVA_HOME=/Users/biuea/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
```

- Java 17 (Amazon Corretto)
- Gradle 8.x (각 프로젝트 gradlew 사용)
- Node.js 18+ (Node 프로젝트)

## 서비스 아키텍처

```
                        ┌─────────────┐
                        │ API Gateway │
                        └──────┬──────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
    ┌────▼────┐          ┌─────▼─────┐         ┌────▼────┐
    │greeting │          │  greeting │         │offercent│
    │-new-back│          │-aggregator│         │  -bff   │
    │(메인ATS)│          │(집계/오케) │         │(보상BFF)│
    └────┬────┘          └─────┬─────┘         └────┬────┘
         │                     │                     │
    ┌────┼────┬────┬──────┬────┤              ┌──────┼──────┐
    │    │    │    │      │    │              │      │      │
   MySQL Mongo Kafka Redis  S3  PG           search  user  offer
```

## 레포 맵

### BE — JVM (Kotlin/Java)

| 레포 | 역할 |
|------|------|
| **greeting-new-back** | 메인 ATS 백엔드 (Hexagonal Architecture). 공고, 지원자, 평가, 면접 |
| **greeting-aggregator** | 서비스 간 오케스트레이션 (결제 Facade 등) |
| **greeting-api-gateway** | API Gateway (인증, 라우팅, 플랜 체크) |
| **greeting-communication** | 메일/문자/알림톡 발송 |
| **greeting-integration** | 외부 서비스 연동 (잡보드, 무하유 등) |
| **greeting-workspace-server** | 워크스페이스 관리 |
| **greeting_authn-server** | 인증 서버 (JWT) |
| **greeting_authz-server** | 인가 서버 (RBAC) |
| **greeting_payment-server** | 결제 서버 (Toss Payments, 구독, 크레딧) |
| **greeting_dashboard-back** | 대시보드 백엔드 |
| **greeting_trm-server** | TRM 서버 |
| **greeting-ats** | ATS 레거시 |
| **greeting-expired_applicant_processor** | 만료 지원자 처리 |
| **greeting-shiftee-worker** | Shiftee 연동 워커 |
| **offercent-bff** | 오퍼센트 BFF |
| **offercent-search** | 오퍼센트 검색 (OpenSearch) |
| **offercent-user** | 오퍼센트 유저 |
| **opening** | 공고 서비스 |
| **integration** | 통합 서비스 |
| **doodlin-commons** | 공통 라이브러리 |
| **doodlin-communication** | 커뮤니케이션 공통 |
| **spring-kafka** | Kafka 공통 |

### BE — Node.js

| 레포 | 역할 |
|------|------|
| **greeting-alert-server** | 알림 서버 (WebSocket) |
| **greeting-notifiacation_server** | 알림 발송 서버 |
| **greeting-excel-worker** | 엑셀 처리 워커 |
| **greeting_plan-data-processor** | 플랜 다운그레이드 처리 (Kafka Consumer) |
| **greeting_recruitment-api** | 채용 API |
| **next-greeting** | Next.js 서버 |
| **doodlin-utils** | 유틸리티 |

### 인프라/데이터

| 레포 | 역할 |
|------|------|
| **greeting-db-schema** | DB 스키마 (Flyway 마이그레이션) |
| **greeting-topic** | Kafka 토픽 (Terraform) |
| **data-normalizer** | 데이터 정규화 |
| **greepick-ai** | AI 서비스 |

## 공통 컨벤션

### Git
- 브랜치: `feature/grt-{번호}`, `fix/grt-{번호}`, `refactor/xxx`
- base branch: `dev` (대부분), `main` (일부)
- PR → 코드 리뷰 → dev 머지 → QA → prod 배포

### 코드
- Kotlin: 엔티티에 비즈니스 로직 캡슐화, Service는 얇게
- enum에 상태 전이 규칙 캡슐화 (`canTransitionTo`, `validateTransitionTo`)
- Controller → Facade → Service (SRP)
- DB: FK/JSON/ENUM 미사용, TINYINT(1) for boolean, DATETIME(6), COMMENT 필수

### Feature Flag
- `SimpleRuntimeConfig` + `FeatureFlagService` + `BooleanFeatureKey` 패턴
- Retool에서 배포 없이 런타임 on/off

### 테스트
- Kotest BehaviorSpec (Given/When/Then)
- TestContainers (MySQL, Redis, Kafka)
- `BaseIntegrationTest` 패턴 (싱글턴 컨테이너 + Initializer)

## 분석 파이프라인

`.analysis/` 디렉토리에 8개 파이프라인이 정의되어 있다.

| 파이프라인 | 용도 | 가이드 |
|----------|------|--------|
| **prd** | PRD 분석 | `.analysis/prd/PIPELINE.md` |
| **be-implementation** | BE 구현 설계 (TDD + 티켓) | `.analysis/be-implementation/PIPELINE.md` |
| **inquiry** | 문의/버그 대응 | `.analysis/inquiry/PIPELINE.md` |
| **incident** | 장애 대응 | `.analysis/incident/PIPELINE.md` |
| **pr-review** | PR 코드 리뷰 | `.analysis/pr-review/PIPELINE.md` |
| **release** | 배포 영향 분석 | `.analysis/release/PIPELINE.md` |
| **refactoring** | 리팩토링 계획 | `.analysis/refactoring/PIPELINE.md` |
| **api-change** | API 변경 분석 | `.analysis/api-change/PIPELINE.md` |

사용법: 해당 PIPELINE.md를 읽고 지시에 따라 수행.

## 유틸리티

```bash
# Worktree 관리
bin/worktree-create.sh <repo> <branch>   # 생성
bin/worktree-list.sh [repo]              # 조회
bin/worktree-remove.sh <repo> <branch>   # 제거
```
