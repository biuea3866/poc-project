# 백엔드 Pinpoint 부트스트랩 계약

## 목적

백엔드 서비스가 Pinpoint agent를 일관된 방식으로 주입하고 운영 환경별로 같은 규칙을 따르도록 초기 계약을 정의한다.

## 결정 사항

- JVM 시작 시 `-javaagent` 옵션으로 Pinpoint agent를 주입한다.
- `pinpoint.applicationName`은 서비스 단위로 고정한다.
- `pinpoint.agentId`는 환경별로 유일해야 한다.
- collector 연결 정보는 환경 변수 또는 JVM 옵션으로 주입한다.

## 설정 포인트

- `PINPOINT_AGENT_PATH`
- `PINPOINT_APPLICATION_NAME`
- `PINPOINT_AGENT_ID`
- `PINPOINT_COLLECTOR_HOST`
- `PINPOINT_COLLECTOR_TCP_PORT`
- `PINPOINT_SAMPLING_PERCENT`

## 민감 정보 정책

- 인증 토큰, 비밀번호, 주민번호와 같은 민감 데이터는 trace 대상에서 제외한다.
- 요청/응답 본문 전체를 수집하지 않는다.

## 구현 순서

1. JVM 옵션과 agent 경로 규칙을 고정한다.
2. 서비스 이름과 agent id 규칙을 확정한다.
3. 개발 환경 기준 collector 연결 설정을 검증한다.
4. 샘플링 정책을 확정한다.

## 포함 파일

- `pinpoint-agent.env.template`: 추적 가능한 백엔드 Pinpoint 설정 템플릿
- `pinpoint-bootstrap.sh`: JVM 옵션 생성 스크립트
- `verify-bootstrap.sh`: 필수 설정 검증 스크립트
- `jvm-options-example.md`: 실제 실행 예시

## 시작 방법

1. `pinpoint-agent.env.template`를 `pinpoint-agent.env`로 복사한다.
2. 환경에 맞게 agent 경로와 collector 값을 채운다.
3. `verify-bootstrap.sh`로 설정을 검증한다.
4. `pinpoint-bootstrap.sh`로 JVM 옵션을 생성한다.
