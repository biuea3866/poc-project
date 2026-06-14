# ADR-0001. 배치 트랜잭션 경계와 헥사고날 컨벤션의 조정

- 상태: 채택
- 날짜: 2026-06-13

## 배경

프로젝트 컨벤션(`be-code-convention.md`)은 다음을 강제합니다.

- UseCase는 DomainService만 호출하고 Repository/Gateway를 직접 참조하지 않는다.
- `@Transactional`은 UseCase 또는 DomainService에 선언한다.

그러나 Spring Batch는 **chunk 단위로 트랜잭션을 프레임워크가 소유**합니다. "아이템 1건당 `@Transactional` UseCase 호출"은 다음 문제를 만듭니다.

1. 발송 단위(묶음) 수십만 건 × 트랜잭션 → 처리량 붕괴
2. chunk 트랜잭션과 아이템 트랜잭션의 중첩
3. AsyncItemProcessor·멀티스레드 step 에서 트랜잭션-스레드 바인딩 충돌

## 결정

배치 맥락에 맞게 레이어 책임을 다음과 같이 조정합니다.

| 항목 | 결정 |
|---|---|
| 트랜잭션 경계 | **chunk(Step)이 소유**. UseCase에 `@Transactional` 없음 |
| `SendNotificationGroupUseCase` | 트랜잭션 없는 "발송 오케스트레이션"으로 축소. DomainService(`NotificationDispatchService`)만 호출 |
| 묶음/단건 규칙 | 도메인 `NotificationGroup.toMessage()` 에 캡슐화 (Rich Domain Model 유지) |
| sent 영속화 | DomainService가 아니라 **chunk 단위로 Writer가 bulk update** → chunk 트랜잭션과 정렬 |
| Reader/Processor/Writer/Config | infrastructure 레이어(Spring Batch는 기술 어댑터) |

즉 컨벤션의 "UseCase→DomainService→Gateway" 호출 방향은 유지하되, **영속화 시점만 chunk 경계로 이동**합니다.

## 전략 2(tasklet)의 추가 결정

tasklet 1회 호출 = 단일 트랜잭션이면 100만 행 update가 하나의 트랜잭션이 되어 undo log가 폭증합니다.
따라서 tasklet step은 `ResourcelessTransactionManager`(DB 무관)로 두고, tasklet 내부에서 1000건 단위
독립 트랜잭션(`TransactionTemplate`)으로 커밋합니다("내부 수동 chunking").

## 대안과 미채택 사유

| 대안 | 미채택 사유 |
|---|---|
| 아이템별 `REQUIRES_NEW` UseCase | 수십만 트랜잭션 → 성능 붕괴. 벤치마크 목적과 정면 충돌 |
| DomainService가 sent 영속화까지 수행 | chunk 단위 bulk update가 불가능해져 청크 의미·성능 손상 |

## 영향

- `SendNotificationGroupUseCase`는 컨벤션상 UseCase가 트랜잭션을 갖는 규칙에서 의도적으로 벗어납니다.
- 이 일탈은 배치 도메인에 한정되며, 일반 API 서비스에는 적용하지 않습니다.
