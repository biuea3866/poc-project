# TDD 요약 — employee-service (Step 1-D 산출물)

본 파일은 `.analysis/outputs/` 경로용 요약본입니다. 풀 본문은 `hr-platform/docs/tdd/TDD-001-employee-service.md` 참조.

## 한 줄 요약

hr-platform 첫 도메인 **employee-service**를 Hexagonal 4-layer · Rich Domain Model로 구현. Person+Employment 분리 SSOT 모델 · 22개 API · 9종 Kafka 이벤트 · 4단계 상태 머신 · 16티켓 7-wave DAG.

## 채택 핵심 결정

1. **빌드 골격**: Gradle 8.14 + Kotlin 2.0 + Spring Boot 3.4 멀티모듈, 서비스 1개 = Spring Boot Application 1개, 공통 모듈 2종(`core`, `common-kafka`). 자세히 → [ADR-001](../../../hr-platform/docs/adr/ADR-001-hexagonal-multimodule.md)
2. **레이어**: presentation / application / domain / infrastructure 4-layer. Port 인터페이스 미사용. UseCase는 DomainService만 호출.
3. **SSOT 모델**: Person 1:N Employment, Department Materialized Path, EmploymentHistory append-only. 자세히 → [ADR-002](../../../hr-platform/docs/adr/ADR-002-employee-ssot-model.md)
4. **상태 머신**: PRE_HIRED → ACTIVE → ON_LEAVE → RESIGNED · 7 전이 · 9 이벤트
5. **API 보강**: PRD 11개 + 사전 리뷰 누락 11개 = 22개

## 채택 미채택 알고리즘 요약

| 결정 | 채택 | 미채택 후보 (이유) |
|---|---|---|
| 빌드 골격 | 단일 레포 멀티모듈 | (A) 레포 분리(부담 큼) / (B) Modular Monolith(이벤트 분리 요구 위반) |
| 도메인 모델 | Person+Employment 분리 | Single User(글로벌 확장 차단) / Event Sourcing(MVP 학습 비용) |
| 부서 트리 | Materialized Path | Adjacency List(SLO 미달) / Closure Table(SMB 규모 과한 복잡도) / Nested Set(이동 시 다수 행 갱신) |

## 핵심 다이어그램 인덱스 (TDD 본문 참조)

- Component Diagram — 4-layer 의존 관계 (TDD §Detail Design)
- Sequence Diagram — 입사 등록 (golden) + 휴직 (보강) 2종 (TDD §Detail Design)
- ERD — Person/Employment/Department/EmploymentHistory 4 엔티티 (TDD §ERD)

## 후속 단계

| 단계 | 산출물 | 비고 |
|---|---|---|
| Step 2 직전 | `hr-platform/docs/tickets/HR-M1-EMPLOYEE-TICKETS.md` | 본 파이프라인 종료점 |
| 별도 호출 | wave 1~7 worktree 스폰 (BS-01 → BE-12) | `/feature` 또는 `/implement` 후속 호출 |
| 별도 호출 | doc-sync 스킬 (TDD → 위키 게시) | 사용자 확인 후 |
| 별도 호출 | jira-ticket 스킬 (티켓 md → Jira) | 사용자 확인 후 |

## 검증

- [x] 필수 섹션 9종 (Background/Overview/Terminology/Define Problem/Possible Solutions/Detail Design/ERD/Testing Plan/Release Scenario/Project Information/Document History) 모두 채움
- [x] Mermaid 다이어그램 3종 (Component LR / Sequence × 2 / ERD) 포함
- [x] 사전 리뷰(Step 0) 보강 지시 10개 항목 모두 반영
- [x] TPM 검수(Step 1-B) PASS_WITH_NOTES 보정 항목은 티켓 분해 단계에서 반영 예정 (L 사이즈 분할 등)
- [x] 메모리 룰 8종 준수 명시 (be-code-convention · usecase-domain-service · querydsl · encapsulation · zoned-datetime · tdd-first · integration-test-required · transactional-location)
