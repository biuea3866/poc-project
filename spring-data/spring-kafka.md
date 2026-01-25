## Spring Kafka Consumer 기본 동작 예제

### 요구사항
1. Spring Boot 3.2+ 사용
2. Kafka 토픽: "user-events"
3. 메시지 형식: JSON (User 객체 - id, name, email)
4. 다음 컴포넌트들을 구현해줘:
   - KafkaConsumerConfig: Consumer 설정 클래스
   - User DTO 클래스
   - UserEventListener: @KafkaListener를 사용한 리스너
   - KafkaProducerConfig: 테스트용 Producer 설정
   - UserEventProducer: 테스트 메시지 발송용 Producer
   - RestController: 메시지 발송 테스트용 엔드포인트 (/send)

5. 다음 기능들을 포함해줘:
   - JSON 메시지 자동 역직렬화
   - 에러 핸들링 (DefaultErrorHandler 사용)
   - 로깅 (메시지 수신 시 로그 출력)
   - application.yml 설정 파일

6. 프로젝트 구조:
   - src/main/java/com/example/kafka
     - config/
     - dto/
     - listener/
     - producer/
     - controller/
   - src/main/resources/
     - application.yml

7. README.md 파일도 만들어줘:
   - 프로젝트 설명
   - Kafka 실행 방법 (Docker Compose)
   - 애플리케이션 실행 방법
   - 테스트 방법 (curl 예제)

8. docker-compose.yml도 포함해줘 (Kafka + Zookeeper)

## Spring Kafka 고처리량 Consumer 예제

### 요구사항:
1. Spring Boot 3.2+ 사용
2. Kafka 토픽: "high-throughput-events" (파티션 10개)
3. 메시지 형식: JSON (Event 객체 - id, timestamp, type, payload)

4. 다음 3가지 Consumer 전략을 모두 구현해줘:

   A. 기본 Consumer (비교 기준)
      - 토픽: "normal-events"
      - concurrency: 1
      - 단건 처리
      
   B. 배치 Consumer
      - 토픽: "batch-events"  
      - 배치 크기: 500
      - concurrency: 10
      - 배치 처리 로직
      
   C. 최적화된 Consumer
      - 토픽: "optimized-events"
      - concurrency: 10
      - 배치 처리
      - 수동 Ack
      - Non-blocking retry (DLT)
      - 병렬 실행 (ThreadPoolTaskExecutor)

5. 성능 측정 기능:
   - 각 Consumer의 처리 시간 측정
   - 처리량(TPS) 계산 및 로깅
   - Micrometer 메트릭 수집

6. 부하 테스트 도구:
   - LoadTestController: 대량 메시지 발송 엔드포인트
   - 1만 개, 10만 개, 100만 개 메시지 발송 기능
   - 발송 속도 조절 기능

7. 프로젝트 구조:
   - config/
     - NormalConsumerConfig
     - BatchConsumerConfig  
     - OptimizedConsumerConfig
     - ProducerConfig
     - MetricsConfig
   - listener/
     - NormalEventListener
     - BatchEventListener
     - OptimizedEventListener
   - service/
     - EventProcessor (공통 처리 로직)
     - PerformanceMonitor (성능 측정)
   - controller/
     - LoadTestController
   - dto/
     - Event

8. 설정 파일:
   - application.yml: 각 Consumer별 설정
   - 환경별 프로파일 (local, perf-test)

9. 테스트:
   - 각 Consumer 전략별 성능 비교 테스트
   - 동시성 테스트
   - 장애 복구 테스트

10. 문서:
    - README.md
      - 각 전략 설명
      - 성능 비교 결과표
      - 부하 테스트 실행 방법
      - 권장 설정값
    - PERFORMANCE.md
      - 상세한 성능 측정 결과
      - 그래프/차트 (Markdown)
      - 튜닝 가이드

11. Docker 환경:
    - docker-compose.yml
      - Kafka (파티션 10개 설정)
      - Zookeeper
      - Prometheus (메트릭 수집)
      - Grafana (대시보드)
    - Grafana 대시보드 JSON 파일

12. 추가 기능:
    - DLT(Dead Letter Topic) 처리
    - 재처리 메커니즘
    - 헬스 체크 엔드포인트
    - 관리자 API (Consumer pause/resume)