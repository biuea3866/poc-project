# closet-common

> 전체 모듈이 공유하는 공통 라이브러리 (엔티티 베이스, 예외 처리, API 응답, VO)

## 역할

closet-common은 다른 모든 서비스 모듈이 의존하는 공통 모듈이다.
BaseEntity, Money VO, 글로벌 예외 처리, 통일된 API 응답 포맷, 테스트 픽스처 등을 제공한다.
bootJar는 비활성화되어 있으며 라이브러리로만 사용된다.

## 기술 스택

| 기술 | 용도 |
|------|------|
| Spring Boot Starter Web | 웹 관련 기본 의존성 |
| JPA (Jakarta Persistence) | BaseEntity의 엔티티 매핑 |
| kotlin-logging | 로깅 |
| Testcontainers (MySQL, Redis) | 통합 테스트 인프라 |

## 도메인 모델

### BaseEntity
모든 엔티티의 부모 클래스. `id`(auto increment), `createdAt`, `updatedAt`, `deletedAt` 필드를 제공하며 JPA Auditing으로 자동 관리된다. `softDelete()` 메서드로 논리 삭제를 지원한다.

### Money (Value Object)
금액을 표현하는 `@Embeddable` VO. `BigDecimal` 기반이며 `+`, `-`, `*` 연산자와 `Comparable`을 구현한다. 0 미만 금액은 생성 시 거부된다.

### ErrorCode
공통 에러 코드 enum. `INVALID_INPUT(C001)`, `ENTITY_NOT_FOUND(C002)`, `INTERNAL_SERVER_ERROR(C003)`, `UNAUTHORIZED(C004)`, `FORBIDDEN(C005)`, `DUPLICATE_ENTITY(C006)`, `INVALID_STATE_TRANSITION(C007)`을 정의한다.

### ApiResponse / ErrorResponse
통일된 API 응답 포맷. `success`, `data`, `error` 필드를 가지며 `ok()`, `created()`, `fail()` 팩토리 메서드를 제공한다.

### GlobalExceptionHandler
`@RestControllerAdvice`로 `BusinessException`, `MethodArgumentNotValidException`, `IllegalArgumentException`, 일반 `Exception`을 처리한다.

## 패키지 구조

```
src/main/kotlin/com/closet/common/
├── config/         # JpaAuditingConfig
├── entity/         # BaseEntity
├── exception/      # BusinessException, ErrorCode, GlobalExceptionHandler
├── response/       # ApiResponse, ErrorResponse
└── vo/             # Money

src/test/kotlin/com/closet/common/test/
├── BaseIntegrationTest   # Testcontainers 싱글턴 (MySQL + Redis)
└── fixture/              # MemberFixture, OrderFixture, ProductFixture
```

## 의존 서비스

독립 라이브러리 모듈 (다른 서비스에 의존하지 않음)
