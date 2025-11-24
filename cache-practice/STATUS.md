# Cache Practice 프로젝트 현재 상태

**마지막 업데이트**: 2025-11-23 17:43

## 완료된 작업 ✅

### 1. 프로젝트 설정
- ✅ Spring Boot + Kotlin 프로젝트 구조 생성
- ✅ build.gradle.kts 설정 (Caffeine, MySQL, JPA, Micrometer)
- ✅ application.yml 설정 (MySQL 포트: 3308)
- ✅ Docker Compose 설정 (MySQL, Prometheus, Grafana)

### 2. 도메인 구현
- ✅ Order 엔티티 (주문)
- ✅ OrderItem 엔티티 (주문 항목)
- ✅ OrderRepository (JPA Repository)
- ✅ DataSeeder (100만 건 데이터 시드)

### 3. 캐시 전략 구현
- ✅ CacheConfig (Caffeine 설정, TTL 60초)
- ✅ LazyLoadingOrderService (점진적 캐시 구축)
- ✅ EagerLoadingOrderService (부트 시 전체 로드)

### 4. API 엔드포인트
- ✅ GET /api/orders/lazy/{id} - Lazy Loading 캐시 사용
- ✅ GET /api/orders/lazy/no-cache/{id} - 캐시 미사용
- ✅ GET /api/orders/eager/{id} - Eager Loading 캐시 사용
- ✅ GET /api/orders/eager/no-cache/{id} - 캐시 미사용
- ✅ GET /actuator/prometheus - Prometheus 메트릭
- ✅ GET /actuator/health - 헬스 체크

### 5. 인프라
- ✅ Docker Compose 실행 중
  - MySQL (포트 3308)
  - Prometheus (포트 9090)
  - Grafana (포트 3000, admin/admin)
- ✅ Prometheus 설정 (prometheus.yml)
- ✅ Grafana 데이터소스 설정
- ✅ Grafana 대시보드 JSON

### 6. 부하 테스트
- ✅ k6 스크립트 생성
  - test-no-cache.js
  - test-lazy-cache.js
  - test-eager-cache.js

### 7. 문서
- ✅ README.md - 전체 가이드
- ✅ GRAFANA_GUIDE.md - Grafana 사용법 상세 가이드
- ✅ PERFORMANCE_REPORT.md - 성능 리포트 템플릿
- ✅ history.md - 변경 이력
- ✅ local_cache_context.md - 요구사항

## 진행 중인 작업 🔄

### 데이터베이스 시드
- **상태**: 진행 중 (백그라운드)
- **진행률**: 210,000 / 1,000,000 건 (21%)
- **예상 완료 시간**: 약 50분 후 (총 60분 소요 예상)
- **진행 속도**: 약 10,000건 / 6초

**로그 확인**:
```bash
tail -f seed-log.txt
```

## 대기 중인 작업 ⏳

### 1. 애플리케이션 실행
- 데이터 시드 완료 후 일반 모드로 애플리케이션 실행
- Eager Loading 서비스가 자동으로 캐시 로드

### 2. k6 부하 테스트 실행
- 시나리오 1: 캐시 없음 (Baseline)
- 시나리오 2: Lazy Loading
- 시나리오 3: Eager Loading (2분, TTL 만료 관찰)

### 3. 메트릭 수집
- Grafana 대시보드에서 각 시나리오별 메트릭 캡처
- CPU, 메모리, 응답시간, 캐시 히트율 등

### 4. 성능 리포트 작성
- PERFORMANCE_REPORT.md에 실제 측정 데이터 입력
- 스크린샷 추가
- 분석 및 권장사항 작성

## 다음 단계

### 데이터 시드 완료 후 (자동)
1. 애플리케이션을 일반 모드로 재시작
2. Eager Loading으로 캐시 로드 (예상 10-20분)
3. 준비 완료 확인

### 부하 테스트 실행 (수동)
```bash
# 1. 캐시 없음 테스트 (2분)
k6 run k6/test-no-cache.js

# 2. Lazy Loading 테스트 (2분)
k6 run k6/test-lazy-cache.js

# 3. Eager Loading 테스트 (2분, TTL 관찰)
k6 run k6/test-eager-cache.js
```

### Grafana 확인
1. http://localhost:3000 접속 (admin/admin)
2. GRAFANA_GUIDE.md 참고하여 메트릭 확인
3. 각 테스트별 스크린샷 캡처

## 중요 URL

| 서비스 | URL | 인증 |
|--------|-----|------|
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| API (Lazy) | http://localhost:8080/api/orders/lazy/1 | - |
| API (Eager) | http://localhost:8080/api/orders/eager/1 | - |
| Metrics | http://localhost:8080/actuator/prometheus | - |
| Health | http://localhost:8080/actuator/health | - |

## 예상 타임라인

```
[완료] 17:00-17:20  프로젝트 구현
[완료] 17:20-17:40  Docker 인프라 시작
[진행중] 17:40-18:40  데이터 시드 (100만 건)
[대기] 18:40-19:00  애플리케이션 재시작 & 캐시 로드
[대기] 19:00-19:10  부하 테스트 실행 (3개 시나리오)
[대기] 19:10-19:30  메트릭 수집 및 분석
[대기] 19:30-20:00  성능 리포트 작성
```

## 문제 해결

### 데이터 시드가 중단된 경우
```bash
# 진행 상황 확인
docker exec -it cache-practice-mysql mysql -uroot -ppassword -e "SELECT COUNT(*) FROM cache_practice.orders;"

# 재시작
./gradlew bootRun --args='--spring.profiles.active=seed'
```

### Docker 컨테이너 재시작
```bash
cd cache-practice
docker-compose down
docker-compose up -d
```

### 로그 확인
```bash
# 시드 로그
tail -f seed-log.txt

# Docker 로그
docker logs cache-practice-mysql
docker logs cache-practice-prometheus
docker logs cache-practice-grafana
```

---

**참고**:
- 데이터 시드는 백그라운드에서 실행 중이므로 터미널을 닫아도 계속 진행됩니다
- 진행 상황은 `tail -f seed-log.txt`로 실시간 확인 가능합니다
- 모든 문서는 cache-practice 디렉토리에 있습니다
