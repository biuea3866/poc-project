# HTTP 시나리오 스크립트

IntelliJ HTTP Client / VS Code REST Client 호환 단일 파일.

## 파일

- `resilience-practice.http` — 모든 시나리오를 7개 섹션으로 정리
- `http-client.env.json` — `local` 환경 (`{{orderHost}}` 등 변수)

## 섹션 구성

| 섹션 | 내용 |
|-----|------|
| 1. 기본 주문 호출 | 정상 주문 / `/orders/global` (Redis 글로벌) |
| 2. Fault Injection | payment/inventory 지연·실패 주입 및 복구 |
| 3. TimeLimiter | 5초 지연 → 2초 후 fallback |
| 4. CircuitBreaker OPEN | 실패 ×10회 → OPEN → 즉시 fallback → HALF_OPEN 전이 |
| 5. 로컬 RateLimiter | 1초 10건 초과 시 429 |
| 6. Redis 글로벌 RateLimiter | 1초 20건 초과 시 429 |
| 7. Actuator | health / circuitbreakers / ratelimiters / bulkheads / prometheus |

## 사용법

1. IntelliJ에서 `resilience-practice.http` 열기 → 우상단 환경 드롭다운에서 `local` 선택
2. 각 `### ...` 블록 좌측의 ▶ 클릭
3. VS Code면 REST Client 확장 설치 후 동일하게 동작

## 주의

- **레이트리미터**: HTTP Client는 순차 실행이라 1초 간격이 벌어져 429가 안 뜰 수 있다. 정확한 검증은 `./gradlew :order-service:test`.
- **CircuitBreaker**: 1~10번 호출 사이 시간이 너무 벌어지면 윈도우가 흩어진다. 빠르게 연속 실행 권장.
- **fault inject**: 실패/지연 주입 후 복구 요청을 보내지 않으면 다른 시나리오에 영향이 남는다. 각 시나리오 끝의 복구(`fail=false`, `delay=0`)를 잊지 말 것.
