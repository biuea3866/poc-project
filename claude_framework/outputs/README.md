# outputs/

명령어 실행 결과(산출물) 보관 디렉토리. 각 슬래시 커맨드는 자기 결과를 다음 경로에 저장한다.

## 구조

```
outputs/
├── analyze-prd/
│   └── <YYYY-MM-DD>-<feature>.md
├── plan-project/
│   └── <feature>/
│       ├── 00-overview.md
│       ├── 01-design.md
│       ├── 02-tdd.md
│       └── 03-tickets.md
├── refactor/
│   └── <YYYYMMDD>-<scope>/
│       ├── plan.md
│       ├── before.md
│       ├── diff-verification.md      # ★ 강제 산출물 (운영 사고 방지)
│       ├── after.md
│       └── retrospective.md
├── multi-repo/
│   └── <YYYYMMDD>-<topic>/
│       ├── 01-inventory.md           # ★ 검색 누락 검증
│       ├── 02-plan.md
│       ├── 03-progress.md            # ★ done/pending 표
│       ├── 04-postdeploy.md
│       └── 05-retrospective.md
├── incident/<YYYYMMDD>-<topic>/
├── inquiry/<YYYYMMDD>-<topic>/
├── release/<version>/
├── api-change/<change-id>/
└── feedback-loop/<YYYY-MM-DD>-health.md
```

## 완료 단언 규칙

각 디렉토리의 **강제 산출물** 이 모두 존재해야 "완료" 단언 가능. `rules/COMPLETION-RULE.md` 참조.

## 별도 위치
- 메타-피드백 제안: `docs/feedback-loop/proposals/<YYYYMMDD>-<topic>.md`
- QA 보고서: `docs/qa/<sprint>.md`
