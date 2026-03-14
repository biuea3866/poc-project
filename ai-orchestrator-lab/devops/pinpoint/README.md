# DevOps Pinpoint 로컬 배포 기준선

## 목적

Pinpoint를 로컬 또는 개발 환경에서 반복 가능하게 기동할 수 있도록 최소 배포 기준을 정의한다.

## 포함 항목

- `docker-compose.yml`
- `env.template`
- `run-local.sh`
- `verify-config.sh`
- `stop-local.sh`

## 운영 체크리스트

- Pinpoint 네트워크 이름 확인
- collector/web 포트 충돌 여부 확인
- HBase/collector/web 이미지 버전 고정
- 백엔드에서 사용할 collector host/port 공유

## 다음 단계

1. `env.template`을 `.env`로 복사한다.
2. 실제 Pinpoint 이미지 태그를 확정한다.
3. `verify-config.sh`로 compose 설정을 먼저 검증한다.
4. `run-local.sh`로 로컬 기동을 검증한다.
5. 백엔드 JVM 옵션과 collector 연결 값을 교차 검증한다.
