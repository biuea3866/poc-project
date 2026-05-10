---
description: 배포 영향 분석 — 변경 인벤토리 + 영향도 + 롤백 계획
argument-hint: <간단 설명 또는 대상>
---

# /release — 절차

**대상**: $ARGUMENTS


배포 영향 분석 + 배포 진행 절차.

## 진입점

- 사용자: `/release <버전>` 또는 main-orchestrator 호출

## 단계

### 1. 변경 인벤토리
- 직전 release 이후 머지된 PR 목록 (gh CLI)
- 각 PR 의 분류: feat / fix / refactor / chore / breaking
- breaking change 여부 강조

### 2. 영향도 분석
- DB 마이그레이션 유무 (Flyway 버전)
- 외부 API 계약 변경 (BC 깨짐?)
- 환경 변수/설정 추가
- 동기/비동기 영향 (Kafka 토픽, 이벤트 스키마)
- 배포 순서 의존성 (BE → FE 또는 그 반대)

### 3. 사전 점검 (PR Senior Gate 결과 누적 확인)
- 머지된 PR 중 `request-changes` 가 manual override 로 머지된 케이스 있나?
- 있으면 추가 검토

### 4. 롤백 계획
- 데이터 마이그레이션 롤백 가능성
- 비가역 변경 식별 (예: 컬럼 drop, 데이터 삭제)
- feature flag 로 격리된 변경은 즉시 off 가능

### 5. 배포 절차
- staging 먼저, smoke test
- canary % 비율
- 모니터링 대시보드 링크
- 알람 임계값 일시 조정 여부

### 6. 사후 검증
- 배포 후 N분 내 핵심 메트릭 추세
- 롤백 트리거 조건 (에러율 X% 초과 등)

### 7. 회고
- 배포 중 발견된 이슈 → harness-rules 보강 후보
- 메타-피드백 트리거 가능 (예: 배포 실패 → 룰 추가)

## 산출물

- `outputs/release/<version>/`
  - `release-notes.md` (사용자/팀 공유용)
  - `impact-analysis.md` (영향도)
  - `rollback-plan.md`
  - `postdeploy-check.md` (배포 후 결과)

## 안전 장치

- main 직접 배포 금지 (release 브랜치 또는 tag)
- DB 마이그레이션은 down 스크립트도 작성
- breaking change 는 deprecation cycle 1릴리스 권장

## 참고

- API 변경 분석: `commands/api-change.md`
- 인시던트 대응: `commands/incident.md`

## 완료 단언 규칙

> "완료/검증 끝" 같은 단언은 [`rules/COMPLETION-RULE.md`](../COMPLETION-RULE.md) 의 §1~4 (강제 산출물 / 검증 아티팩트 / 도구 호출 선행 / "지금 시작" 단언 금지) 를 모두 충족해야 한다. 충족 안 된 항목이 있으면 `in-progress` 로 보고.
