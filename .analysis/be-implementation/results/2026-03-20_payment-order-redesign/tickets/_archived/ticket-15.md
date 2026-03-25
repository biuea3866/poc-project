# [Ticket #15] Credit 도메인 — 크레딧 원장

## 개요
- TDD 참조: tdd.md 섹션 4.1.5, 4.2 (domain/credit/), 4.5, 8.1
- 선행 티켓: #8
- 크기: L

## 작업 내용

### 변경 사항

1. **CreditType enum**
   - `SMS`: SMS 발송 크레딧
   - `AI_EVALUATION`: AI 서류평가 크레딧
   - 확장 가능 설계: 새로운 크레딧 유형 추가 시 enum 값만 추가

2. **CreditTransactionType enum**
   - `CHARGE`: 충전 (주문 결제로 인한 크레딧 지급)
   - `USE`: 사용 (기능 사용으로 인한 차감)
   - `REFUND`: 환불 (결제 취소로 인한 크레딧 복원)
   - `EXPIRE`: 만료 (유효기간 도래로 인한 자동 차감)
   - `GRANT`: 무상 지급 (프로모션, 보상 등)

3. **CreditBalance entity 구현**
   - `workspaceId`: 워크스페이스 ID
   - `creditType`: CreditType
   - `balance`: 현재 잔액 (INT, 0 이상)
   - `updatedAt`: 최종 업데이트 시각
   - `version`: Optimistic Lock용 (동시 차감 경합 방지)
   - UNIQUE 제약: (workspace_id, credit_type) — 워크스페이스당 크레딧 타입별 1개 잔액 레코드

4. **CreditLedger entity 구현 (append-only)**
   - `workspaceId`: 워크스페이스 ID
   - `creditType`: CreditType
   - `transactionType`: CreditTransactionType
   - `amount`: 변동량 (양수: 충전/환불/지급, 음수: 사용/만료)
   - `balanceAfter`: 거래 후 잔액 (무결성 검증용)
   - `orderId`: 연관 주문 ID (충전/환불 시, nullable)
   - `description`: 거래 설명
   - `expiredAt`: 만료일 (CHARGE 시 설정, nullable)
   - `createdAt`: 거래 생성 시각
   - **append-only**: UPDATE/DELETE 불가, INSERT만 허용

5. **CreditRepository 구현**
   - `CreditBalanceRepository`:
     - `findByWorkspaceIdAndCreditType(workspaceId: Int, creditType: CreditType): CreditBalance?`
     - `findByWorkspaceId(workspaceId: Int): List<CreditBalance>`
   - `CreditLedgerRepository`:
     - `findByWorkspaceIdAndCreditTypeOrderByCreatedAtDesc(workspaceId: Int, creditType: CreditType): List<CreditLedger>`
     - `findByOrderId(orderId: Long): List<CreditLedger>`
     - `findChargeEntriesExpiredBefore(expiredAt: LocalDateTime): List<CreditLedger>` (만료 스케줄러용)

6. **CreditService 구현**
   - `charge(order: Order)`:
     - Order의 product metadata에서 credit_amount 추출
     - CreditLedger INSERT (type=CHARGE, amount=+N, expiredAt 설정)
     - CreditBalance UPDATE (balance += N) with Optimistic Lock
     - balanceAfter 검증: 실제 balance와 일치하는지 확인
   - `use(workspaceId: Int, creditType: CreditType, amount: Int, description: String)`:
     - CreditBalance 조회 → 잔액 >= amount 검증
     - CreditBalance UPDATE (balance -= amount) with Optimistic Lock
     - CreditLedger INSERT (type=USE, amount=-N)
     - 잔액 부족 시 `InsufficientCreditException`
   - `refund(order: Order)`:
     - 해당 주문의 CHARGE 원장 조회
     - CreditLedger INSERT (type=REFUND, amount=-N, 원래 충전량만큼 차감)
     - CreditBalance UPDATE (balance -= N)
     - 잔액이 0 미만이 되면 0으로 보정 (이미 사용한 분)
   - `getBalance(workspaceId: Int, creditType: CreditType): Int`:
     - CreditBalance 조회 → balance 반환
     - 레코드 없으면 0 반환
   - `getBalances(workspaceId: Int): Map<CreditType, Int>`:
     - 워크스페이스의 모든 크레딧 타입별 잔액
   - `expireCredits(ledgerEntry: CreditLedger, remainingAmount: Int)`:
     - 만료 대상 CHARGE 건의 잔여 크레딧 계산 후 차감
     - CreditLedger INSERT (type=EXPIRE, amount=-remainingAmount)
     - CreditBalance UPDATE (balance -= remainingAmount)

7. **Optimistic Lock 동시성 처리**
   - CreditBalance에 `@Version` 필드 사용
   - 동시 차감 시 `OptimisticLockException` 발생 → 재시도 (최대 3회)
   - 재시도 로직: `@Retryable` 또는 수동 retry loop

