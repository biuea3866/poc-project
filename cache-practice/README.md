# Cache Practice Project

Spring Boot 로컬 캐시(Caffeine) 성능 비교 프로젝트

## 프로젝트 개요

이 프로젝트는 Caffeine 캐시를 활용한 두 가지 캐시 전략의 성능을 비교합니다:
- **Lazy Loading**: Write/Read 패턴으로 점진적으로 캐시 구축
- **Eager Loading**: 애플리케이션 부트 시 전체 데이터 캐시 로드 (TTL 1분)

## 기술 스택

- Kotlin + Spring Boot 3.2.0
- Caffeine Cache
- MySQL 8.0
- Prometheus + Grafana
- k6 (부하 테스트)

## 프로젝트 구조

```
cache-practice/
├── src/main/kotlin/com/example/cachepractice/
│   ├── domain/              # Order, OrderItem 엔티티
│   ├── service/             # Lazy/Eager 캐시 서비스
│   ├── controller/          # REST API 컨트롤러
│   └── config/              # 캐시 설정
├── docker/
│   ├── prometheus/          # Prometheus 설정
│   └── grafana/             # Grafana 대시보드
├── k6/                      # 부하 테스트 스크립트
└── docker-compose.yml
```

## 시작하기

### 1. 인프라 시작

```bash
cd cache-practice
docker-compose up -d
```

이 명령어는 다음을 시작합니다:
- MySQL (포트 3306)
- Prometheus (포트 9090)
- Grafana (포트 3000)

### 2. 데이터베이스 시드 (100만 건)

```bash
./gradlew bootRun --args='--spring.profiles.active=seed'
```

주의: 100만 건의 데이터를 생성하는 데 시간이 걸립니다 (약 10-30분).

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션이 시작되면 Eager Loading 서비스가 자동으로 모든 주문을 캐시에 로드합니다.

## API 엔드포인트

### Lazy Loading (점진적 캐시)
- `GET /api/orders/lazy/{id}` - 캐시 사용
- `GET /api/orders/lazy/no-cache/{id}` - 캐시 미사용

### Eager Loading (부트 시 로딩)
- `GET /api/orders/eager/{id}` - 캐시 사용 (TTL 1분)
- `GET /api/orders/eager/no-cache/{id}` - 캐시 미사용

### 모니터링
- `GET /actuator/prometheus` - Prometheus 메트릭
- `GET /actuator/health` - 헬스 체크

## 부하 테스트 실행

k6가 설치되어 있어야 합니다: https://k6.io/docs/getting-started/installation/

### 1. 캐시 없이 테스트
```bash
k6 run k6/test-no-cache.js
```

### 2. Lazy Loading 캐시 테스트
```bash
k6 run k6/test-lazy-cache.js
```

### 3. Eager Loading 캐시 테스트 (2분 동안 실행)
```bash
k6 run k6/test-eager-cache.js
```

이 테스트는 2분 동안 실행되며, 1분 후 TTL이 만료되어 cache penetration이 발생합니다.

## 모니터링

### Grafana 대시보드

1. 브라우저에서 http://localhost:3000 접속
2. 로그인: admin / admin
3. "Cache Practice Metrics" 대시보드 확인

### 주요 메트릭

- **Application CPU Usage**: 애플리케이션 CPU 사용률
- **Application Memory Usage**: JVM 메모리 사용량
- **Cache Hit Rate**: 캐시 히트율
- **HTTP Request Latency**: API 응답 시간

### Prometheus

- 브라우저에서 http://localhost:9090 접속
- 직접 쿼리로 메트릭 확인 가능

## 성능 비교 시나리오

### 시나리오 1: 캐시 없음 vs Lazy Loading
1. 캐시 없는 상태로 부하 테스트 실행
2. Grafana에서 CPU, 메모리, 지연시간 기록
3. Lazy Loading 캐시로 부하 테스트 실행
4. 메트릭 비교

### 시나리오 2: Lazy Loading vs Eager Loading
1. Lazy Loading으로 부하 테스트
2. 애플리케이션 재시작 (Eager Loading 활성화)
3. 2분 동안 부하 테스트 (1분 후 TTL 만료 관찰)
4. Cache penetration 발생 시 메트릭 변화 확인

## 캐시 전략 상세

### Lazy Loading (Write/Read Pattern)
- 첫 요청 시 DB에서 조회 후 캐시에 저장
- 이후 동일 요청은 캐시에서 반환
- 장점: 필요한 데이터만 캐시, 메모리 효율적
- 단점: 첫 요청은 느림 (Cache warming 필요)

### Eager Loading (Boot-time Loading)
- 애플리케이션 시작 시 모든 데이터 캐시 로드
- TTL 1분 후 전체 캐시 만료
- 장점: 모든 요청이 빠름
- 단점: 메모리 사용량 높음, Cache penetration 위험

## 트러블슈팅

### MySQL 연결 실패
```bash
# MySQL 컨테이너 로그 확인
docker logs cache-practice-mysql

# MySQL 재시작
docker-compose restart mysql
```

### 메모리 부족
100만 건 데이터와 Eager Loading은 많은 메모리를 사용합니다.
JVM 힙 크기 조정:
```bash
./gradlew bootRun -Dspring-boot.run.jvmArguments="-Xmx4g"
```

### Grafana 대시보드가 보이지 않음
```bash
# Grafana 재시작
docker-compose restart grafana
```

## 참고사항

- 데이터 시드는 한 번만 실행하면 됩니다
- Eager Loading은 시작 시간이 오래 걸릴 수 있습니다
- k6 테스트 결과는 콘솔에 출력됩니다
- 성능 비교 결과는 PERFORMANCE_REPORT.md에 기록하세요
