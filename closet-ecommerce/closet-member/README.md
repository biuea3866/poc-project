# closet-member

> 회원 인증/인가, 회원 정보 관리, 배송지 관리, 포인트 관리 서비스

## 역할

closet-member는 회원 도메인을 담당하는 서비스이다.
회원가입/로그인(JWT), 회원 등급(NORMAL/SILVER/GOLD/PLATINUM), 포인트 적립/사용, 배송지 CRUD를 관리한다.
Redis를 통해 Refresh Token을 저장하고, BCrypt로 비밀번호를 암호화한다.

## 기술 스택

| 기술 | 용도 |
|------|------|
| Spring Boot Starter Web | REST API |
| Spring Data JPA | 엔티티 매핑, Repository |
| MySQL 8.0 (Flyway) | 데이터 저장 |
| Spring Data Redis | Refresh Token 저장 |
| JJWT 0.12.3 | JWT Access/Refresh Token 발급 및 검증 |
| Spring Security Crypto | BCrypt 비밀번호 해싱 |
| Virtual Threads | 가상 스레드 활성화 |

## 도메인 모델

### Member (Aggregate Root)
회원 엔티티. `email`(unique), `passwordHash`, `name`, `phone`, `grade`, `pointBalance`, `status` 필드를 가진다.
`register()` 팩토리 메서드로 생성하며, `withdraw()`, `upgradeGrade()`, `earnPoints()`, `usePoints()` 등 비즈니스 로직을 캡슐화한다.

### MemberGrade
회원 등급 enum: `NORMAL -> SILVER -> GOLD -> PLATINUM`. 등급별 포인트 적립률을 가지며(1%~5%), `canTransitionTo()` / `validateTransitionTo()`로 상태 전이 규칙을 관리한다.

### MemberStatus
회원 상태 enum: `ACTIVE`, `INACTIVE`, `WITHDRAWN`. WITHDRAWN에서는 다른 상태로 전이 불가.

### ShippingAddress
배송지 엔티티. `memberId`, `name`, `phone`, `zipCode`, `address`, `detailAddress`, `isDefault` 필드. 기본 배송지 설정/해제 기능을 제공한다.

### PointHistory
포인트 변동 이력 엔티티. `memberId`, `type`, `amount`, `balanceAfter`, `reason`, `referenceId` 필드. `earn()`, `use()` 팩토리 메서드를 제공한다.

### PointType
포인트 변동 유형 enum: `EARN`, `USE`, `EXPIRE`, `CANCEL`.

## API

| Method | Path | 설명 |
|--------|------|------|
| POST | /api/v1/members/register | 회원가입 |
| POST | /api/v1/members/login | 로그인 |
| GET | /api/v1/members/me | 내 정보 조회 |
| DELETE | /api/v1/members/me | 회원 탈퇴 |
| POST | /api/v1/members/auth/refresh | 토큰 갱신 |
| POST | /api/v1/members/me/addresses | 배송지 등록 |
| GET | /api/v1/members/me/addresses | 배송지 목록 조회 |
| PUT | /api/v1/members/me/addresses/{id} | 배송지 수정 |
| DELETE | /api/v1/members/me/addresses/{id} | 배송지 삭제 |
| PATCH | /api/v1/members/me/addresses/{id}/default | 기본 배송지 설정 |

## 패키지 구조

```
src/main/kotlin/com/closet/member/
├── application/       # AuthService, MemberService, ShippingAddressService
├── config/            # JwtAuthenticationFilter, JwtTokenProvider, PasswordEncoderConfig
├── domain/            # Member, MemberGrade, MemberStatus, PointHistory, PointType, ShippingAddress
│   └── repository/    # MemberRepository, PointHistoryRepository, ShippingAddressRepository
└── presentation/      # MemberController, ShippingAddressController
    └── dto/           # MemberDtos, ShippingAddressDtos
```

## DB 테이블

| 테이블 | 설명 |
|--------|------|
| member | 회원 정보 (email unique, grade, point_balance, status) |
| shipping_address | 회원 배송지 (member_id, is_default) |
| point_history | 포인트 변동 이력 (type, amount, balance_after, reference_id) |

## 포트

- 서버 포트: 8081

## 의존 서비스

- closet-common (공통 라이브러리)
- Redis (Refresh Token 저장)
