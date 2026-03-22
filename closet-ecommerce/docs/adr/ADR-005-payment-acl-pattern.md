# ADR-005: 결제 PG ACL 패턴

## 상태: 승인

## 컨텍스트
결제 시스템에서 외부 PG(Payment Gateway)사와의 연동이 필요하다.
- PG사 변경 가능성 (Toss → NHN KCP 등)
- PG사별 API 스펙이 상이
- 도메인 로직이 특정 PG사에 종속되면 안 됨
- Anti-Corruption Layer(ACL) 패턴 적용 검토

## 결정
`PaymentGateway` 인터페이스를 정의하고, PG사별 어댑터를 구현하는 ACL 패턴을 적용한다.

```
도메인 계층                 ACL(어댑터 계층)              외부
PaymentService  →  PaymentGateway(Port)  ←  TossPaymentAdapter  →  Toss API
                                         ←  KcpPaymentAdapter   →  KCP API
```

- `PaymentGateway` 인터페이스: `approve()`, `cancel()`, `partialCancel()` 정의
- `TossPaymentAdapter`: Toss Payments API 구현체
- 요청/응답 DTO는 도메인 모델로 변환 (PG사 DTO가 도메인에 침투하지 않음)

## 이유
- PG사 교체 시 어댑터만 추가/교체하면 됨 (OCP 준수)
- 도메인 로직이 외부 API 스펙에 오염되지 않음
- 테스트 시 Mock 어댑터로 대체 가능
- DDD의 Anti-Corruption Layer 패턴과 일치

## 결과
- `PaymentGateway` 인터페이스는 `payment` 모듈의 도메인 계층에 위치
- 어댑터 구현체는 `payment` 모듈의 인프라 계층에 위치
- PG 응답은 반드시 도메인 모델(`PaymentApproval`, `PaymentCancellation`)로 변환
- PG사별 에러 코드는 내부 에러 코드(`PAY0XX`)로 매핑
- WebClient/RestClient로 PG API 호출, 타임아웃/재시도 정책 포함

## 대안 (검토했으나 선택하지 않은 것)

### PG SDK 직접 사용 (추상화 없이)
- 장점: 빠른 구현, PG사 기능 100% 활용
- 단점: PG사 교체 시 도메인 코드 전면 수정, 테스트 어려움
- 기각 사유: 유지보수성과 테스트 용이성 저해

### 범용 결제 라이브러리 사용 (iamport 등)
- 장점: 다중 PG 지원, 표준화된 API
- 단점: 외부 라이브러리 의존, 커스터마이징 제한
- 기각 사유: 학습 목적으로 직접 구현하여 결제 플로우 이해

### Strategy 패턴 (런타임 PG 선택)
- 장점: 런타임에 PG사 전환 가능
- 단점: 현재 단일 PG사만 사용, 불필요한 복잡성
- 기각 사유: ACL 패턴으로도 추후 Strategy 확장 가능, 현 단계에서는 과설계
