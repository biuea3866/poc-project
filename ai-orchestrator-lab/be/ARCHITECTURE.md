# AI Wiki BE 구조

`ai-orchestrator-lab/be`는 실제 백엔드 제품 루트입니다.

## 모듈

- `apps/api`
  Spring Boot 진입점과 HTTP 어댑터
- `core/domain`
  순수 도메인 POJO, 상태 머신, 포트
- `core/application`
  유스케이스와 애플리케이션 서비스
- `adapters/persistence-jpa`
  JPA Entity와 영속성 어댑터

## 강제 규칙

- JPA Entity는 `adapters/persistence-jpa`에만 둡니다.
- 도메인 모델은 `core/domain`에만 둡니다.
- `apps/api`는 도메인 구현 세부사항을 직접 참조하지 않습니다.
- ACTIVE 문서만 분석 요청이 가능하고, PROCESSING 중 재요청은 차단해야 합니다.
