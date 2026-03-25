# [Ticket #4a-1] Feature Flag 설정

## 개요
- TDD 참조: tdd.md 섹션 5.3 (무중단 마이그레이션)
- 선행 티켓: #2, #3
- 크기: S

## 작업 내용

### 변경 사항

듀얼라이트/듀얼리드 전체를 제어하는 Feature Flag 인프라를 구축한다.

#### Feature Flag 목록

| Flag | 기본값 | 용도 |
|------|--------|------|
| `dual-write-payment-log` | false | PaymentLogsOnGroup 듀얼라이트 |
| `dual-write-message-point-log` | false | MessagePointLogsOnWorkspace 듀얼라이트 |
| `dual-write-charge-log` | false | MessagePointChargeLogsOnWorkspace 듀얼라이트 |
| `read-from-mysql-payment` | false | 결제 이력 읽기 소스 전환 |
| `read-from-mysql-credit` | false | 크레딧 이력 읽기 소스 전환 |

#### 코드 예시

```kotlin
@Component
@ConfigurationProperties(prefix = "feature.dual-write")
class DualWriteFeatureFlag {
    var paymentLog: Boolean = false
    var messagePointLog: Boolean = false
    var chargeLog: Boolean = false
    var readFromMysqlPayment: Boolean = false
    var readFromMysqlCredit: Boolean = false
}
```

```yaml
# application.yml
feature:
  dual-write:
    payment-log: false
    message-point-log: false
    charge-log: false
    read-from-mysql-payment: false
    read-from-mysql-credit: false
```

#### 모니터링 메트릭 공통 Bean

```kotlin
@Configuration
class DualWriteMetricsConfig(private val meterRegistry: MeterRegistry) {

    @Bean
    fun dualWriteMetrics(): DualWriteMetrics = DualWriteMetrics(meterRegistry)
}

class DualWriteMetrics(private val registry: MeterRegistry) {

    fun successCounter(collection: String): Counter =
        registry.counter("dual_write_success_count", "collection", collection)

    fun failureCounter(collection: String): Counter =
        registry.counter("dual_write_failure_count", "collection", collection)

    fun latencyTimer(collection: String): Timer =
        registry.timer("dual_write_latency_ms", "collection", collection)
}
```

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | config | DualWriteFeatureFlag.kt | 신규 |
| greeting_payment-server | config | DualWriteMetricsConfig.kt | 신규 |
| greeting_payment-server | config | DualWriteMetrics.kt | 신규 |
| greeting_payment-server | resources | application.yml | 수정 (feature.dual-write 섹션 추가) |
| greeting_payment-server | resources | application-prod.yml | 수정 (동일) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-01 | Flag 기본값 false | yml 미설정 | DualWriteFeatureFlag 주입 | 전 항목 false |
| TC-02 | Flag yml 설정 반영 | payment-log: true | DualWriteFeatureFlag 주입 | paymentLog = true |
| TC-03 | 메트릭 Bean 생성 | 앱 기동 | DualWriteMetrics 주입 | counter/timer 정상 생성 |

## 기대 결과 (AC)
- [ ] 5개 Feature Flag가 application.yml로 제어 가능
- [ ] 런타임 Flag 변경 없이 배포로 on/off 전환 (향후 동적 전환 확장 가능)
- [ ] DualWriteMetrics Bean으로 메트릭 카운터/타이머 생성 가능
