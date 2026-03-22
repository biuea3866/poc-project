# 역할별 구현 기록 가이드라인

## BE (Backend Engineer)
구현 시 반드시 기록할 항목:
- 도메인 모델 변경 (엔티티, enum, VO)
- DB 스키마 변경 (Flyway DDL)
- API 엔드포인트 추가/변경
- 비즈니스 규칙 변경
- 이벤트 발행/구독 변경
- 테스트 케이스

저장 위치: `.analysis/be-implementation/results/{날짜}_{기능명}/`

## FE (Frontend Engineer)
구현 시 반드시 기록할 항목:
- 페이지/컴포넌트 추가/변경
- API 연동 추가/변경
- 상태 관리 변경 (Zustand store)
- 타입 정의 변경
- UI/UX 변경 사항 (스크린샷)

저장 위치: `.analysis/implementation/results/{날짜}_{기능명}/`

## DevOps
구현 시 반드시 기록할 항목:
- Docker/인프라 변경
- CI/CD 변경
- 모니터링 설정 변경
- 환경 변수 추가
- 배포 절차 변경

저장 위치: `.analysis/release/results/{날짜}_{변경명}/`

## QA
검증 시 반드시 기록할 항목:
- 테스트 케이스 목록
- 실행 결과 (통과/실패)
- 발견된 버그
- 재검증 결과
- 성능 테스트 결과

저장 위치: `.analysis/verification/results/{날짜}_{검증명}/`

## PM/PO
기획 시 반드시 기록할 항목:
- 배경 및 목표
- User Story + AC
- 데이터 모델 초안
- API 스펙 초안
- KPI 목표
- 경쟁사 벤치마크

저장 위치: `.analysis/prd/results/{날짜}_{기능명}/`

## TechLead
설계 시 반드시 기록할 항목:
- ADR (Architecture Decision Record)
- 기술 부채 식별
- 티켓 분할 결과
- 코드 리뷰 가이드라인 변경
- 설계 원칙 추가/변경

저장 위치: `docs/adr/` 또는 `.analysis/be-implementation/results/`
