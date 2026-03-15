# Pinpoint Deployment Scaffold

## Purpose

Pinpoint 모니터링 시스템의 초기 배포 구조를 빠르게 시작하기 위한 DevOps 스캐폴드입니다.

## Included Files

- `.env.example`: 이미지/포트/네트워크 변수
- `docker-compose.yml`: HBase, Collector, Web 기본 구조

## Next Steps

1. `.env.example`을 복사해 실제 값을 채웁니다.
2. 선택한 Pinpoint 배포 방식에 맞춰 이미지를 확정합니다.
3. `be` lane과 함께 agent 주입 경로와 collector 연결 정보를 맞춥니다.
4. 운영 체크리스트와 기술 문서를 Confluence에 기록합니다.

## Cross-Lane Checklist

- 서비스 이름 규칙 일치
- JVM `-javaagent` 경로 합의
- Collector endpoint 확인
- 샘플링/추적 범위 확인
- 보안/민감 데이터 마스킹 검토
