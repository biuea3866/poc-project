# 문서 동기화 가이드

## 원칙

TDD(Technical Design Document)가 기준 문서(Source of Truth)입니다. TDD가 변경되면 하위 문서를 순서대로 동기화합니다.

## 동기화 대상

TDD 변경 시 아래 문서가 영향을 받습니다.

| 순서 | 대상 | 설명 |
|------|------|------|
| 1 | Confluence용 md | TDD를 Confluence 형식으로 변환한 파일 |
| 2 | Confluence 페이지 | 실제 Confluence에 게시된 페이지 |
| 3 | 상세 설계 (detailed_design.md) | TDD의 Detail Design 확장 |
| 4 | 마이그레이션 계획 (migration_plan.md) | 해당 시 |
| 5 | 티켓 md 파일 | 변경된 설계가 반영된 티켓 |
| 6 | Jira 이슈 | 티켓 md의 Jira 반영 |

## 동기화 순서

```
TDD 수정
  → Confluence용 md 재생성
    → Confluence 페이지 업데이트
      → 티켓 md 업데이트 (영향받는 티켓만)
        → Jira 이슈 업데이트
```

## Confluence Mermaid 처리

Confluence는 Mermaid를 지원하지 않습니다. PNG 이미지로 변환하여 첨부합니다.

```bash
mmdc -i diagram.mmd -o diagram.png -w 2400 -b white -t default -s 2
```

변환한 PNG를 Confluence 페이지에 첨부파일로 업로드하고, 본문에서 이미지로 참조합니다.

## 주의사항

- **전체 교체(replace) 시 Confluence 수동 편집 내용이 유실됩니다.** Confluence에서 직접 수정한 내용이 있다면 md에 먼저 반영한 후 동기화합니다.
- 부분 수정이 가능하면 전체 교체 대신 해당 섹션만 업데이트합니다.
- 동기화 후 Confluence 페이지의 Mermaid 이미지가 최신 PNG인지 확인합니다.
