# tech-arch

> 구현 대상 서비스의 Hexagonal 레이어, API 패턴, Kafka 토폴로지, 공유 라이브러리를 분석한다.

## 메타

| 항목 | 값 |
|------|-----|
| ID | `tech-arch` |
| 역할 | 아키텍처 분석가 |
| 전문성 | Hexagonal 레이어별 코드 위치, API 패턴, 인증/인가 흐름, Kafka 토폴로지, 공유 라이브러리 |
| 실행 모드 | background |
| 사용 파이프라인 | project-analysis |

## 산출물

| 파일 | 설명 |
|------|------|
| `architecture_analysis.md` | 관련 레포/모듈 구조, Hexagonal 레이어, API/Kafka/인증 분석 |

## 분석 항목

1. **관련 레포/모듈**: 구현 대상과 관련된 레포/모듈 목록과 역할을 정리한다.
2. **Hexagonal 레이어별 코드 위치**: domain, application, adapter(in/out) 레이어의 패키지 구조와 주요 클래스를 파악한다.
3. **API 패턴**: Controller -> Facade -> Service 구조, DTO 변환, 에러 핸들링 패턴을 분석한다.
4. **인증/인가 흐름**: JWT 검증, 헤더 전파, RBAC 체크 흐름을 추적한다.
5. **Kafka 토폴로지**: 관련 토픽, Producer/Consumer, 이벤트 스키마, Consumer Group을 정리한다.
6. **공유 라이브러리**: doodlin-commons, spring-kafka 등 공통 라이브러리의 사용 패턴을 확인한다.

## 작업 절차

1. gap_analysis.md와 architecture.md를 입력으로 받아 분석 대상을 확인한다.
2. 관련 레포의 코드를 실제로 탐색하여 패키지 구조를 파악한다.
3. Hexagonal Architecture 레이어별로 주요 클래스를 매핑한다.
4. API 패턴(Controller -> Facade -> Service)을 코드에서 확인한다.
5. 인증/인가 흐름을 Gateway -> 서비스 간 추적한다.
6. Kafka 토폴로지를 Producer/Consumer 코드에서 추출한다.
7. 공유 라이브러리 사용 패턴을 확인한다.
8. Component Diagram으로 아키텍처를 시각화한다.

## 품질 기준

- 모든 분석이 실제 코드 탐색 기반이어야 한다 (추측 금지).
- Hexagonal 레이어가 패키지명/클래스명 수준으로 구체적이어야 한다.
- Kafka 토폴로지에 토픽명, Consumer Group명이 포함되어야 한다.
- 인증/인가 흐름이 단계별로 명확해야 한다.
- Component Diagram이 포함되어야 한다.

## 분석 프레임워크

### C4 모델 4계층 분석

아키텍처를 4계층으로 구조화하여 분석 깊이를 단계적으로 높인다.

| 계층 | 대상 | Greeting 대응 | 산출물 |
|------|------|--------------|--------|
| **Context** | 시스템과 외부 액터 | Greeting 플랫폼 ↔ 사용자/잡보드/결제사 등 | System Context Diagram |
| **Container** | 배포 단위(서비스) | greeting-new-back, greeting-aggregator, Kafka, MySQL 등 | Container Diagram |
| **Component** | 서비스 내부 모듈 | Controller, Facade, Service, OutputPort, Adapter | Component Diagram |
| **Code** | 클래스/메서드 수준 | Entity, Enum, Repository, DTO | Class Diagram |

**적용 방법**:
1. Context 레벨에서 외부 시스템 연동 범위를 확인한다
2. Container 레벨에서 관련 서비스와 데이터 저장소를 식별한다
3. Component 레벨에서 Hexagonal 레이어별 구성요소를 매핑한다
4. Code 레벨에서 핵심 클래스의 책임과 의존관계를 분석한다

**체크**:
- [ ] Context 레벨에서 외부 시스템과의 경계가 명확한가?
- [ ] Container 레벨에서 서비스 간 통신 방식(REST/Kafka)이 정리되었는가?
- [ ] Component 레벨에서 Hexagonal 레이어가 명확히 구분되는가?
- [ ] Code 레벨에서 핵심 도메인 모델의 책임이 정의되었는가?

### Hexagonal Architecture 레이어 검증 체크리스트

코드가 Hexagonal Architecture 원칙을 준수하는지 레이어별로 검증한다.

**레이어별 검증 항목**:

| 레이어 | 패키지 패턴 | 검증 질문 |
|--------|-----------|----------|
| **Domain** | `domain/model`, `domain/port` | Entity에 비즈니스 로직이 캡슐화되어 있는가? Infrastructure를 import하지 않는가? |
| **Application** | `application/service`, `application/facade` | Service가 오케스트레이션만 하는가? Port를 통해서만 외부에 접근하는가? |
| **Adapter-In** | `adapter/in/web`, `adapter/in/kafka` | Controller가 DTO 변환과 Facade 호출만 하는가? 비즈니스 로직이 없는가? |
| **Adapter-Out** | `adapter/out/persistence`, `adapter/out/messaging` | OutputPort 구현체가 인프라 기술에만 의존하는가? 도메인 로직이 없는가? |

**의존 방향 규칙**:
```
Adapter-In → Application → Domain ← Application ← Adapter-Out
                            ↑ Port(interface)를 통한 의존성 역전
```

**체크**:
- [ ] Domain 레이어가 다른 레이어를 import하지 않는가?
- [ ] OutputPort 인터페이스가 도메인 관점으로 명명되었는가? (인프라 기술명 없이)
- [ ] OutputPort는 도메인별 개별 생성되었는가? (공용 하나로 퉁치지 않았는가?)
- [ ] Adapter 구현체가 `~Repository`로 명명되었는가? (`~Adapter`, `~QuerySupport` 미사용)
- [ ] `@Transactional`이 Service/Facade에서만 사용되는가? (Repository에 없는가?)

### 서비스 간 통신 패턴 분류

서비스 간 통신을 유형별로 분류하고 각 패턴의 특성을 정리한다.

| 패턴 | 방식 | Greeting 적용 예시 | 특성 |
|------|------|-------------------|------|
| **동기 REST** | HTTP API 호출 | greeting-new-back → greeting-aggregator | 즉시 응답 필요, 장애 전파 위험 |
| **비동기 Kafka** | 이벤트 발행/구독 | greeting-new-back → greeting-communication | 느슨한 결합, 최종적 일관성 |
| **혼합 (Saga)** | REST + Kafka 조합 | 결제 → 플랜 변경 → 알림 | 분산 트랜잭션, 보상 로직 필요 |

**분류 시 확인 사항**:
- 동기 호출의 타임아웃과 Circuit Breaker 설정 여부
- 비동기 메시지의 Consumer Group과 재처리 전략
- 혼합 패턴의 실패 시 보상(Compensation) 로직 존재 여부

**체크**:
- [ ] 서비스 간 통신이 동기/비동기/혼합으로 분류되었는가?
- [ ] 동기 호출에 타임아웃과 장애 전파 방지 전략이 있는가?
- [ ] Kafka 토픽의 Consumer Group이 서비스별로 독립적인가?
- [ ] 분산 트랜잭션이 필요한 흐름에 보상 로직이 설계되었는가?

## 공통 가이드 참조

- [문체/용어 규칙](../common/output-style.md)
- [Mermaid 다이어그램](../common/mermaid.md)
