# Cache Practice 프로젝트 변경 이력

## 2025-11-23

### 요구사항 #1: 캐시 전략 수정 및 성능 테스트
**요청 내용**:
- 100만 건 DB 데이터 중 일부만 캐싱하는 전략 구현
- maximumSize 제한으로 메모리 효율적인 캐싱
- 실제 테스트 및 성능 분석

**작업 완료**:
1. ✅ **캐시 설정 최적화**
   - `maximumSize=10,000` (전체의 1%)
   - `expireAfterWrite=60s`
   - Window TinyLFU 알고리즘

2. ✅ **Docker 인프라 구축**
   - MySQL: 3308 포트
   - Prometheus: 9090 포트
   - Grafana: 3000 포트
   - MySQL Exporter 정상 작동

3. ✅ **데이터 시딩 완료**
   - 1,000,000건 Order 데이터 생성

4. ✅ **k6 부하 테스트 실행**
   - No Cache baseline
   - Lazy Loading (랜덤 패턴)
   - Lazy Loading (Hot-key 패턴) ⭐

**완료일**: 2025-11-23

---

### 요구사항 #2: 성능 분석 및 문제 해결
**요청 내용**:
1. 왜 로컬 캐시 성능 향상이 미비한가?
2. MySQL이 Grafana에 안 잡히는 이유?
3. Cache Penetration 분석
4. JVM/MySQL CPU/Memory 사용량 분석

**문제 발견 및 해결**:

#### 1. 캐시 성능 미비 원인 분석 ✅
**문제**: 캐시 히트율 0.92% (거의 작동하지 않음)
**원인**: k6 테스트가 완전 랜덤 패턴 사용
```javascript
// 문제의 코드
const orderId = Math.floor(Math.random() * 1000000) + 1;
```

**해결**: Hot-key 패턴 적용 (Zipf 분포)
```javascript
function getHotKeyOrderId() {
  if (Math.random() < 0.8) {
    return Math.floor(Math.random() * 10000) + 1; // 80% hot keys
  } else {
    return Math.floor(Math.random() * 990000) + 10001; // 20% cold keys
  }
}
```

**결과**:
- 캐시 히트율: 0.92% → **52.6%** (57배 증가!)
- 평균 응답 시간: 7.27ms → **2.83ms** (61% 개선!)
- MySQL 쿼리 부하: **50% 감소**

#### 2. MySQL Exporter 문제 해결 ✅
**문제**: MySQL Exporter 계속 재시작
```
ERROR: "no user specified in section or parent"
```

**해결**: docker-compose.yml 수정
```yaml
mysql-exporter:
  command:
    - '--mysqld.username=root'
    - '--mysqld.address=mysql:3306'
  environment:
    - MYSQLD_EXPORTER_PASSWORD=password
```

**결과**: ✅ MySQL 메트릭 정상 수집

#### 3. Cache Penetration 분석 완료 ✅
**측정 결과**:
- 테스트 기간: 2분 (TTL 60초 × 2 주기)
- 캐시 히트율: 52.6% (안정적 유지)
- P95 응답 시간: 7.42ms (일정 유지)
- **위험도**: 낮음

**이유**:
- 점진적 만료 (동시 만료 없음)
- Hot-key는 TTL 내 재접근
- Window TinyLFU가 인기 데이터 유지

#### 4. JVM/MySQL 리소스 분석 완료 ✅
**JVM 메트릭**:
- Heap 사용량: ~150MB / 2GB (7.5%)
- 활성 스레드: 24 (blocked: 0)
- 캐시 메모리 오버헤드: ~15MB

**MySQL 메트릭**:
- 초당 쿼리: 516 → 258 (50% 감소!)
- 연결 수: 7
- InnoDB 버퍼 풀 히트율: 99.5%

**완료일**: 2025-11-23

---

### 요구사항 #3: 문서 업데이트
**요청 내용**:
- PERFORMANCE_REPORT.md 업데이트
- COMPREHENSIVE_ANALYSIS.md 생성
- history.md 업데이트

**생성된 파일**:
1. ✅ **PERFORMANCE_REPORT.md** (최종 업데이트)
   - 3가지 테스트 시나리오 비교
   - Hot-key 패턴 결과 포함
   - JVM/MySQL 메트릭 포함
   - Cache Penetration 분석 포함

2. ✅ **COMPREHENSIVE_ANALYSIS.md** (신규 생성)
   - 4가지 문제에 대한 상세 분석
   - 랜덤 vs Hot-key 패턴 비교
   - 문제 해결 과정 상세 기록

3. ✅ **k6/test-lazy-cache-hotkey.js** (신규 생성)
   - Zipf 분포 시뮬레이션
   - 실제 서비스 패턴 반영

**완료일**: 2025-11-23

---

## 핵심 성과 요약

### 📊 최종 성능 비교

| 메트릭 | No Cache | Lazy (랜덤) | Lazy (Hot-key) |
|--------|----------|-------------|----------------|
| 평균 응답 시간 | 8.32ms | 7.27ms | **2.83ms** ✅ |
| 캐시 히트율 | 0% | 0.92% | **52.6%** ✅ |
| MySQL 부하 | 516 q/s | 516 q/s | **258 q/s** ✅ |
| 메모리 오버헤드 | - | +15MB | +15MB |

### 🎯 핵심 교훈

1. **"캐시가 안 되는 것이 아니라, 테스트가 잘못되었다"**
   - Uniform 분포: 0.92% 히트율 → 12.6% 개선
   - Zipf 분포: 52.6% 히트율 → **66% 개선**

2. **적은 캐시로 큰 효과**
   - 전체의 1%만 캐싱해도 52.6% 히트율
   - MySQL 부하 50% 감소
   - 메모리 오버헤드 단 15MB

3. **Window TinyLFU 효과적**
   - Hot-key 자동 식별
   - TTL 만료에도 안정적

---

## 변경 이력 템플릿

### YYYY-MM-DD
**요구사항 #N**: [요구사항 제목]
**요청 내용**: [상세 설명]
**작업 내용**: [수행한 작업]
**완료일**: [완료 날짜]
