# [Ticket #4c] Shadow Read 검증 + 읽기 전환 + MongoDB 쓰기 중단

## 개요
- TDD 참조: tdd.md 섹션 5.3 (Phase D, E, F)
- 선행 티켓: #4a, #4b(기존 #4 배치이관), #6
- 크기: M

## 작업 내용

### 변경 사항

듀얼라이트 + 배치이관 완료 후, 읽기를 MySQL로 전환하고, 최종적으로 MongoDB 쓰기를 중단한다.

#### Phase D: Shadow Read 검증
- 모든 읽기 지점에서 **MongoDB와 MySQL 양쪽 읽기 후 결과 비교**
- 불일치 시 알림 (Slack/로그) + 불일치 건수 메트릭
- 불일치율 0%가 7일 연속 유지되면 MySQL Primary Reader로 전환

```kotlin
// ShadowReadService.kt
fun <T> shadowRead(
    mongoRead: () -> T,
    mysqlRead: () -> T,
    entityName: String,
): T {
    val mongoResult = mongoRead()

    if (featureFlag.isEnabled("shadow-read-$entityName")) {
        try {
            val mysqlResult = mysqlRead()
            if (mongoResult != mysqlResult) {
                shadowReadMismatchCounter.increment(entityName)
                log.warn("Shadow read mismatch for $entityName: mongo=$mongoResult, mysql=$mysqlResult")
            }
        } catch (e: Exception) {
            log.warn("Shadow read MySQL failed for $entityName", e)
        }
    }

    return mongoResult  // 검증 단계에서는 여전히 MongoDB가 Primary
}
```

#### Phase E: 읽기 전환
- Feature flag `read-from-mysql-*`를 ON으로 전환
- DualReadService가 MySQL에서 읽기 시작
- MongoDB 읽기는 폴백으로만 유지 (MySQL 장애 시 자동 폴백)

#### Phase F: MongoDB 쓰기 중단
- Feature flag `dual-write-*`를 OFF로 전환
- DualWriteService가 MySQL에만 쓰기 (Single Write)
- 1~2주 모니터링 후 MongoDB 의존성 제거 (#26에서 처리)

#### 롤백 계획
- 각 Phase에서 문제 발생 시 Feature flag로 **5분 내 이전 단계로 롤백**
  - Phase D 문제 → Shadow Read OFF
  - Phase E 문제 → read-from-mysql OFF (MongoDB 읽기 복귀)
  - Phase F 문제 → dual-write ON (듀얼라이트 복귀)

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | domain | ShadowReadService.kt | 신규 |
| greeting_payment-server | domain | DualReadPaymentLogService.kt | 수정 (폴백 로직 추가) |
| greeting_payment-server | domain | DualReadCreditService.kt | 수정 (폴백 로직 추가) |
| greeting_payment-server | config | FeatureFlagConfig.kt | 수정 (shadow-read 플래그 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-01 | Shadow Read 일치 | 양쪽 동일 데이터 | 결제 이력 조회 | 불일치 카운터 0, MongoDB 결과 반환 |
| TC-02 | Shadow Read 불일치 탐지 | MongoDB에만 신규 데이터 | 결제 이력 조회 | 불일치 카운터 +1, 알림 발송 |
| TC-03 | MySQL Primary 읽기 전환 | read-from-mysql ON | 결제 이력 조회 | MySQL 결과 반환 |
| TC-04 | MySQL 장애 시 MongoDB 폴백 | read-from-mysql ON + MySQL 장애 | 결제 이력 조회 | MongoDB 폴백 결과 반환 |
| TC-05 | MongoDB 쓰기 중단 | dual-write OFF | 결제 기록 | MySQL만 저장 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-E01 | Shadow Read MySQL 타임아웃 | MySQL 응답 3초 초과 | Shadow Read | MongoDB 결과 반환, 타임아웃 로그 |
| TC-E02 | 읽기 전환 직후 롤백 | read-from-mysql ON → OFF | 플래그 변경 | 즉시 MongoDB 읽기 복귀 |

## 기대 결과 (AC)
- [ ] Shadow Read로 MongoDB/MySQL 데이터 일치율 실시간 모니터링 가능
- [ ] 불일치율 0% 달성 시 MySQL 읽기 전환 가능
- [ ] Feature flag로 각 단계 5분 내 롤백 가능
- [ ] MongoDB 쓰기 중단 후 MySQL 단독 운영 정상 동작
- [ ] 전 과정에서 서비스 중단 없음
