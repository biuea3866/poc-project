# Grafana 메트릭 확인 가이드

## 개요
이 가이드는 cache-practice 프로젝트의 성능 메트릭을 Grafana에서 확인하는 방법을 설명합니다.

## 1. Grafana 접속

### 초기 접속
1. 브라우저에서 `http://localhost:3000` 접속
2. 로그인 정보 입력:
   - **Username**: `admin`
   - **Password**: `admin`
3. 첫 로그인 시 비밀번호 변경 요청이 나타나면 "Skip" 또는 새 비밀번호 설정

## 2. Prometheus 데이터소스 확인

### 데이터소스 설정 확인
1. 좌측 메뉴에서 **Configuration (⚙️)** > **Data Sources** 클릭
2. "Prometheus" 데이터소스가 자동으로 설정되어 있는지 확인
3. "Prometheus" 클릭 후 하단의 **"Save & Test"** 버튼 클릭
4. "Data source is working" 메시지 확인

**문제 발생 시**:
- URL이 `http://prometheus:9090`으로 설정되어 있는지 확인
- Docker 네트워크에서 Prometheus 컨테이너가 실행 중인지 확인

## 3. 대시보드 생성

### 방법 1: 새 대시보드 직접 생성

1. 좌측 메뉴에서 **"+"** > **Dashboard** 클릭
2. **"Add new panel"** 클릭
3. 아래의 주요 메트릭을 추가

### 방법 2: Import Dashboard (권장)

1. 좌측 메뉴에서 **Dashboards** > **Import** 클릭
2. Spring Boot 공식 대시보드 ID 입력: `11378` 또는 `12900`
3. **Load** 클릭
4. Prometheus 데이터소스 선택 후 **Import**

## 4. 주요 메트릭 확인

### 4.1 애플리케이션 CPU 사용률

**Prometheus Query**:
```promql
process_cpu_usage{application="cache-practice"}
```

**설명**:
- 애플리케이션의 CPU 사용률을 0~1 범위로 표시 (0.5 = 50%)
- 캐시 사용 전후 CPU 사용률 변화 관찰

**패널 설정**:
- **Visualization**: Time series
- **Unit**: Percent (0.0-1.0)
- **Threshold**: 0.7 (Yellow), 0.9 (Red)

### 4.2 JVM 메모리 사용량

**Prometheus Query**:
```promql
jvm_memory_used_bytes{application="cache-practice"}
```

**더 상세한 쿼리** (Heap 메모리만):
```promql
jvm_memory_used_bytes{application="cache-practice",area="heap"}
```

**설명**:
- JVM Heap 메모리 사용량 (바이트)
- Eager Loading 시 메모리 증가 패턴 관찰
- Lazy Loading 시 점진적 메모리 증가 관찰

**패널 설정**:
- **Visualization**: Time series
- **Unit**: bytes (IEC)
- **Legend**: {{area}} - {{id}}

### 4.3 캐시 히트/미스율

**캐시 히트율**:
```promql
rate(cache_gets_total{result="hit"}[1m]) / rate(cache_gets_total[1m])
```

**캐시 미스율**:
```promql
rate(cache_gets_total{result="miss"}[1m]) / rate(cache_gets_total[1m])
```

**설명**:
- 1분 동안의 캐시 히트/미스 비율
- Lazy Loading: 초기에 낮다가 점차 증가
- Eager Loading: 초기부터 높고 TTL 만료 시 급락

**패널 설정**:
- **Visualization**: Time series
- **Unit**: Percent (0.0-1.0)
- **Legend**: Hit Rate / Miss Rate

### 4.4 HTTP 요청 응답 시간

**평균 응답 시간**:
```promql
rate(http_server_requests_seconds_sum{application="cache-practice"}[1m]) / rate(http_server_requests_seconds_count{application="cache-practice"}[1m])
```

