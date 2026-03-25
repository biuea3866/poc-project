# [Ticket #16] 크레딧 만료 스케줄러

## 개요
- TDD 참조: tdd.md 섹션 4.1.5 (credit_ledger.expired_at), 8.4
- 선행 티켓: #15
- 크기: S

## 작업 내용

### 변경 사항

1. **CreditExpirationScheduler 구현**
   - `@Scheduled` CRON: `0 0 1 * * *` (매일 01:00 KST)
   - 갱신 스케줄러(#14, 00:00)와 1시간 간격을 두어 부하 분산
   - Spring `@Component` + `@Scheduled` 사용

2. **만료 대상 조회**
   - 조건: `credit_ledger.expired_at <= now AND transaction_type = 'CHARGE' AND 잔여 크레딧 > 0`
   - 잔여 크레딧 계산: 해당 CHARGE 건의 `amount` - (해당 건에서 이미 USE/EXPIRE로 차감된 합계)
   - 구현 방식 (선택 1 — 간단 방식):
     - CHARGE 건의 `amount`를 원본으로 보고, 동일 orderId의 EXPIRE 거래 합산으로 이미 만료 처리된 양 계산
     - `remaining = charge.amount - SUM(expire.amount WHERE orderId = charge.orderId)`
   - 구현 방식 (선택 2 — 정밀 방식):
     - CHARGE 건에 `remaining` 컬럼 추가 (향후 고려, 이 티켓에서는 선택 1)

3. **만료 처리 로직**
   ```
   for each expiredChargeEntry:
     1. 잔여 크레딧(remaining) 계산
     2. if remaining <= 0: skip (이미 전부 사용/만료됨)
     3. CreditLedger INSERT (type=EXPIRE, amount=-remaining, orderId=charge.orderId)
     4. CreditBalance UPDATE (balance -= remaining) with Optimistic Lock
     5. balance < 0 방지: min(remaining, currentBalance)로 보정
   ```

4. **워크스페이스 관리자 알림 (optional)**
   - 만료 처리된 건에 대해 알림 발송 (이 티켓에서는 TODO 주석)
   - 향후 구현: 만료 예정 알림 (D-7, D-1) 별도 티켓
   - 로그에 만료 처리 내역 기록 (workspaceId, creditType, expiredAmount)

5. **배치 사이즈 제한**
   - 한 번에 처리할 최대 건수: 설정값 (기본 500건)
   - `@Value("${credit.expiration.batch-size:500}")`
   - 만료 처리는 PG 호출이 없으므로 갱신보다 큰 배치 가능

6. **에러 핸들링**
   - 개별 건 실패가 전체를 중단하지 않음
   - 실패 건 WARN 로그 + 다음 건 계속 처리
   - Optimistic Lock 충돌 시 해당 건 스킵 (다음 날 재처리)

7. **로깅**
   - 스케줄러 시작/종료 INFO 로그
   - 처리 요약: 만료 대상 N건, 처리 완료 N건, 스킵 N건, 실패 N건
   - 총 만료 크레딧 수량 로그

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | infrastructure | infrastructure/scheduler/CreditExpirationScheduler.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/CreditLedgerRepository.kt | 수정 (만료 조회 쿼리 추가) |
| greeting_payment-server | application | application/CreditService.kt | 수정 (expireCredits 메서드 활용) |
| greeting_payment-server | config | application.yml | 수정 (batch-size 설정 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T16-01 | 만료 처리 — 전액 잔여 | CHARGE(1000, expiredAt=어제), 사용 0건 | 스케줄러 실행 | EXPIRE(-1000), balance -= 1000 |
| T16-02 | 만료 처리 — 부분 잔여 | CHARGE(1000, expiredAt=어제), USE(-300) | 스케줄러 실행 | EXPIRE(-700), balance -= 700 |
| T16-03 | 만료 대상 0건 | 모든 CHARGE의 expiredAt > now | 스케줄러 실행 | 처리 없음, 정상 종료 |
| T16-04 | 이미 전부 사용된 건 스킵 | CHARGE(1000), USE(-1000) | 스케줄러 실행 | remaining=0, 스킵 |
| T16-05 | 복수 건 만료 | 3건 만료 대상 | 스케줄러 실행 | 3건 모두 EXPIRE 처리 |
| T16-06 | 처리 요약 로그 | 대상 5건, 처리 4건, 스킵 1건 | 스케줄러 완료 | INFO 로그에 요약 출력 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T16-E01 | balance보다 remaining이 큰 경우 | balance=500, remaining=700 (다른 건에서 이미 차감) | 만료 처리 | min(700, 500) = 500만 만료, balance=0 |
| T16-E02 | Optimistic Lock 충돌 | 만료 처리 중 동시 USE 발생 | 만료 처리 | 해당 건 스킵, 다음 날 재처리 |
| T16-E03 | 개별 건 실패 — 전체 중단 안 됨 | 2번째 건 예외 | 스케줄러 실행 | 나머지 건 정상 처리 |
| T16-E04 | 배치 사이즈 초과 | 600건 대상, batch-size=500 | 스케줄러 실행 | 500건만 처리, 100건은 다음 날 |
| T16-E05 | expiredAt이 NULL인 CHARGE | CHARGE(expiredAt=NULL) | 스케줄러 실행 | 만료 대상에서 제외 (무기한) |

## 기대 결과 (AC)
- [ ] 스케줄러가 매일 01:00 KST에 실행되어 만료 대상 CHARGE 건을 조회한다
- [ ] expired_at <= now이고 잔여 크레딧이 있는 CHARGE 건만 만료 대상이다
- [ ] 만료 시 CreditLedger에 EXPIRE 거래가 INSERT되고 CreditBalance가 차감된다
- [ ] balance가 음수가 되지 않도록 min(remaining, currentBalance)로 보정한다
- [ ] 이미 전부 사용된 CHARGE 건(remaining=0)은 스킵된다
- [ ] 개별 건 실패가 전체 배치를 중단하지 않는다
- [ ] 처리 결과 요약 로그가 출력된다 (대상/처리/스킵/실패 건수)
- [ ] 단위 테스트 커버리지 90% 이상
