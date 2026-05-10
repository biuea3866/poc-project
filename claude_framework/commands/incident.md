---
description: 장애 대응 — RCA + 임시 조치 + 영구 fix + harness-rules 갱신
argument-hint: <간단 설명 또는 대상>
---

# /incident — 절차

**대상**: $ARGUMENTS


장애 발생 시 대응 절차.

## 진입점

- 사용자: `/incident <설명>` 또는 main-orchestrator 가 직접 호출

## 단계

### 1. 트리거·범위 확정
- 영향받는 서비스/사용자 규모 추정
- 시작 시각 (사용자 보고 시각이 아닌 실제 발생 시각)
- 알람/모니터링 링크 수집

### 2. 진단
- 로그·메트릭·트레이스 수집 (`outputs/incident/<YYYYMMDD>-<topic>/logs/`)
- 가설 도출 → 검증 (배포/설정/외부 의존)
- 재현 시도 (가능하면 staging)

### 3. 임시 조치 (Mitigation)
- 롤백 / 기능 플래그 off / 트래픽 차단 중 적절한 것 선택
- 임시 조치 후 영향 줄어들었는지 확인
- **임시 조치는 근본 해결이 아님 — 별도 추적**

### 4. RCA (Root Cause Analysis)
- 5 Whys 반복
- 동일 패턴이 다른 서비스에도 있는지 (cross-cutting 검사)
- harness-rules 로 검출 가능한 패턴이었는지 → 가능했다면 룰 추가 검토

### 5. 사후 조치
- 영구 fix PR (테스트 필수)
- harness-rules 업데이트 (해당 패턴 재발 방지)
- 운영 대응 체크리스트 업데이트
- Postmortem 문서 작성

### 6. 메타-피드백 트리거
- harness-rules 에 누락된 패턴이 발견됨 → process-reviewer 가 자동 트리거 (룰차단 트리거 조건)
- 동일 장애가 30일 내 재발 시 high 위험도로 보고

## 산출물

- `outputs/incident/<YYYYMMDD>-<topic>/`
  - `report.md` (RCA + 타임라인 + 조치)
  - `logs/` (수집한 원본 로그)
  - `postmortem.md` (선택)
- harness-rules 업데이트 PR (해당 시)

## 안전 장치

- 임시 조치 후 24시간 내 RCA 완료 강제 (기한 초과 시 main-orchestrator 가 알림)
- `git_upstream_guard` + `--no-verify` 차단 룰 항상 활성 (긴급 fix 도 PR 거치도록)

## 참고

- harness-rules 갱신 흐름: `commands/audit-feedback-loop.md`

## 완료 단언 규칙

> "완료/검증 끝" 같은 단언은 [`rules/COMPLETION-RULE.md`](../COMPLETION-RULE.md) 의 §1~4 (강제 산출물 / 검증 아티팩트 / 도구 호출 선행 / "지금 시작" 단언 금지) 를 모두 충족해야 한다. 충족 안 된 항목이 있으면 `in-progress` 로 보고.