**P95 응답 시간**:
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application="cache-practice"}[1m]))
```

**P99 응답 시간**:
```promql
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{application="cache-practice"}[1m]))
```

**설명**:
- API 엔드포인트별 응답 시간
- 캐시 적용 시 응답 시간 단축 효과 확인
- Cache penetration 발생 시 응답 시간 증가 관찰

**패널 설정**:
- **Visualization**: Time series
- **Unit**: seconds (s)
- **Legend**: {{uri}} - {{method}}

### 4.5 HTTP 요청 처리량 (Throughput)

**초당 요청 수**:
```promql
rate(http_server_requests_seconds_count{application="cache-practice"}[1m])
```

**URI별 요청 수**:
```promql
sum by(uri) (rate(http_server_requests_seconds_count{application="cache-practice"}[1m]))
```

**설명**:
- 초당 처리하는 요청 수 (requests/second)
- 캐시 적용 시 처리량 증가 확인

**패널 설정**:
- **Visualization**: Time series
- **Unit**: requests/sec
- **Legend**: {{uri}}

### 4.6 데이터베이스 커넥션 풀

**활성 커넥션 수**:
```promql
hikaricp_connections_active{application="cache-practice"}
```

**유휴 커넥션 수**:
```promql
hikaricp_connections_idle{application="cache-practice"}
```

**대기 중인 스레드 수**:
```promql
hikaricp_connections_pending{application="cache-practice"}
```

**설명**:
- 캐시 사용 시 DB 커넥션 사용량 감소 확인
- Cache penetration 시 커넥션 사용량 급증 관찰

**패널 설정**:
- **Visualization**: Time series
- **Legend**: Active / Idle / Pending

### 4.7 JVM GC (Garbage Collection)

**GC 발생 빈도**:
```promql
rate(jvm_gc_pause_seconds_count{application="cache-practice"}[1m])
```

**GC 소요 시간**:
```promql
rate(jvm_gc_pause_seconds_sum{application="cache-practice"}[1m])
```

**설명**:
- Eager Loading 시 메모리 증가로 인한 GC 빈도 변화
- GC로 인한 애플리케이션 중단 시간 관찰

**패널 설정**:
- **Visualization**: Time series
- **Unit**: seconds (s)
- **Legend**: {{action}} - {{cause}}

## 5. 대시보드 레이아웃 구성

### 권장 패널 배치

```
┌─────────────────────────────────────────────────────┐
│  Dashboard: Cache Practice Performance Metrics     │
├──────────────────┬──────────────────┬───────────────┤
│  CPU Usage       │  Memory Usage    │  Throughput   │
│  (Time Series)   │  (Time Series)   │  (Time Series)│
├──────────────────┴──────────────────┴───────────────┤
│  HTTP Response Time (P50, P95, P99)                 │
│  (Time Series with multiple metrics)                │
├──────────────────┬──────────────────────────────────┤
│  Cache Hit Rate  │  Database Connections            │
│  (Time Series)   │  (Time Series)                   │
├──────────────────┴──────────────────────────────────┤
│  GC Metrics                                          │
│  (Time Series with Count and Duration)              │
└─────────────────────────────────────────────────────┘
```

## 6. 테스트 시나리오별 관찰 포인트

### 시나리오 1: 캐시 없음 (Baseline)

**관찰 포인트**:
- ✅ CPU 사용률: 높게 유지
- ✅ 응답 시간: 상대적으로 긴 시간 (예: 50-200ms)
- ✅ DB 커넥션: 활성 커넥션 수 높음
- ✅ 처리량: 상대적으로 낮음

### 시나리오 2: Lazy Loading 캐시

**관찰 포인트**:
- ✅ CPU 사용률: 시간이 지나면서 점차 감소
- ✅ 메모리: 점진적으로 증가
- ✅ 캐시 히트율: 0%에서 시작하여 점차 증가 (90%+)
- ✅ 응답 시간: 점차 단축 (예: 50ms → 5ms)
- ✅ DB 커넥션: 시간이 지나면서 감소

**타임라인**:
1. **0-30초**: 낮은 히트율, 높은 응답 시간
2. **30초-1분**: 히트율 증가, 응답 시간 개선
3. **1분 이후**: 안정적인 성능

### 시나리오 3: Eager Loading 캐시 (TTL 1분)

**관찰 포인트**:
- ✅ 애플리케이션 시작 시: 메모리 급증 (캐시 로드)
- ✅ 0-1분: 높은 캐시 히트율 (~100%), 빠른 응답 시간
- ✅ 1분 시점 (TTL 만료):
  - 캐시 히트율 급락
  - 응답 시간 급증
  - CPU 사용률 급증
  - DB 커넥션 급증
  - 처리량 감소
- ✅ 1분 이후: 캐시 재워밍 시작, 점진적 회복

**중요 시점**:
- **T+0**: 애플리케이션 시작, 전체 캐시 로드
- **T+1분**: TTL 만료, Cache Penetration 발생
- **T+1분 30초**: 캐시 재워밍 완료, 성능 회복

## 7. 비교 분석을 위한 대시보드 활용

### 시간 범위 설정
1. 우측 상단의 시간 선택기 클릭
2. **"Last 15 minutes"** 또는 **"Last 30 minutes"** 선택
3. 테스트 실행 시간에 맞춰 조정

### 주석(Annotation) 추가
1. 패널에서 특정 시점 클릭
2. **"Add annotation"** 클릭
3. 테스트 시작/종료 시점 표시:
   - "Test Started: No Cache"
   - "Test Started: Lazy Loading"
   - "Test Started: Eager Loading"
   - "TTL Expired: Cache Penetration"

### 변수(Variables) 활용
대시보드 상단에 변수 추가하여 동적으로 필터링:
- **cache_type**: lazy, eager, none
- **uri**: API 엔드포인트별 필터링

## 8. 스크린샷 캡처

### 성능 리포트용 스크린샷 캡처 방법

1. 각 테스트 시나리오별로 대시보드 캡처:
   - `screenshots/no-cache.png`
   - `screenshots/lazy-loading.png`
   - `screenshots/eager-loading.png`

2. Grafana 내장 기능 사용:
   - 패널 우측 상단의 **[...]** 메뉴 클릭
   - **"Share"** > **"Link"** 또는 **"Snapshot"** 선택
   - URL 복사하여 PERFORMANCE_REPORT.md에 링크

3. 전체 대시보드 공유:
   - 우측 상단의 **공유 아이콘** 클릭
   - **"Export"** > **"Save to file"** 선택
   - JSON 파일로 저장

## 9. 문제 해결

### 메트릭이 표시되지 않는 경우

1. **Prometheus가 애플리케이션 메트릭을 수집하는지 확인**:
   - `http://localhost:9090` 접속
   - **Status** > **Targets** 확인
   - `spring-boot-app` 타겟이 **UP** 상태인지 확인

