# Multi-Repo Change Pipeline

여러 레포에 걸친 변경(설정 / 라이브러리 버전 / 인프라 자격증명 / 공통 컨벤션)을 다룰 때, **누락 없이** 진행하기 위한 절차. Sprint 4 직전 Kafka SCRAM 누락 사고처럼 "다 했다" 를 단언하기 전에 어느 레포가 어떻게 처리됐는지를 강제로 명시화한다.

## 진입점

- 사용자: "전 레포에 X 적용해줘", "관련된 레포 다 찾아서 수정해줘"
- main-orchestrator: 다중 레포 영향 감지 시 자동 진입

## 단계

### 1. 영향 레포 인벤토리 (강제)

다음 산출물을 작성하기 전에는 다음 단계로 가지 못한다:

```
pipelines/multi-repo/<YYYYMMDD>-<topic>/01-inventory.md
```

형식:
```markdown
# <topic> 영향 레포 인벤토리

## 검색 방법
- grep 쿼리: `<쿼리 1>`, `<쿼리 2>`
- 검색 범위: `<디렉토리 목록>`
- 제외: `<제외 패턴>`

## 후보 레포 목록 (검색 결과)
| 레포 | 파일 경로 | 매칭 라인 | 비고 |
|------|----------|-----------|------|
| repo-a | src/.../KafkaConfig.kt:42 | `bootstrap.servers=...` | local/dev/prod 분리 필요 |
| repo-b | configs/application-dev.yml:18 | `kafka.security.protocol=PLAINTEXT` | SCRAM 전환 대상 |
| ... | ... | ... | ... |

## 비대상 분류 (왜 변경 불필요한지)
| 레포 | 사유 |
|------|------|
| repo-x | Kafka 사용하지 않음 |
| repo-y | 외부 시스템, 수정 권한 없음 |
```

**검색 누락 검증 체크리스트** — 모두 ✅ 되기 전에는 인벤토리 미완료:

- [ ] 검색 쿼리를 최소 3가지 다른 키워드로 시도 (예: `kafka`, `bootstrap.servers`, `KafkaConfig`)
- [ ] BE 레포뿐 아니라 인프라 레포(terraform, helm, docker-compose) 도 포함
- [ ] 환경별 설정 파일 분리 (`application-local.yml`, `application-dev.yml`, `application-prod.yml`) 확인
- [ ] `.env*` 파일 확인 (gitignore 일 수도 있으나 `.example` 형태 존재 여부)
- [ ] 레거시 / 서브 레포 (예: `legacy-*`, `archive-*`) 제외 사유 명시

### 2. 레포별 작업 계획

```
pipelines/multi-repo/<YYYYMMDD>-<topic>/02-plan.md
```

각 레포에 대해:
- 변경 파일 목록
- 환경별 적용 범위 (local/dev/staging/prod 중 어디에)
- 의존성 (다른 레포 변경 후 적용해야 하는지)
- 롤백 방법

### 3. 단계별 적용 (한 레포씩)

각 레포에 대해 다음을 **모두** 수행하기 전에는 done 표시 금지:

1. 변경 적용
2. 로컬 빌드/테스트 통과 확인 (raw 출력 첨부 필수)
3. PR 생성 + 링크 기록
4. PR Senior Gate 통과 확인 (Critical 0)
5. 머지 + 머지 commit SHA 기록

### 4. 진행 상황 추적 (강제)

```
pipelines/multi-repo/<YYYYMMDD>-<topic>/03-progress.md
```

```markdown
# 진행 상황

| 레포 | 환경 | 상태 | PR | 머지 commit | 검증 |
|------|------|------|----|-------------|------|
| repo-a | local | done | #123 | abc123 | ✅ 빌드 + 테스트 통과 (artifact 링크) |
| repo-a | dev | done | #124 | def456 | ✅ |
| repo-a | prod | pending | - | - | (다음 릴리스 사이클) |
| repo-b | local | in-progress | #125 | - | 🔄 CI |
| repo-b | dev | pending | - | - | repo-b local 머지 후 |
```

**완료 단언 금지 조건** — 다음 중 하나라도 충족 시 "전 레포 완료" 단언 금지:

- 위 표에 `pending` / `in-progress` / 빈 셀이 1개라도 있음
- 비대상 분류된 레포의 사유 누락
- 검증 컬럼이 비어 있는 done 항목

이 조건을 위반하면 자동으로 메타-피드백 루프 트리거 (process-reviewer 트리거 #1).

### 5. 사후 검증 (배포 후)

```
pipelines/multi-repo/<YYYYMMDD>-<topic>/04-postdeploy.md
```

배포 후 N분 내 핵심 메트릭으로 실제 적용 확인:
- 새 인증 방식의 연결 성공률
- 오래된 인증 방식의 연결 시도 (있으면 누락 의심)
- 로그에 fallback / 에러 패턴 검색

### 6. 회고 (최종)

```
pipelines/multi-repo/<YYYYMMDD>-<topic>/05-retrospective.md
```

- 누락된 레포가 있었나? 왜 검색에 안 잡혔나?
- 검색 키워드 보강 후보 (다음에 같은 작업 시 누락 방지)
- harness-rules 추가 후보 (예: 환경 분리 누락 패턴)

## 산출물 위치 (강제)

```
pipelines/multi-repo/<YYYYMMDD>-<topic>/
├── 01-inventory.md      (1단계 — 강제)
├── 02-plan.md           (2단계 — 강제)
├── 03-progress.md       (3·4단계 — 강제, 머지마다 갱신)
├── 04-postdeploy.md     (5단계 — 강제 if 운영 배포 있음)
└── 05-retrospective.md  (6단계 — 강제)
```

5개 산출물 모두 존재하지 않으면 "다중 레포 작업 완료" 단언 금지.

## 안전 장치

- 레포 1개씩 적용 (전 레포 동시 적용 금지 — 롤백 어려움)
- 환경 순서 강제: local → dev → staging → prod (역순 금지)
- 인프라 자격증명 변경 시 secret rotation 별도 단계
- `git_upstream_guard` + `--no-verify` 차단 룰은 multi-repo 작업에서도 동일 적용

## 참고

- 변경 영향 분석: `pipelines/api-change/PIPELINE.md`
- 배포 절차: `pipelines/release/PIPELINE.md`
- 메타-피드백: `pipelines/feedback-loop/PIPELINE.md`

## 완료 단언 규칙

> "완료/검증 끝" 같은 단언은 [`pipelines/COMPLETION-RULE.md`](../COMPLETION-RULE.md) 의 §1~4 (강제 산출물 / 검증 아티팩트 / 도구 호출 선행 / "지금 시작" 단언 금지) 를 모두 충족해야 한다. 충족 안 된 항목이 있으면 `in-progress` 로 보고.
