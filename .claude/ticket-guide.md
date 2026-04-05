# Closet 티켓 작성 가이드

> 모든 노션 티켓은 이 가이드의 구조를 따라야 한다.
> 기존 완료 티켓(CP-5, CP-9, CP-13)을 레퍼런스로 한다.

## BE 티켓 구조

### 1. 개요
- **목적**: 이 티켓이 해결하는 문제/구현하는 기능을 2~3문장으로 설명
- **범위**: bullet 목록으로 구현 범위 (테이블, API, Consumer 등)
- **관련 유저스토리**: US-XXX 참조

### 2. 데이터 모델
- **테이블 DDL**: `CREATE TABLE` 전체 SQL (코드블록, sql)
  - FK/ENUM/JSON/BOOLEAN 금지, TINYINT(1), DATETIME(6), COMMENT 필수
- **Enum 정의**: 테이블 형태 (값 | 설명 | 전이 가능 상태)
- **ERD 관계**: 테이블 간 관계 설명 (FK 없이 애플리케이션 레벨 참조)

### 3. API 스펙
각 엔드포인트마다:
- **메서드 + URL** (예: `POST /api/v1/coupons`)
- **설명**: 한 줄
- **Request Body**: JSON 코드블록
- **Validation Rules**: 테이블 (필드 | 규칙 | 에러 코드)
- **Response 200/201**: JSON 코드블록
- **Error Responses**: 테이블 (상태코드 | 에러코드 | 설명)

### 4. Kafka 스펙 (해당 시)
- **Producer**: 발행하는 이벤트 (토픽, eventType, 페이로드 JSON)
- **Consumer**: 수신하는 이벤트 (토픽, 처리 로직, 에러 처리)
- **Outbox 패턴**: 트랜잭션 처리 방식
- 토픽 네이밍: `event.closet.{domain}` (ClosetTopics 상수)

### 5. 시퀀스 다이어그램
- Mermaid `sequenceDiagram` 코드블록
- 주요 플로우 1~2개 (정상 + 에러)

### 6. 상태 머신 (해당 시)
- Mermaid `stateDiagram-v2` 코드블록
- 상태 전이 규칙 (enum 내부 캡슐화)

### 7. AC (Acceptance Criteria)
- `to_do` 체크리스트 형태 (10개 이상)
- 각 항목은 검증 가능한 구체적 조건
- 예: "쿠폰 발급 시 사용자당 발급 한도를 초과하면 409 에러가 반환된다"

### 8. 테스트 시나리오
- **단위 테스트**: 테이블 (테스트 | Given | When | Then)
  - Kotest BehaviorSpec 기준
- **통합 테스트**: 테이블 (테스트 | 설명 | 검증 항목)
  - TestContainers (MySQL, Redis, Kafka, ES)

---

## FE 티켓 구조

### 1. 개요
- 목적, 범위, 관련 유저스토리

### 2. 페이지/컴포넌트 구조
- 페이지 라우팅 (`/promotions`, `/promotions/[id]`)
- 컴포넌트 트리 (서버/클라이언트 구분)
- 상태 관리 방식

### 3. API 연동 스펙
- 호출하는 BE API 목록
- Request/Response 타입 (TypeScript interface)
- 에러 핸들링 방식

### 4. UI/UX 요구사항
- 반응형 브레이크포인트
- 인터랙션 (로딩, 스켈레톤, 토스트)
- 접근성 요구사항

### 5. AC (to_do 체크리스트)

### 6. 테스트 시나리오
- 컴포넌트 테스트 + E2E 시나리오

---

## DevOps 티켓 구조

### 1. 개요
### 2. 인프라 구성
- Docker Compose / Dockerfile
- CI/CD 파이프라인 (GitHub Actions YAML)
- 모니터링 (Prometheus/Grafana 설정)
### 3. 구성 파일 스펙 (코드블록)
### 4. AC (to_do 체크리스트)
### 5. 검증 시나리오

---

## QA 티켓 구조

### 1. 개요
### 2. 테스트 범위
- 서비스별 TC 매트릭스
### 3. 테스트 케이스 상세
- 정상 / 예외 / 엣지 케이스 테이블
### 4. 비기능 테스트 (해당 시)
- 성능, 동시성, 보안
### 5. AC (to_do 체크리스트)

---

## 공통 규칙

- **코드 컨벤션**: ZonedDateTime, QueryDSL, 엔티티 캡슐화, Facade 패턴
- **DB 규칙**: FK/ENUM/JSON 금지, TINYINT(1) boolean, DATETIME(6), COMMENT 필수
- **Kafka**: `event.closet.{domain}` 토픽, DTO 직접 매핑, Outbox 패턴
- **시간**: ZonedDateTime (LocalDateTime 금지)
- **테스트**: Kotest BehaviorSpec, Given/When/Then, TestContainers
