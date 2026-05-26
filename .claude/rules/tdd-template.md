# TDD 템플릿 가이드

## 필수 섹션

TDD(Technical Design Document)는 아래 섹션을 모두 포함합니다.

```markdown
# {기능명} TDD

## Background
{프로젝트 배경과 동기}

## Overview
{전체 요약 — 무엇을, 왜, 어떻게}

## Terminology
{용어 정의 테이블}

## Define Problem
### AS-IS
{현재 구조와 문제점}
### TO-BE
{목표 구조}

## Possible Solutions
### 벤치마킹 참조 제품
| 제품명 | 카테고리 | 참조 URL | 참조 패턴 |
### 방안 비교
| 방안 | 설명 | 왜 채택 | 미채택 대안 |

## Detail Design
### 클래스 역할 정의
#### 도메인 모델
| 클래스명 | 역할 | 핵심 책임 |
#### 서비스 클래스
| 클래스명 | 역할 | 입력 → 출력 | 의존 |
### AS-IS / TO-BE 비교
### Component Diagram (Mermaid flowchart LR)
### Sequence Diagram (Mermaid)

## ERD
{Mermaid erDiagram — DDL 전문은 티켓에, TDD에는 요약만}

## Testing Plan
TDD에서는 **세 계층의 커버리지 전략**을 명시합니다. 각 티켓은 [`ticket-guide.md`](./ticket-guide.md)의 형식에 따라
세 계층 케이스를 모두 채워야 하며, TDD는 그 상위에서 **계층별 범위·도구·환경**을 정의합니다.

| 계층 | 범위 | 도구 | 환경 |
|---|---|---|---|
| 단위 (Unit) | 도메인 모델·서비스의 순수 로직, 상태 전이, 정책 | Kotest BehaviorSpec + MockK | JVM 단독, 외부 의존 없음 |
| 레포지토리 (Repository) | JPA 매핑·QueryDSL 쿼리·인덱스·트랜잭션·낙관락·고유제약 | `@DataJpaTest` + Testcontainers | 실 DB 컨테이너 |
| 시나리오 (Scenario) | UseCase·Facade End-to-End — 외부 이벤트 진입부터 상태·발행까지 | `@SpringBootTest` + Testcontainers + WireMock | DB + Redis + Kafka + 외부 stub |

다음 항목을 본문에 포함합니다.
- 각 계층의 **목표 커버리지**(라인/브랜치)와 측정 도구(Kover 등).
- 시나리오 계층에서 **반드시 다루는 플로우 목록**(메인, 멱등, 보상, 동시성, 외부 장애).
- 성능/부하 테스트가 있는 경우 별도 절로 분리 (kafka throughput, p99 응답 등).
- `BaseIntegrationTest` 같은 공유 인프라가 필요하면 그 위치와 사용 규칙.

## Release Scenario
{배포 순서, 마이그레이션 선/후 조건, 롤백 플랜}

## Project Information
{일정, 담당자, Jira Epic}

## Document History
| 날짜 | 변경 내용 | 작성자 |
```

## 작성 규칙

### 방안 비교 테이블

- "무엇인가" 대신 "설명" 열을 사용합니다.
- 각 방안을 풀어서 기술합니다. 표만 나열하지 않습니다.
- 채택 이유와 미채택 대안을 명시합니다.

### 상태값 한글화

- 완료, 실패, 부분 실패, 진행 중, 대기
- enum 이름은 영문 유지, 설명은 한글로 씁니다.

### 기술 용어 한글화

| 원문 | 한글 |
|------|------|
| fire-and-forget | 결과 미추적 |
| 4-phase | 4단계 그룹 병렬 |
| blue-green | 블루그린 배포 |
| shadow traffic | 섀도 트래픽 |
| dual write | 이중 기록 |
| strangler fig | 점진적 대체 |

### 선택 섹션

- **FE 영향 분석**: API 변경/이관 시 필수 (FE 의존 서비스, 전환 전략, 핵심 리스크)
- **Security Information**: 보안 관련 변경 시
- **Milestone**: 단계별 마일스톤이 있을 때
