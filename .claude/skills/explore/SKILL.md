---
name: explore
description: 기능/도메인/API/클래스명으로 관련 레포·파일·클래스를 빠르게 찾는다. .architecture/ 스냅샷을 우선 참조하고 필요 시 자동 재생성.
model: opus
user-invocable: true
---

탐색 대상: $ARGUMENTS

스냅샷: !`ls <WORKSPACE_ROOT>/.architecture/ 2>/dev/null | wc -l | tr -d ' '`개 레포 보유

---

**탐색 접근 순서**
1. 아래 도메인 매핑 테이블로 후보 레포 결정
2. `.architecture/<repo>/api-map.md`, `domain-map.md` 읽기 (없으면 `bin/sync-architecture.sh --no-fetch <repo>`)
3. 스냅샷에서 파일 좁힌 후 실제 코드 진입
4. 레이어 순서로 추적: Controller → Facade → Service → Entity ← Repository

> `<MAIN_SERVICE>/domain-map.md`는 32K 초과 — grep 필터 후 읽기:
> `grep -n "<키워드>" .architecture/<PROJECT>/<MAIN_SERVICE>/domain-map.md | grep -i "facade\|service\|port" | head -40`

---

## 도메인 → 레포 매핑

### BE — 핵심 도메인

| 도메인/기능 | 레포 | 비고 |
|------------|------|------|
| <엔티티A> (EntityA) | `<PROJECT>/<MAIN_SERVICE>` | Hexagonal 4-레이어 |
| <엔티티B> (EntityB) | `<PROJECT>/<MAIN_SERVICE>`, `<LISTING_SERVICE>` | <LISTING_SERVICE>은 별도 서비스 |
| <엔티티C> (EntityC) | `<PROJECT>/<MAIN_SERVICE>` | AI 분석 포함 |
| 오케스트레이션 | `<PROJECT>/<AGGREGATOR_SERVICE>` | 10개 adaptor, 80+ Port |
| 워크스페이스 | `<WORKSPACE_SERVICE>` | Kafka: `event.opening.workspace`(Avro) |

### BE — 결제/플랜

| 도메인/기능 | 레포 | 비고 |
|------------|------|------|
| 결제/구독 | `<PAYMENT_SERVICE>` | <PAYMENT_PROVIDER> |
| 플랜 다운그레이드 | `<PLAN_WORKER>` | NestJS, `PLAN_WORKER_KOTLIN_ENABLED` Flag로 Kotlin 이관 중 |

### BE — 인증/인가

| 도메인/기능 | 레포 | 비고 |
|------------|------|------|
| 인증 (AuthN) | `<AUTHN_SERVICE>` | JJWT HS256, SSO/SAML/OIDC, 2FA |
| 인가 (AuthZ) | `<AUTHZ_SERVICE>` | RBAC — WORKSPACE/OPENING/APPLICANT |
| API Gateway | `<API_GATEWAY>` | Spring Cloud Gateway(WebFlux). routes/dev/*.yaml |

### BE — 알림/커뮤니케이션

| 도메인/기능 | 레포 | 비고 |
|------------|------|------|
| 메일/문자/알림톡 | `<COMMUNICATION_SERVICE>` | Kafka `queue.<COMPANY>.{mail\|kakao\|sms}.send.*` |
| 발송 엔진 | `<COMPANY>-communication` | 채널별 실제 발송 |
| 실시간 알림 | `<ALERT_SERVICE>` | Node.js, WebSocket |

### BE — 분석/파일/배치

| 도메인/기능 | 레포 | 비고 |
|------------|------|------|
| 대시보드 | `<DASHBOARD_SERVICE>` | QueryDSL 동적 필터, MySQL+MongoDB |
| 엑셀 처리 | `<PROJECT>/<MAIN_SERVICE>` file-processor 모듈 | Apache POI, Presigned URL |
| 만료 <엔티티A> 처리 | `<BATCH_CLEANUP_SERVICE>` | Spring Batch, 매일 UTC 15:00 |

### BE — <SECONDARY_PRODUCT>/외부연동/인프라

| 도메인/기능 | 레포 | 비고 |
|------------|------|------|
| <SECONDARY_PRODUCT> | `<TRM_SERVICE>` | MongoDB 검색, <PRIMARY_PRODUCT>↔<SECONDARY_PRODUCT> 매칭 |
| 외부 연동 (잡보드) | `<INTEGRATION_SERVICE>` | JobPlanet/Programmers/Shiftee |
| DB 스키마 | `<PROJECT>/<DB_SCHEMA_REPO>` | Flyway, <PRIMARY_PRODUCT> 420개+ 마이그레이션 |
| Kafka 토픽 | `<KAFKA_TOPIC_REPO>` | Terraform. `event.*`/`queue.*`/`cdc.*`/`dlq.*` |
| 공통 라이브러리 | `<COMPANY>-commons`, `spring-kafka` | KafkaMessageProcessor<T>, DLQ 자동 발행 |

### FE

| 도메인/기능 | 레포 | 비고 |
|------------|------|------|
| <PRIMARY_PRODUCT> 메인 | `<FRONT_FE>` | |
| <사용자_페이지> | `<CAREER_FE>` | Pages Router, next-i18next |
| 오퍼 레터 | `<INTERVIEW_FE>` | App Router |
| 설문/폼 | `<FORMS_FE>` | Redux Toolkit + React Query |
| <SECONDARY_PRODUCT> | `<TRM_FE>` | |
