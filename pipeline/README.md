# 분석 파이프라인

프로젝트 분석부터 구현까지의 체계적 워크플로우를 정의합니다.

## 파이프라인 목록

| 파이프라인 | 시작점 | 규모 | 용도 | 가이드 |
|----------|--------|------|------|--------|
| **project-analysis** | PRD (PM 요청) | 대 | PRD → 벤치마킹 → 설계 → 티켓 → 구현 | `project-analysis/CLAUDE.md` |
| **be-refactoring** | 기술 결정 (개발자) | 중~대 | 서비스 전환, 아키텍처 변경, DB 이관 | `be-refactoring/CLAUDE.md` |
| **be-tech-debt** | 코드 품질 (개발자) | 소~중 | detekt 해소, 테스트 추가, 컨벤션 통일 | `be-tech-debt/CLAUDE.md` |
| **pr-review** | PR | - | PR 코드 리뷰 | `pr-review/PIPELINE.md` |

## 산출물 구조

```
# PRD 기반 프로젝트
project-analysis/results/{날짜}_{기능명}/
├── prd/          ← Phase 1 + 1.5 (PRD 분석, 벤치마킹)
├── be/           ← Phase 2 + 2-3 (TDD, 설계, 티켓)
└── README.md

# 리팩토링 (중~대)
be-refactoring/results/{날짜}_{대상}/
├── analysis/     ← Phase 1 (현황, 영향, 리스크, 레퍼런스)
├── plan/         ← Phase 2 (마이그레이션, TDD, 설계, 티켓)
└── README.md

# 기술 부채 (소~중)
be-tech-debt/results/{날짜}_{대상}/
├── diagnosis/    ← Phase 1 (부채 목록, 우선순위)
├── fix/          ← Phase 2 (수정 계획, 티켓)
└── README.md
```

## 사용법

해당 파이프라인의 CLAUDE.md 또는 PIPELINE.md를 읽고 지시에 따라 수행합니다.