8. **잔액 정합성 보장**
   - CreditLedger의 `balance_after` 값이 CreditBalance.balance와 일치해야 함
   - 매 거래마다: `balanceAfter = 현재 balance + amount`로 계산 후 저장
   - 불일치 감지 시 `CreditBalanceInconsistencyException` (CRITICAL 로그)
   - 정합성 검증 유틸: CreditLedger의 running total 합산 == CreditBalance.balance

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | domain | domain/credit/CreditType.kt | 신규 |
| greeting_payment-server | domain | domain/credit/CreditTransactionType.kt | 신규 |
| greeting_payment-server | domain | domain/credit/CreditBalance.kt | 신규 |
| greeting_payment-server | domain | domain/credit/CreditLedger.kt | 신규 |
| greeting_payment-server | domain | domain/credit/exception/InsufficientCreditException.kt | 신규 |
| greeting_payment-server | domain | domain/credit/exception/CreditBalanceInconsistencyException.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/CreditBalanceRepository.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/CreditLedgerRepository.kt | 신규 |
| greeting_payment-server | application | application/CreditService.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T15-01 | 크레딧 충전 성공 | Order(CONSUMABLE, SMS_PACK_1000) | charge(order) | CreditBalance.balance += 1000, CreditLedger(CHARGE, +1000) |
| T15-02 | 크레딧 사용 성공 | balance=1000, 사용량=10 | use(ws1, SMS, 10, "SMS 발송") | balance=990, CreditLedger(USE, -10) |
| T15-03 | 크레딧 환불 성공 | CHARGE 1000건 존재 | refund(order) | balance -= 1000, CreditLedger(REFUND, -1000) |
| T15-04 | 잔액 조회 | balance=500 | getBalance(ws1, SMS) | 500 |
| T15-05 | 복수 크레딧 타입 잔액 | SMS=1000, AI_EVALUATION=50 | getBalances(ws1) | {SMS: 1000, AI_EVALUATION: 50} |
| T15-06 | balanceAfter 정합성 | balance=1000 → charge(+500) | charge() | CreditLedger.balanceAfter = 1500, CreditBalance.balance = 1500 |
| T15-07 | 최초 충전 (CreditBalance 없음) | ws1에 CreditBalance 레코드 없음 | charge(order) | CreditBalance 신규 생성(balance=1000) |
| T15-08 | 크레딧 만료 처리 | CHARGE(1000, expiredAt=어제) | expireCredits(entry, 800) | balance -= 800, CreditLedger(EXPIRE, -800) |
| T15-09 | 무상 지급 | 관리자가 프로모션 지급 | grantCredit(ws1, SMS, 100, "프로모션") | CreditLedger(GRANT, +100), balance += 100 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T15-E01 | 잔액 부족 | balance=5, 사용량=10 | use(ws1, SMS, 10) | InsufficientCreditException |
| T15-E02 | Optimistic Lock 충돌 | 동시에 두 건 차감 | use() 동시 호출 | 한 건 성공, 한 건 재시도 후 성공/실패 |
| T15-E03 | 환불 시 잔액 0 미만 보정 | 충전 1000 → 사용 800 → 환불 1000 | refund(order) | balance = 0 (음수 보정), CreditLedger(REFUND, -200) |
| T15-E04 | balanceAfter 불일치 감지 | 수동으로 balance 변조 | use() 시도 | CreditBalanceInconsistencyException (CRITICAL 로그) |
| T15-E05 | 잔액 레코드 없을 때 조회 | CreditBalance 레코드 없음 | getBalance(ws1, SMS) | 0 반환 |
| T15-E06 | amount=0 차감 시도 | amount=0 | use(ws1, SMS, 0) | 유효성 검증 실패 (amount > 0 필수) |
| T15-E07 | 음수 충전 시도 | amount=-100 | charge() | 유효성 검증 실패 |

## 기대 결과 (AC)
- [ ] CreditType enum이 SMS, AI_EVALUATION을 포함하며 확장 가능하다
- [ ] CreditTransactionType enum이 CHARGE, USE, REFUND, EXPIRE, GRANT를 포함한다
- [ ] CreditBalance가 (workspace_id, credit_type) UNIQUE이며 Optimistic Lock(version)을 사용한다
- [ ] CreditLedger가 append-only이며 balance_after 필드로 잔액 추적이 가능하다
- [ ] CreditService.charge()가 Order 기반으로 크레딧을 충전하고 원장 기록을 남긴다
- [ ] CreditService.use()가 잔액 부족 시 InsufficientCreditException을 던진다
- [ ] Optimistic Lock 충돌 시 재시도 로직이 동작한다 (최대 3회)
- [ ] balance_after 값이 CreditBalance.balance와 항상 일치하며, 불일치 시 CRITICAL 로그가 출력된다
- [ ] 환불 시 이미 사용한 크레딧은 잔액 0으로 보정된다 (음수 방지)
- [ ] 단위 테스트 커버리지 80% 이상
