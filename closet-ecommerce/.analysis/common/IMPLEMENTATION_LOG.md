# 구현 기록 체계 (Implementation Log Framework)

> 모든 역할이 구현 시 반드시 따라야 하는 문서화 프레임워크

## 목적

Closet 이커머스 프로젝트의 모든 구현 작업은 추적 가능하고 재현 가능해야 한다.
이 프레임워크는 BE/FE/DevOps/QA/PM/TechLead 모든 역할이 일관된 형식으로 구현 기록을 남기도록 강제한다.

## 구성 파일

| 파일 | 용도 |
|------|------|
| `IMPLEMENTATION_LOG_TEMPLATE.md` | 구현 기록 템플릿 (복사해서 사용) |
| `ROLE_GUIDELINES.md` | 역할별 필수 기록 항목 및 저장 위치 |
| 이 파일 (`IMPLEMENTATION_LOG.md`) | 프레임워크 설명 및 규칙 |

## 규칙

### 1. 모든 구현은 기록한다
- 코드 변경이 있으면 반드시 구현 기록을 남긴다
- 기록 없는 PR은 리뷰 거부 사유가 된다

### 2. 역할별 저장 위치를 준수한다
- BE: `.analysis/be-implementation/results/{날짜}_{기능명}/`
- FE: `.analysis/implementation/results/{날짜}_{기능명}/`
- DevOps: `.analysis/release/results/{날짜}_{변경명}/`
- QA: `.analysis/verification/results/{날짜}_{검증명}/`
- PM: `.analysis/prd/results/{날짜}_{기능명}/`
- TechLead: `docs/adr/` 또는 `.analysis/be-implementation/results/`

### 3. 템플릿을 사용한다
- `IMPLEMENTATION_LOG_TEMPLATE.md`를 복사하여 작성한다
- 섹션을 임의로 삭제하지 않는다 (해당 없으면 "N/A" 기재)

### 4. 리뷰 체크리스트를 완료한다
- 모든 체크리스트 항목을 확인한 후 PR을 생성한다

### 5. Confluence에도 반영한다
- 구현 기록은 `.analysis/` 디렉토리와 Confluence 양쪽에 기록한다

## 워크플로우

```
1. Jira 티켓 확인
2. IMPLEMENTATION_LOG_TEMPLATE.md 복사
3. 목표/배경 작성
4. 구현 진행
5. 변경 파일/설계 결정 기록
6. 테스트 작성 및 결과 기록
7. 리스크/제한사항 기록
8. 리뷰 체크리스트 확인
9. PR 생성 (기록 파일 포함)
10. Confluence 반영
```
