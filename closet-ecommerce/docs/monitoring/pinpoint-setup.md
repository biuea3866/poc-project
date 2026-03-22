# Pinpoint APM 설정 가이드

## 개요

Closet e-commerce 프로젝트의 마이크로서비스 간 분산 추적을 위해 Pinpoint APM을 연동한다.
기존에 운영 중인 Pinpoint 인프라(HBase, Collector, Web)에 각 서비스의 Pinpoint Agent를 연결하는 방식이다.

## Pinpoint Web UI 접근

```
http://localhost:28080
```

## 연동 서비스 목록

| 서비스 | Application Name | 포트 |
|--------|-----------------|------|
| Gateway | closet-gateway | 8080 |
| Member | closet-member | 8081 |
| Product | closet-product | 8082 |
| Order | closet-order | 8083 |
| Payment | closet-payment | 8084 |
| BFF | closet-bff | 8085 |

## 시작/중지 방법

```bash
# Pinpoint APM 연동하여 시작
make start-apm

# Pinpoint 없이 시작
make start

# 전체 서비스 중지
make stop

# Pinpoint Web UI 열기
make pinpoint
```

## Pinpoint Agent 설치

최초 1회만 실행하면 된다. `start-apm` 실행 시 자동으로 다운로드하지만 수동으로도 가능하다.

```bash
bash docker/pinpoint/download-agent.sh
```

## 주요 확인 항목

### 1. ServerMap (서비스 간 호출 맵)

Pinpoint Web UI 상단의 ServerMap 탭에서 확인 가능하다.
- 서비스 간 호출 관계를 시각적으로 확인
- 각 노드(서비스)의 요청 수, 에러율 표시
- BFF -> Order -> Payment 같은 분산 호출 흐름 파악

### 2. CallTree (분산 트랜잭션 추적)

특정 요청을 클릭하면 CallTree를 확인할 수 있다.
- 요청이 어떤 서비스를 거쳤는지 순서대로 표시
- 각 구간별 소요 시간 확인
- SQL 쿼리, HTTP 호출 등 세부 정보 포함

### 3. Response Time 분포

Scatter Chart에서 응답 시간 분포를 확인한다.
- X축: 시간, Y축: 응답 시간
- 점이 위로 튀는 구간이 성능 이슈 구간
- 드래그하여 특정 구간의 트랜잭션 상세 확인

### 4. Inspector

서비스별 JVM 메트릭을 확인한다.
- Heap 사용량
- CPU 사용률
- Active Thread 수
- Response Time 통계

## 트러블슈팅

### Agent 연결이 안 되는 경우

1. Pinpoint Collector가 실행 중인지 확인:
   ```bash
   docker ps | grep pinpoint-collector
   ```

2. Collector 포트가 열려 있는지 확인:
   ```bash
   lsof -i :29994
   ```

3. 서비스 로그에서 Pinpoint 관련 에러 확인:
   ```bash
   grep -i pinpoint /tmp/closet-logs/*.log
   ```

### ServerMap에 서비스가 안 보이는 경우

- 서비스가 최소 1건 이상의 요청을 처리해야 ServerMap에 표시된다
- 시간 범위를 확인하여 최근 시간대로 설정
- Application 드롭다운에서 `closet-*` 서비스 선택

### 응답 시간이 비정상적으로 느린 경우

1. CallTree에서 병목 구간 확인
2. SQL 탭에서 Slow Query 확인
3. Inspector에서 JVM 메트릭(GC, Heap) 확인