2. **애플리케이션 Actuator 엔드포인트 확인**:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```
   - 메트릭 데이터가 출력되는지 확인

3. **Grafana 데이터소스 재연결**:
   - Configuration > Data Sources > Prometheus
   - **"Save & Test"** 다시 클릭

### 대시보드가 비어있는 경우

1. 시간 범위 조정 (Last 5 minutes → Last 1 hour)
2. 쿼리 직접 테스트:
   - Explore 메뉴 이용
   - 간단한 쿼리 실행: `up`

### 캐시 메트릭이 보이지 않는 경우

1. Caffeine 캐시 통계가 활성화되어 있는지 확인:
   ```kotlin
   // CacheConfig.kt 에서 recordStats() 호출 확인
   .recordStats()
   ```

2. Spring Cache 메트릭 자동 설정 확인

## 10. 추가 리소스

### 유용한 Prometheus 쿼리 예제

**메모리 사용률 (백분율)**:
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

**캐시 크기**:
```promql
cache_size{cache="lazyOrderCache"}
```

**초당 에러율**:
```promql
rate(http_server_requests_seconds_count{status=~"5.."}[1m])
```

### Grafana 대시보드 갤러리
- Spring Boot 2.1+ Dashboard: https://grafana.com/grafana/dashboards/11378
- JVM Micrometer Dashboard: https://grafana.com/grafana/dashboards/4701

---

**작성일**: 2025-11-23
**버전**: 1.0
